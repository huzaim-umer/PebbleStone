package com.example.pebblestone.data

import android.content.Context
import com.example.pebblestone.model.GameStats
import kotlin.math.min

/**
 * Persists [GameStats] in SharedPreferences. Deliberately tiny and synchronous —
 * the payload is a handful of ints written once per finished game.
 */
class StatsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("pebble_stats", Context.MODE_PRIVATE)

    fun load(): GameStats = GameStats(
        gamesPlayed = prefs.getInt(KEY_PLAYED, 0),
        gamesWon = prefs.getInt(KEY_WON, 0),
        bestRemaining = prefs.getInt(KEY_BEST_REMAINING, 0),
        currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
        bestStreak = prefs.getInt(KEY_BEST_STREAK, 0),
    )

    /** Records one finished game and returns the updated totals. */
    fun record(won: Boolean, remaining: Int): GameStats {
        val prev = load()
        val bestRemaining =
            if (prev.gamesPlayed == 0) remaining else min(prev.bestRemaining, remaining)
        val currentStreak = if (won) prev.currentStreak + 1 else 0
        val updated = prev.copy(
            gamesPlayed = prev.gamesPlayed + 1,
            gamesWon = prev.gamesWon + if (won) 1 else 0,
            bestRemaining = bestRemaining,
            currentStreak = currentStreak,
            bestStreak = maxOf(prev.bestStreak, currentStreak),
        )
        prefs.edit()
            .putInt(KEY_PLAYED, updated.gamesPlayed)
            .putInt(KEY_WON, updated.gamesWon)
            .putInt(KEY_BEST_REMAINING, updated.bestRemaining)
            .putInt(KEY_CURRENT_STREAK, updated.currentStreak)
            .putInt(KEY_BEST_STREAK, updated.bestStreak)
            .apply()
        return updated
    }

    private companion object {
        const val KEY_PLAYED = "games_played"
        const val KEY_WON = "games_won"
        const val KEY_BEST_REMAINING = "best_remaining"
        const val KEY_CURRENT_STREAK = "current_streak"
        const val KEY_BEST_STREAK = "best_streak"
    }
}
