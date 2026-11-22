package com.mutabei.workout

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.security.MessageDigest
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

fun buildAppCheckDiagnosticSnapshot(): String {
    val parts = mutableListOf<String>()
    parts += "calls=${appCheckTotalCalls.get()}(dedup=${appCheckDedupedCalls.get()})"
    appCheckFirstError?.let { parts += "firstErr=$it" }
    val lastErr = appCheckLastError
    if (lastErr != null && lastErr != appCheckFirstError) {
        parts += "lastErr=$lastErr"
    }
    appCheckLastSuccess?.let { parts += "lastOk=$it" }
    return parts.joinToString(" | ")
}

private fun formatAppCheckCauseChain(error: Throwable): String =
    generateSequence(error) { it.cause }
        .map { t ->
            val cls = t.javaClass.simpleName.ifBlank { "Error" }
            val msg = t.message?.trim().orEmpty()
            if (msg.isNotBlank()) "$cls: $msg" else cls
        }
        .distinct()
        .joinToString(" ← ")

private class DedupDiagnosticAppCheckProvider(
    private val delegate: AppCheckProvider
) : AppCheckProvider {
    private val lock = Any()
    private var inFlight: Task<AppCheckToken>? = null

    override fun getToken(): Task<AppCheckToken> {
        synchronized(lock) {
            val active = inFlight
            if (active != null && !active.isComplete) {
                appCheckDedupedCalls.incrementAndGet()
                return active
            }
            val callNum = appCheckTotalCalls.incrementAndGet()
            val task = delegate.getToken()
            inFlight = task
            task.addOnSuccessListener { token ->
                synchronized(lock) {
                    if (inFlight === task) inFlight = null
                }
                val expSec = (token.expireTimeMillis - System.currentTimeMillis()) / 1000L
                appCheckLastSuccess = "#$callNum(len=${token.token.length}, exp=${expSec}s)"
                Log.d(TAG, "AppCheck token succeeded: $appCheckLastSuccess")
            }.addOnFailureListener { e ->
                synchronized(lock) {
                    if (inFlight === task) inFlight = null
                }
                val chain = formatAppCheckCauseChain(e)
                if (appCheckFirstError == null) {
                    appCheckFirstError = "#$callNum: $chain"
                }
                appCheckLastError = "#$callNum: $chain"
                Log.e(TAG, "AppCheck token failed on call#$callNum: $chain", e)
            }
            return task
        }
    }
}

private class DedupDiagnosticAppCheckProviderFactory(
    private val delegateFactory: AppCheckProviderFactory
) : AppCheckProviderFactory {
    private val lock = Any()
    private var cachedProvider: AppCheckProvider? = null

    override fun create(firebaseApp: FirebaseApp): AppCheckProvider {
        synchronized(lock) {
            return cachedProvider ?: DedupDiagnosticAppCheckProvider(
                delegateFactory.create(firebaseApp)
            ).also { cachedProvider = it }
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
                DedupDiagnosticAppCheckProviderFactory(
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
                        DedupDiagnosticAppCheckProviderFactory(
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
