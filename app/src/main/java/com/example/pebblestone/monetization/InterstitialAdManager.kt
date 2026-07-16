package com.example.pebblestone.monetization

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Loads and shows a single interstitial, keeping one preloaded at a time.
 * Displays at most once per [AdIds.GAMES_PER_INTERSTITIAL] games, and never once
 * ads have been removed via purchase.
 */
class InterstitialAdManager(private val activity: Activity) {
    private var ad: InterstitialAd? = null
    private var loading = false
    private var gamesSinceLastAd = 0

    /** Set true once MobileAds is initialised (after consent). */
    var adsEnabled = false
        private set

    fun onAdsAvailable() {
        adsEnabled = true
        preload()
    }

    private fun preload() {
        if (!adsEnabled || loading || ad != null) return
        loading = true
        InterstitialAd.load(
            activity,
            AdIds.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    ad = null
                    loading = false
                }
            },
        )
    }

    /**
     * Call when a new game starts. Shows an interstitial if enough games have
     * elapsed and one is ready; otherwise just counts and preloads. No-op when
     * [adsRemoved] is true.
     */
    fun onGameStarted(adsRemoved: Boolean) {
        if (adsRemoved || !adsEnabled) return
        gamesSinceLastAd++
        if (gamesSinceLastAd < AdIds.GAMES_PER_INTERSTITIAL) {
            preload()
            return
        }
        val ready = ad
        if (ready == null) {
            preload()
            return
        }
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                ad = null
                preload()
            }
        }
        gamesSinceLastAd = 0
        ready.show(activity)
    }

    private companion object {
        const val TAG = "InterstitialAd"
    }
}
