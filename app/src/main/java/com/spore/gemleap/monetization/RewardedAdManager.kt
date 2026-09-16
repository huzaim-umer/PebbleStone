package com.spore.gemleap.monetization

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Loads and shows a rewarded ad for the "watch for a hint" flow, keeping one
 * preloaded. If no rewarded unit is configured ([AdIds.rewarded] is null) or none
 * is ready, [showThenGrant] just grants the reward for free so the hint never blocks.
 */
class RewardedAdManager(private val activity: Activity) {
    private var ad: RewardedAd? = null
    private var loading = false
    private var adsEnabled = false

    private val unitId: String? get() = AdIds.rewarded

    fun onAdsAvailable() {
        adsEnabled = true
        preload()
    }

    private fun preload() {
        val id = unitId ?: return
        if (!adsEnabled || loading || ad != null) return
        loading = true
        RewardedAd.load(
            activity,
            id,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed to load: ${error.message}")
                    ad = null
                    loading = false
                }
            },
        )
    }

    /**
     * Shows the rewarded ad and calls [onGranted] once the reward is earned. If no
     * ad is ready (or rewarded ads are unconfigured), grants immediately so the
     * player still gets their hint.
     */
    fun showThenGrant(onGranted: () -> Unit) {
        val ready = ad
        if (!adsEnabled || ready == null) {
            onGranted()
            preload()
            return
        }
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${error.message}")
                ad = null
                preload()
                onGranted() // fall back to a free hint on show failure
            }
        }
        ready.show(activity) { onGranted() }
    }

    private companion object {
        const val TAG = "RewardedAd"
    }
}
