package com.example.pebblestone.model

/** Persisted lifetime play statistics. */
data class GameStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    /** Fewest stones ever left at the end of a game (lower is better; 1 = win). */
    val bestRemaining: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    val winRatePercent: Int
        get() = if (gamesPlayed == 0) 0 else (gamesWon * 100) / gamesPlayed

    val hasHistory: Boolean get() = gamesPlayed > 0
}
