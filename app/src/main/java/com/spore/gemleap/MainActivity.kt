package com.spore.gemleap

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.spore.gemleap.data.StatsRepository
import com.spore.gemleap.model.GameTheme
import com.spore.gemleap.monetization.BillingManager
import com.spore.gemleap.monetization.ConsentManager
import com.spore.gemleap.monetization.InterstitialAdManager
import com.spore.gemleap.monetization.RewardedAdManager
import com.spore.gemleap.social.LeaderboardManager
import com.spore.gemleap.ui.GameScreen
import com.spore.gemleap.ui.theme.PebbleStoneTheme
import com.spore.gemleap.update.UpdateManager

class MainActivity : ComponentActivity() {
    private lateinit var billing: BillingManager
    private lateinit var interstitial: InterstitialAdManager
    private lateinit var rewarded: RewardedAdManager
    private lateinit var consent: ConsentManager
    private lateinit var leaderboard: LeaderboardManager
    private lateinit var leaderboardLauncher: ActivityResultLauncher<Intent>
    private lateinit var updates: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repo = StatsRepository(this)
        billing = BillingManager(this, repo).also { it.connect() }
        interstitial = InterstitialAdManager(this, repo)
        rewarded = RewardedAdManager(this)
        consent = ConsentManager(this)
        leaderboard = LeaderboardManager(this).also {
            it.init()
            // Seed the board with existing wins; held until sign-in completes if needed.
            it.submitWins(repo.load().gamesWon)
        }
        leaderboardLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
        updates = UpdateManager(this)
        // Gather GDPR consent, then initialise ads and preload interstitial + rewarded.
        consent.gatherConsent(this) {
            MobileAds.initialize(this) { }
            interstitial.onAdsAvailable()
            rewarded.onAdsAvailable()
        }

        setContent {
            PebbleStoneTheme(darkTheme = true) {
                GameScreen(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    adsRemoved = billing.adsRemoved,
                    removeAdsPrice = billing.priceText,
                    onRemoveAds = { billing.purchaseRemoveAds(this@MainActivity) },
                    onRestorePurchases = { billing.queryPurchases() },
                    onNewGameStarted = { interstitial.onGameStarted(billing.adsRemoved) },
                    onWatchUndoAd = { grant -> rewarded.showThenGrant(grant) },
                    leaderboardAvailable = leaderboard.available,
                    leaderboardSignedIn = leaderboard.signedIn,
                    leaderboardRank = leaderboard.rank,
                    rankLoading = leaderboard.rankLoading,
                    onShowLeaderboard = { leaderboard.show(leaderboardLauncher) },
                    onSignIn = { leaderboard.signIn() },
                    onStatsOpened = { leaderboard.refreshRank() },
                    onWinsChanged = { leaderboard.submitWins(it) },
                    onThemeChanged = ::applySystemBars,
                    updateAvailable = updates.updateAvailable,
                    updateReady = updates.updateReady,
                    onUpdate = updates::startUpdate,
                    onUpdateLater = updates::later,
                    onInstallUpdate = updates::installNow,
                    onInstallLater = updates::installLater,
                )
            }
        }
    }

    /** Transparent system bars with icons that stay readable on the theme's background. */
    private fun applySystemBars(theme: GameTheme) {
        val style = if (theme.isLight) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onResume() {
        super.onResume()
        leaderboard.onResume()
        // Picks up purchases that completed while away (e.g. a pending payment that cleared).
        billing.queryPurchases(userInitiated = false)
        // Offers a newer Play version (and finishes one that downloaded while away).
        updates.check()
    }

    override fun onStop() {
        super.onStop()
        // Match the home-screen icon to the chosen theme once the player leaves the game.
        AppIconManager.sync(this, StatsRepository(this).loadThemeId())
    }

    override fun onDestroy() {
        super.onDestroy()
        updates.release()
        billing.endConnection()
    }
}
