package com.example.pebblestone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pebblestone.data.StatsRepository
import com.example.pebblestone.monetization.BillingManager
import com.example.pebblestone.monetization.ConsentManager
import com.example.pebblestone.monetization.InterstitialAdManager
import com.example.pebblestone.ui.GameScreen
import com.example.pebblestone.ui.theme.PebbleStoneTheme
import com.example.pebblestone.ui.theme.WoodDark
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private lateinit var billing: BillingManager
    private lateinit var interstitial: InterstitialAdManager
    private lateinit var consent: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repo = StatsRepository(this)
        billing = BillingManager(this, repo).also { it.connect() }
        interstitial = InterstitialAdManager(this)
        consent = ConsentManager(this)
        // Gather GDPR consent, then initialise ads and preload the interstitial.
        consent.gatherConsent(this) {
            MobileAds.initialize(this) { }
            interstitial.onAdsAvailable()
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
