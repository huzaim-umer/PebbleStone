package com.spore.gemleap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.spore.gemleap.data.StatsRepository
import com.spore.gemleap.monetization.BillingManager
import com.spore.gemleap.monetization.ConsentManager
import com.spore.gemleap.monetization.InterstitialAdManager
import com.spore.gemleap.monetization.RewardedAdManager
import com.spore.gemleap.ui.GameScreen
import com.spore.gemleap.ui.theme.PebbleStoneTheme
import com.spore.gemleap.ui.theme.WoodDark
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private lateinit var billing: BillingManager
    private lateinit var interstitial: InterstitialAdManager
    private lateinit var rewarded: RewardedAdManager
    private lateinit var consent: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repo = StatsRepository(this)
        billing = BillingManager(this, repo).also { it.connect() }
        interstitial = InterstitialAdManager(this)
        rewarded = RewardedAdManager(this)
        consent = ConsentManager(this)
        // Gather GDPR consent, then initialise ads and preload interstitial + rewarded.
        consent.gatherConsent(this) {
            MobileAds.initialize(this) { }
            interstitial.onAdsAvailable()
            rewarded.onAdsAvailable()
        }

        setContent {
            PebbleStoneTheme(darkTheme = true) {
                Surface(color = WoodDark) {
                    GameScreen(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                        adsRemoved = billing.adsRemoved,
                        removeAdsPrice = billing.priceText,
                        onRemoveAds = { billing.purchaseRemoveAds(this@MainActivity) },
                        onRestorePurchases = { billing.queryPurchases() },
                        onNewGameStarted = { interstitial.onGameStarted(billing.adsRemoved) },
                        onWatchHintAd = { grant -> rewarded.showThenGrant(grant) },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billing.endConnection()
    }
}
