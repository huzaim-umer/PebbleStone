package com.spore.gemleap.monetization

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.spore.gemleap.data.StatsRepository

/**
 * Loads and shows a single interstitial, keeping one preloaded at a time.
 * Displays at most once per [AdIds.GAMES_PER_INTERSTITIAL] games, and never once
 * ads have been removed via purchase. The game counter is persisted so it survives
 * app restarts, and failed loads are retried a few times with a delay.
 */
class InterstitialAdManager(
    private val activity: Activity,
    private val repo: StatsRepository,
) {
    private var ad: InterstitialAd? = null
    private var loading = false
    private var gamesSinceLastAd = repo.loadGamesSinceAd()
    private var loadFailures = 0
    private val handler = Handler(Looper.getMainLooper())

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
                    Log.i(TAG, "Interstitial loaded")
                    ad = loaded
                    loading = false
                    loadFailures = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.code} ${error.message}")
                    ad = null
                    loading = false
                    if (++loadFailures <= MAX_LOAD_RETRIES) {
                        handler.postDelayed(::preload, RETRY_DELAY_MS * loadFailures)
                    }
                }
            },
        )
    }

    /**
     * Call whenever a new game starts. Shows an interstitial if enough games have
     * elapsed and one is ready; otherwise just counts and preloads. No-op when
     * [adsRemoved] is true.
     */
    fun onGameStarted(adsRemoved: Boolean) {
        if (adsRemoved || !adsEnabled) {
            Log.d(TAG, "Skipped: adsRemoved=$adsRemoved adsEnabled=$adsEnabled")
            return
        }
        gamesSinceLastAd++
        repo.saveGamesSinceAd(gamesSinceLastAd)
        if (gamesSinceLastAd < AdIds.GAMES_PER_INTERSTITIAL) {
            Log.d(TAG, "Not yet: $gamesSinceLastAd/${AdIds.GAMES_PER_INTERSTITIAL}")
            preload()
            return
        }
        val ready = ad
        if (ready == null) {
            Log.d(TAG, "Due but no ad ready (loading=$loading); will show on next game")
            loadFailures = 0
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
        repo.saveGamesSinceAd(0)
        ready.show(activity)
    }

    private companion object {
        const val TAG = "InterstitialAd"
        const val MAX_LOAD_RETRIES = 3
        const val RETRY_DELAY_MS = 20_000L
    }
}
