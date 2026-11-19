package com.mutabei.workout.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.mutabei.workout.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private const val TAG = "Auth"

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Reads the APK's active signing certificate SHA-1 fingerprint for diagnostic display
 * if Google Play Services rejects the OAuth client configuration.
 */
fun getAppSigningSha1(context: Context): String? = try {
    val pm = context.packageManager
    val sigBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = info.signingInfo
        val sig = if (signingInfo?.hasMultipleSigners() == true) {
            signingInfo.apkContentsSigners.firstOrNull()
        } else {
            signingInfo?.signingCertificateHistory?.firstOrNull()
        }
        sig?.toByteArray()
    } else {
        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        info.signatures?.firstOrNull()?.toByteArray()
    }
    sigBytes?.let { bytes ->
        MessageDigest.getInstance("SHA-1")
            .digest(bytes)
            .joinToString(":") { "%02X".format(it) }
    }
} catch (_: Exception) {
    null
}

private suspend fun exchangeGoogleIdTokenWithFirebase(
    idToken: String,
    onProgress: (String) -> Unit
): Result<FirebaseUser> {
    if (idToken.isBlank()) {
        return Result.failure(IllegalStateException("Google ID token is empty"))
    }
    onProgress("جاري تأكيد الحساب مع Firebase...")
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    val authResult = Firebase.auth.signInWithCredential(credential).await()
    val signedInUser = authResult.user ?: Firebase.auth.currentUser
    return if (signedInUser != null) {
        Log.i(TAG, "Firebase Auth succeeded. uid=${signedInUser.uid}")
        Result.success(signedInUser)
    } else {
        Log.e(TAG, "signInWithCredential finished but currentUser is null")
        Result.failure(
            IllegalStateException("اكتمل التحقق من جوجل لكن لم يتم العثور على مستخدم نشط في Firebase Auth (currentUser == null).")
        )
    }
}

private fun formatAuthError(context: Context, error: Throwable): String {
    val sha1 = getAppSigningSha1(context)
    val sha1Hint = if (!sha1.isNullOrBlank()) "\nSHA-1 للنسخة المثبّتة: $sha1" else ""
    return when (error) {
        is FirebaseNetworkException ->
            "تعذّر الاتصال بخوادم Firebase. تأكد من اتصالك بالإنترنت ثم حاول مرة أخرى.\n(${error.javaClass.simpleName}: ${error.localizedMessage ?: "network error"})"

        is FirebaseAuthException ->
            "رفض Firebase Auth تسجيل الدخول.\nالرمز: ${error.errorCode}\nالتفاصيل: ${error.localizedMessage ?: error.message ?: "unknown"}"

        is GoogleIdTokenParsingException ->
            "تعذّرت قراءة رمز الدخول من حساب Google.\n(${error.javaClass.simpleName}: ${error.localizedMessage ?: error.message ?: "parse error"})"

        is NoCredentialException ->
            "لم يتم العثور على حساب Google صالح على الجهاز أو إعدادات Google Play Services غير مكتملة.\n(${error.type}: ${error.errorMessage ?: error.message ?: "no credential"})$sha1Hint"

        is GetCredentialException ->
            "فشل استلام بيانات حساب Google من الجهاز.\nالنوع: ${error.type}\nالتفاصيل: ${error.errorMessage ?: error.message ?: "unknown"}$sha1Hint"

        else ->
            "فشل تسجيل الدخول بحساب Google.\n(${error.javaClass.simpleName}: ${error.localizedMessage ?: error.message ?: "unknown error"})"
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onUnauthenticated: () -> Unit,
    onAutoSignInError: (String) -> Unit = {},
    scope: CoroutineScope
) {
    val existingUser = Firebase.auth.currentUser
    if (existingUser != null) {
        onAuthSuccess(existingUser)
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    if (clientId.isBlank()) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val activity = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                exchangeGoogleIdTokenWithFirebase(token, onProgress = {})
                    .onSuccess { user -> onAuthSuccess(user) }
                    .onFailure { err ->
                        Log.e(TAG, "Auto sign-in Firebase exchange failed", err)
                        onUnauthenticated()
                        onAutoSignInError(formatAuthError(context, err))
                    }
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No previously authorized Google credential for silent sign-in: ${e.message}")
            onUnauthenticated()
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent auto sign-in cancelled or dismissed: ${e.message}", e)
            onUnauthenticated()
        } catch (e: Exception) {
            Log.w(TAG, "Silent auto sign-in unavailable: ${e.javaClass.simpleName}: ${e.message}", e)
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onProgress: (String) -> Unit = {},
    onAuthCancelled: (String) -> Unit = {}
) {
    val existingUser = Firebase.auth.currentUser
    if (existingUser != null) {
        onAuthSuccess(existingUser)
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        Log.e(TAG, "default_web_client_id resource missing", e)
        onAuthError("إعدادات تسجيل دخول جوجل غير مكتملة (default_web_client_id غير موجود).")
        return
    }
    if (clientId.isBlank()) {
        onAuthError("قيمة default_web_client_id فارغة في إعدادات التطبيق.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            onProgress("جاري فتح اختيار حساب Google...")
            val activity = context.findActivity()
                ?: throw IllegalStateException("تعذّر العثور على شاشة التطبيق لفتح تسجيل الدخول.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                exchangeGoogleIdTokenWithFirebase(googleCred.idToken, onProgress)
                    .onSuccess { user -> onAuthSuccess(user) }
                    .onFailure { err ->
                        Log.e(TAG, "Firebase signInWithCredential failed", err)
                        onAuthError(formatAuthError(context, err))
                    }
            } else {
                Log.e(TAG, "Unexpected credential type: ${cred.type}")
                onAuthError("نوع بيانات الدخول المُستلَمة غير متوقع: ${cred.type}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            val sha1 = getAppSigningSha1(context)
            val detail = e.errorMessage?.toString() ?: e.message ?: "cancelled"
            val sha1Line = if (!sha1.isNullOrBlank()) "\nSHA-1 للنسخة المثبّتة: $sha1" else ""
            onAuthCancelled(
                "تم إلغاء نافذة تسجيل الدخول أو رفضها من خدمات Google Play.\nالتفاصيل: $detail$sha1Line"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(formatAuthError(context, e))
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
