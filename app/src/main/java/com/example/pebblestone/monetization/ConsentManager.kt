package com.example.pebblestone.monetization

import android.app.Activity
import android.util.Log
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
                    if (consentInformation.canRequestAds()) onCanRequestAds()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                // Fall back to whatever consent state we already have.
                if (consentInformation.canRequestAds()) onCanRequestAds()
            },
        )
    }

    private companion object {
        const val TAG = "ConsentManager"
    }
}
