package com.mutabei.workout

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

private val releaseAppCheckInstalled = AtomicBoolean(false)
private val appCheckTotalCalls = AtomicInteger(0)
private val appCheckDedupedCalls = AtomicInteger(0)
private val appCheckResets = AtomicInteger(0)
private val appCheckErrorHistory = CopyOnWriteArrayList<String>()

private val retryScheduler = Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "appcheck-retry").apply { isDaemon = true }
}

@Volatile
private var activeProvider: ResilientAppCheckProvider? = null

@Volatile
var appCheckInitSummary: String = "uninitialized"
    private set

@Volatile
var appCheckFirstError: String? = null
    private set

@Volatile
var appCheckLastError: String? = null
    private set

@Volatile
var appCheckLastSuccess: String? = null
    private set

fun isAppCheckHardServerRejection(): Boolean {
    val combined = listOfNotNull(appCheckFirstError, appCheckLastError)
        .joinToString(" ")
        .lowercase()
    return combined.contains("code: 400") ||
        combined.contains("code: 403") ||
        combined.contains("code: 404") ||
        combined.contains("app attestation failed") ||
        combined.contains("invalid_sitekey") ||
        combined.contains("invalid_keytype") ||
        combined.contains("invalid_package_name")
}

/**
 * Allows an explicit user-triggered Gemini retry to recover from a prior transient network
 * failure (e.g., SocketException: Connection reset) that tripped the SDK's in-memory RetryManager.
 * Strictly cooldown-gated so background listeners can never spam Google servers.
 */
fun resetAppCheckProviderForUserRetryIfTransient(): Boolean {
    if (isAppCheckHardServerRejection()) return false
    return activeProvider?.resetIfTransientAndAllowed() ?: false
}

fun buildAppCheckDiagnosticSnapshot(): String {
    val parts = mutableListOf<String>()
    parts += "calls=${appCheckTotalCalls.get()}(dedup=${appCheckDedupedCalls.get()}, rst=${appCheckResets.get()})"
    if (appCheckErrorHistory.isNotEmpty()) {
        parts += "errs=[${appCheckErrorHistory.take(3).joinToString(" ; ")}]"
    }
    val lastErr = appCheckLastError
    if (lastErr != null && appCheckErrorHistory.none { it == lastErr }) {
        parts += "lastErr=$lastErr"
    }
    appCheckLastSuccess?.let { parts += "lastOk=$it" }
    return parts.joinToString(" | ")
}

private fun formatAppCheckCauseChain(error: Throwable): String {
    val chain = generateSequence(error) { it.cause }
        .map { t ->
            val cls = t.javaClass.simpleName.ifBlank { "Error" }
            val msg = t.message?.trim().orEmpty()
            if (msg.isNotBlank()) "$cls: $msg" else cls
        }
        .distinct()
        .joinToString(" ← ")

    // Capture the most relevant SDK frame so we know whether Recaptcha SDK or NetworkClient failed.
    val root = generateSequence(error) { it.cause }.last()
    val frame = root.stackTrace.firstOrNull { st ->
        st.className.startsWith("com.google.firebase.appcheck") ||
            st.className.startsWith("com.google.android.recaptcha")
    } ?: root.stackTrace.firstOrNull()
    val loc = frame?.let {
        val simpleCls = it.className.substringAfterLast('.')
        " @$simpleCls.${it.methodName}"
    }.orEmpty()
    return "$chain$loc"
}

private fun isTransientNetworkFailure(error: Throwable): Boolean {
    val hasIoCause = generateSequence(error) { it.cause }.any { it is IOException }
    if (hasIoCause) return true
    val lower = formatAppCheckCauseChain(error).lowercase()
    return lower.contains("socketexception") ||
        lower.contains("connection reset") ||
        lower.contains("eofexception") ||
        lower.contains("connectexception") ||
        lower.contains("sslexception") ||
        lower.contains("unknownhost") ||
        lower.contains("timed out") ||
        lower.contains("timeout") ||
        lower.contains("network_error") ||
        lower.contains("broken pipe")
}

