package com.pratul.expensesmanager.util

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/** Asks for the phone's own screen lock (PIN, pattern or password) before something sensitive. */
object DeviceAuth {
    /** False when the phone has no screen lock set up, so there's nothing to ask for. */
    fun hasScreenLock(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    /**
     * Shows the system's screen-lock check. [onSuccess] runs only if the user unlocks;
     * [onCancelOrError] gets a message to show (or null if they simply backed out).
     */
    fun confirm(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onCancelOrError: (String?) -> Unit
    ) {
        val activity = context.findActivity() ?: return onCancelOrError("Couldn't show the screen lock check.")
        val builder = BiometricPrompt.Builder(activity).setTitle(title).setSubtitle(subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // PIN, pattern or password only, as asked for
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            // Android 10 can't ask for the screen lock alone; it may offer fingerprint first
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        builder.build().authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val cancelled = errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                    onCancelOrError(if (cancelled) null else errString.toString())
                }
            }
        )
    }
}
