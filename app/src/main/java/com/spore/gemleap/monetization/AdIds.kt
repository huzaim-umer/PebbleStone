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
    const val USE_TEST = true

    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val REAL_INTERSTITIAL = "ca-app-pub-4559645833837224/4920816424"

    val interstitial: String
        get() = if (USE_TEST) TEST_INTERSTITIAL else REAL_INTERSTITIAL

    /** In-app product id — must match the product you create in Play Console. */
    const val PRODUCT_REMOVE_ADS = "remove_ads"

    /** Show an interstitial only once every this many games. */
    const val GAMES_PER_INTERSTITIAL = 3
}