private fun isRetryManagerLockout(error: Throwable): Boolean {
    val lower = formatAppCheckCauseChain(error).lowercase()
    return lower.contains("too many attempts")
}

private class ResilientAppCheckProvider(
    private val firebaseApp: FirebaseApp,
    private val delegateFactory: AppCheckProviderFactory
) : AppCheckProvider {
    private val lock = Any()
    private var delegate: AppCheckProvider = delegateFactory.create(firebaseApp)
    private var inFlight: Task<AppCheckToken>? = null
    private var lastFailureAtMs: Long = 0L
    private var lastFailedException: Exception? = null
    private var lastResetAtMs: Long = 0L
    private var lastErrorWasTransient: Boolean = false

    fun resetIfTransientAndAllowed(): Boolean {
        synchronized(lock) {
            if (!lastErrorWasTransient && appCheckFirstError?.lowercase()?.let {
                    it.contains("socket") || it.contains("connection reset") || it.contains("io") || it.contains("timeout")
                } != true
            ) {
                return false
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastResetAtMs < 5_000L) {
                return false
            }
            delegate = delegateFactory.create(firebaseApp)
            lastResetAtMs = now
            lastFailureAtMs = 0L
            lastFailedException = null
            appCheckResets.incrementAndGet()
            Log.w(TAG, "Recreated AppCheck provider after transient network failure (resets=${appCheckResets.get()}).")
            return true
        }
    }

    override fun getToken(): Task<AppCheckToken> {
        synchronized(lock) {
            val active = inFlight
            if (active != null && !active.isComplete) {
                appCheckDedupedCalls.incrementAndGet()
                return active
            }
            // Brief 4s cooldown after a failure so background Firestore snapshot listeners
            // don't rapidly fire back-to-back calls and escalate RetryManager backoff.
            val now = SystemClock.elapsedRealtime()
            val recentErr = lastFailedException
            if (recentErr != null && now - lastFailureAtMs < 4_000L) {
                appCheckDedupedCalls.incrementAndGet()
                return Tasks.forException(recentErr)
            }

            val tcs = TaskCompletionSource<AppCheckToken>()
            inFlight = tcs.task
            executeAttempt(tcs, attempt = 1)
            return tcs.task
        }
    }

    private fun executeAttempt(tcs: TaskCompletionSource<AppCheckToken>, attempt: Int) {
        val currentDelegate = synchronized(lock) { delegate }
        val callNum = appCheckTotalCalls.incrementAndGet()

        currentDelegate.getToken()
            .addOnSuccessListener { token ->
                synchronized(lock) {
                    if (inFlight === tcs.task) inFlight = null
                    lastFailureAtMs = 0L
                    lastFailedException = null
                    lastErrorWasTransient = false
                }
                val expSec = (token.expireTimeMillis - System.currentTimeMillis()) / 1000L
                appCheckLastSuccess = "#$callNum(len=${token.token.length}, exp=${expSec}s)"
                Log.d(TAG, "AppCheck token succeeded: $appCheckLastSuccess")
                tcs.trySetResult(token)
            }
            .addOnFailureListener { e ->
                val chain = formatAppCheckCauseChain(e)
                val entry = "#$callNum: $chain"
                if (appCheckFirstError == null) {
                    appCheckFirstError = entry
                }
                appCheckLastError = entry
                if (appCheckErrorHistory.size < 4) {
                    appCheckErrorHistory.add(entry)
                }
                Log.e(TAG, "AppCheck token failed on $entry", e)

                val transient = isTransientNetworkFailure(e)
                val staleLockoutAfterNetworkErr = isRetryManagerLockout(e) && !isAppCheckHardServerRejection() &&
                    (lastErrorWasTransient || appCheckFirstError?.lowercase()?.contains("socket") == true ||
                        appCheckFirstError?.lowercase()?.contains("connection reset") == true)

                val shouldRetryFresh = attempt < 2 && (transient || staleLockoutAfterNetworkErr) && synchronized(lock) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastResetAtMs >= 5_000L) {
                        delegate = delegateFactory.create(firebaseApp)
                        lastResetAtMs = now
                        lastErrorWasTransient = true
                        appCheckResets.incrementAndGet()
                        true
                    } else {
                        false
                    }
                }

                if (shouldRetryFresh) {
                    Log.w(TAG, "Retrying AppCheck with fresh provider in 1200ms after transient error: $chain")
                    retryScheduler.schedule(
                        { executeAttempt(tcs, attempt + 1) },
                        1_200L,
                        TimeUnit.MILLISECONDS
                    )
                } else {
                    synchronized(lock) {
                        if (inFlight === tcs.task) inFlight = null
                        lastFailureAtMs = SystemClock.elapsedRealtime()
                        lastFailedException = e
                        if (transient) lastErrorWasTransient = true
                    }
                    tcs.trySetException(e)
                }
            }
    }
}

