package com.example.pebblestone.ui

import com.example.pebblestone.model.Pos

/** Whether the player is on the endless free board or the date-seeded daily one. */
enum class GameMode { FREE, DAILY }

/**
 * Describes the most recent jump so the UI can animate it: the moving stone
 * slides [from] → [to], and the stone at [jumped] fades out. [id] changes each
 * move so the animation restarts.
 */
data class MoveAnim(
    val id: Int,
    val from: Pos,
    val to: Pos,
    val jumped: Pos,
    val movingGem: Int,
    val jumpedGem: Int,
)

/** Persisted daily-challenge progress. Days are stored as epoch-day for easy adjacency. */
data class DailyState(
    val lastWonEpochDay: Long = Long.MIN_VALUE,
    val streak: Int = 0,
    val bestStreak: Int = 0,
) {
    fun solvedOn(epochDay: Long): Boolean = lastWonEpochDay == epochDay
}
