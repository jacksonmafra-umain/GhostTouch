package com.wcdonalds.app.security

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps AndroidX BiometricPrompt for overlay-resistant authentication.
 *
 * ## Why biometric auth defeats overlays
 * The system biometric dialog is rendered by the OS at a window layer ABOVE
 * TYPE_APPLICATION_OVERLAY. This means:
 * - Overlays CANNOT cover the biometric prompt
 * - The user always sees the real biometric dialog
 * - Even if an overlay captures credentials, it can't bypass biometric verification
 *
 * ## Implementation notes
 * - Requires `androidx.biometric:biometric` dependency
 * - The Activity hosting the biometric prompt must extend [FragmentActivity]
 *   (or [AppCompatActivity], which extends FragmentActivity)
 * - Falls back to device credential (PIN/pattern/password) on devices without
 *   biometric hardware
 */
object BiometricHelper {

    private const val TAG = "BiometricHelper"

    /**
     * Checks if biometric authentication is available on this device.
     *
     * @param activity The hosting activity.
     * @return true if the device supports biometric or device credential auth.
     */
    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Shows the biometric authentication prompt.
     *
     * The prompt is rendered by the system at a layer that overlays cannot cover,
     * making it an effective defense against credential interception.
     *
     * @param activity The hosting FragmentActivity.
     * @param title Title displayed in the biometric prompt.
     * @param subtitle Subtitle text.
     * @param onSuccess Called when authentication succeeds.
     * @param onFailure Called when authentication fails or is cancelled.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Verify Identity",
        subtitle: String = "Authenticate to continue",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.d(TAG, "Biometric auth succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w(TAG, "Biometric auth error: $errString (code: $errorCode)")
                onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
                Log.w(TAG, "Biometric auth failed — biometric not recognized")
                // Don't call onFailure here — the system will let the user retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                    or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