private class ResilientAppCheckProviderFactory(
    private val delegateFactory: AppCheckProviderFactory
) : AppCheckProviderFactory {
    private val lock = Any()
    private var cachedProvider: ResilientAppCheckProvider? = null

    override fun create(firebaseApp: FirebaseApp): AppCheckProvider {
        synchronized(lock) {
            val existing = cachedProvider
            if (existing != null) return existing
            val created = ResilientAppCheckProvider(firebaseApp, delegateFactory)
            cachedProvider = created
            activeProvider = created
            return created
        }
    }
}

private fun readSigningSha1Prefix(context: Context): String = try {
    val pm = context.packageManager
    val sigBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = info.signingInfo
        val certs = if (signingInfo?.hasMultipleSigners() == true) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo?.signingCertificateHistory
        }
        certs?.firstOrNull()?.toByteArray()
    } else {
        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        info.signatures?.firstOrNull()?.toByteArray()
    }
    if (sigBytes != null) {
        MessageDigest.getInstance("SHA-1")
            .digest(sigBytes)
            .take(4)
            .joinToString("") { "%02x".format(it) }
    } else {
        "none"
    }
} catch (_: Exception) {
    "err"
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // Avoid stale HttpURLConnection keep-alive socket reuse that can trigger
        // SocketException: Connection reset inside Firebase App Check's NetworkClient.
        System.setProperty("http.keepAlive", "false")

        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = appContext.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Synchronous commit() ensures the debug secret is persisted before
                // DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                ResilientAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            )
            appCheckInitSummary = "debug(tokenPresent=${!debugToken.isNullOrEmpty()})"
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            if (releaseAppCheckInstalled.get()) {
                Log.d(TAG, "RecaptchaAppCheckProviderFactory already installed; skipping reinstall.")
                return
            }
            val resId = appContext.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                appContext.packageName
            )
            if (resId != 0) {
                val siteKey = appContext.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        ResilientAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                        )
                    )
                    releaseAppCheckInstalled.set(true)
                    val opts = FirebaseApp.getInstance().options
                    val proj = opts.projectId.orEmpty()
                    val appSuffix = opts.applicationId.takeLast(6)
                    val sha1Prefix = readSigningSha1Prefix(appContext)
                    appCheckInitSummary =
                        "release(recaptcha, key=${siteKey.take(8)}..(${siteKey.length}), proj=$proj, app=..$appSuffix, sha1=$sha1Prefix)"
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed: $appCheckInitSummary")
                } else {
                    appCheckInitSummary = "release(ERROR: blank recaptcha_site_key)"
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                appCheckInitSummary = "release(ERROR: missing recaptcha_site_key resource)"
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        appCheckInitSummary = "error(${e.javaClass.simpleName}: ${e.message})"
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
