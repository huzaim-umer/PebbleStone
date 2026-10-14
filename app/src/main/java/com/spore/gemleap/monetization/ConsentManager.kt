package com.spore.gemleap.monetization

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Runs the Google UMP consent flow (required for GDPR/EEA users) and reports
 * when ads may be requested. For users outside a consent region this resolves
 * immediately without showing anything.
 */
class ConsentManager(activity: Activity) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    /**
     * True when the player is in a region (EEA/UK) where they must be able to revisit
     * their consent choice; the UI then shows a "Privacy settings" entry.
     */
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    private fun refreshPrivacyOptions() {
        privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /** Requests consent info, shows the form if required, then invokes [onCanRequestAds]. */
    fun gatherConsent(activity: Activity, onCanRequestAds: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    refreshPrivacyOptions()
                    if (consentInformation.canRequestAds()) onCanRequestAds()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                // Fall back to whatever consent state we already have.
                refreshPrivacyOptions()
                if (consentInformation.canRequestAds()) onCanRequestAds()
            },
        )
    }

    /** Lets the player review or change their consent choice (shown from Settings). */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy options form error: ${formError.message}")
            refreshPrivacyOptions()
        }
    }

    private companion object {
        const val TAG = "ConsentManager"
    }
}
