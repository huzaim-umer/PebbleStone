package com.spore.gemleap.social

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.leaderboard.LeaderboardVariant

/** The player's all-time public position; null fields mean Play Games doesn't know yet. */
data class PlayerRank(val rank: Long?, val total: Long?)

/**
 * Global "Total Wins" leaderboard backed by Google Play Games Services.
 *
 * PGS v2 signs the player in silently on launch; [signIn] is only needed when that
 * fails or was declined. Everything is a safe no-op when [LeaderboardIds] is not
 * configured or the player is signed out, so the game never depends on it.
 */
class LeaderboardManager(private val activity: Activity) {
    /** True once the player is authenticated with Play Games. Observed by the UI. */
    var signedIn by mutableStateOf(false)
        private set

    /** Latest known rank, or null before the first successful load. Observed by the UI. */
    var rank by mutableStateOf<PlayerRank?>(null)
        private set

    var rankLoading by mutableStateOf(false)
        private set

    /** Hides the whole feature until real ids are set in strings.xml. */
    val available: Boolean get() = LeaderboardIds.isConfigured(activity)

    private var pendingWins: Int? = null

    fun init() {
        if (!available) return
        PlayGamesSdk.initialize(activity)
        refreshSignInState()
    }

    /** Re-checks sign-in when the game comes back (e.g. after the Play Games sheet). */
    fun onResume() {
        if (available) refreshSignInState()
    }

    private fun refreshSignInState() {
        PlayGames.getGamesSignInClient(activity).isAuthenticated()
            .addOnCompleteListener { task -> onSignInResult(task.isSuccessful && task.result.isAuthenticated) }
    }

    /** Prompts the player to sign in (e.g. after an earlier decline). */
    fun signIn() {
        if (!available) return
        PlayGames.getGamesSignInClient(activity).signIn()
            .addOnCompleteListener { task -> onSignInResult(task.isSuccessful && task.result.isAuthenticated) }
    }

    private fun onSignInResult(authenticated: Boolean) {
        val wasSignedIn = signedIn
        signedIn = authenticated
        if (!authenticated) return
        if (pendingWins != null) flushPending() else if (!wasSignedIn) refreshRank()
    }

    /**
     * Submits the lifetime win total. The leaderboard keeps each player's best score,
     * so resubmitting the same or a lower value is harmless. If not signed in yet the
     * latest value is held and sent as soon as sign-in succeeds.
     */
    fun submitWins(totalWins: Int) {
        if (!available || totalWins <= 0) return
        if (!signedIn) {
            pendingWins = totalWins
            return
        }
        PlayGames.getLeaderboardsClient(activity)
            .submitScoreImmediate(LeaderboardIds.totalWins(activity), totalWins.toLong())
            .addOnCompleteListener { refreshRank() }
    }

    private fun flushPending() {
        pendingWins?.let {
            pendingWins = null
            submitWins(it)
        }
    }

    /** Loads the player's all-time public rank and the number of ranked players. */
    fun refreshRank() {
        if (!available || !signedIn) return
        rankLoading = true
        PlayGames.getLeaderboardsClient(activity)
            .loadLeaderboardMetadata(LeaderboardIds.totalWins(activity), true)
            .addOnCompleteListener { task ->
                rankLoading = false
                val board = if (task.isSuccessful) task.result?.get() else null
                if (board == null) {
                    Log.w(TAG, "Could not load rank: ${task.exception?.message}")
                    return@addOnCompleteListener
                }
                val variant = board.variants.firstOrNull {
                    it.timeSpan == LeaderboardVariant.TIME_SPAN_ALL_TIME &&
                        it.collection == LeaderboardVariant.COLLECTION_PUBLIC
                }
                rank = PlayerRank(
                    rank = variant?.takeIf { it.hasPlayerInfo() }?.playerRank?.takeIf { it > 0 },
                    total = variant?.numScores?.takeIf { it > 0 },
                )
            }
    }

    /** Opens the Play Games leaderboard UI, or starts sign-in if signed out. */
    fun show(launcher: ActivityResultLauncher<android.content.Intent>) {
        if (!available) return
        if (!signedIn) {
            signIn()
            return
        }
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(LeaderboardIds.totalWins(activity))
            .addOnSuccessListener { launcher.launch(it) }
            .addOnFailureListener { Log.w(TAG, "Could not open leaderboard: ${it.message}") }
    }

    private companion object {
        const val TAG = "LeaderboardManager"
    }
}

/** Leaderboard ids come from strings.xml; blank means "not configured yet". */
object LeaderboardIds {
    fun totalWins(activity: Activity): String =
        activity.getString(com.spore.gemleap.R.string.leaderboard_total_wins)

    fun isConfigured(activity: Activity): Boolean =
        totalWins(activity).isNotBlank() &&
            activity.getString(com.spore.gemleap.R.string.pgs_app_id).isNotBlank()
}
