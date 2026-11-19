package com.mutabei.workout

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

private val releaseAppCheckInstalled = AtomicBoolean(false)

@Volatile
var appCheckInitSummary: String = "uninitialized"
    private set

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
                DebugAppCheckProviderFactory.getInstance()
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
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    releaseAppCheckInstalled.set(true)
                    appCheckInitSummary = "release(recaptcha, keyLen=${siteKey.length})"
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
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
