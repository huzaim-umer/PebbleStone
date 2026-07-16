package com.example.pebblestone.data

import android.content.Context
import com.example.pebblestone.model.GameStats
import com.example.pebblestone.ui.DailyState
import kotlin.math.min

/**
 * Persists [GameStats], daily-challenge progress, and the sound preference in
 * SharedPreferences. Deliberately tiny and synchronous — the payload is a handful
 * of primitives written once per finished game.
 */
class StatsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("pebble_stats", Context.MODE_PRIVATE)

    // ---- Lifetime stats ----

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

    // ---- Daily challenge ----

    fun loadDaily(): DailyState = DailyState(
        lastWonEpochDay = prefs.getLong(KEY_DAILY_LAST_WON, Long.MIN_VALUE),
        streak = prefs.getInt(KEY_DAILY_STREAK, 0),
        bestStreak = prefs.getInt(KEY_DAILY_BEST_STREAK, 0),
    )

    /** Records a daily win for [epochDay]; idempotent per day. Consecutive days extend the streak. */
    fun recordDailyWin(epochDay: Long): DailyState {
        val prev = loadDaily()
        if (prev.lastWonEpochDay == epochDay) return prev
        val streak = if (prev.lastWonEpochDay == epochDay - 1) prev.streak + 1 else 1
        val updated = DailyState(
            lastWonEpochDay = epochDay,
            streak = streak,
            bestStreak = maxOf(prev.bestStreak, streak),
        )
        prefs.edit()
            .putLong(KEY_DAILY_LAST_WON, updated.lastWonEpochDay)
            .putInt(KEY_DAILY_STREAK, updated.streak)
            .putInt(KEY_DAILY_BEST_STREAK, updated.bestStreak)
            .apply()
        return updated
    }

    // ---- Preferences ----

    fun loadSoundOn(): Boolean = prefs.getBoolean(KEY_SOUND_ON, true)

    fun saveSoundOn(on: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ON, on).apply()
    }

    private companion object {
        const val KEY_PLAYED = "games_played"
        const val KEY_WON = "games_won"
        const val KEY_BEST_REMAINING = "best_remaining"
        const val KEY_CURRENT_STREAK = "current_streak"
        const val KEY_BEST_STREAK = "best_streak"
        const val KEY_DAILY_LAST_WON = "daily_last_won_epoch"
        const val KEY_DAILY_STREAK = "daily_streak"
        const val KEY_DAILY_BEST_STREAK = "daily_best_streak"
        const val KEY_SOUND_ON = "sound_on"
    }
}
