package com.spore.gemleap.monetization

/**
 * Central place for AdMob unit ids and the billing product id.
 *
 * The ad ids below are Google's OFFICIAL TEST ids — they always fill with test
 * ads and are safe during development. Using your real ad units in a debug build
 * can get your AdMob account flagged for invalid traffic, so keep these until
 * release, then swap in your real ids (and the real app id in AndroidManifest.xml).
 *
 * TODO(release): replace INTERSTITIAL with your real unit and set USE_TEST = false.
 */
object AdIds {
    /**
     * Keep TRUE during development so test ads are shown (clicking your own live
     * ads can get your AdMob account suspended). Flip to FALSE only for the
     * production release that goes to the Play "production" track.
     */
    const val USE_TEST = false

    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val REAL_INTERSTITIAL = "ca-app-pub-4559645833837224/9207153559"

    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val REAL_REWARDED = "ca-app-pub-4559645833837224/2468163428"

    val interstitial: String
        get() = if (USE_TEST) TEST_INTERSTITIAL else REAL_INTERSTITIAL

    /**
     * Rewarded (watch-for-hint) unit, or null when no real unit is configured yet -
     * in that case the app simply gives hints for free instead of showing an ad.
     */
    val rewarded: String?
        get() = when {
            USE_TEST -> TEST_REWARDED
            REAL_REWARDED.isNotEmpty() -> REAL_REWARDED
            else -> null
        }

    /** In-app product id — must match the product you create in Play Console. */
    const val PRODUCT_REMOVE_ADS = "remove_ads"

    /** Show an interstitial only once every this many games. */
    const val GAMES_PER_INTERSTITIAL = 2
}
