package com.spore.gemleap.data

import com.spore.gemleap.model.GameState
import com.spore.gemleap.ui.GameMode

/**
 * An in-progress game, saved so it survives the app being swiped away: the board,
 * the undo history, and which mode it belongs to (the epoch-day matters for Daily).
 *
 * Text form: `1;FREE|DAILY;<epochDay>;<board>;<board>,<board>,...` where each board is
 * [GameState.encode] and the last field (undo history, oldest first) may be empty.
 */
data class SavedGame(
    val mode: GameMode,
    val dailyEpochDay: Long,
    val state: GameState,
    val history: List<GameState>,
) {
    fun encode(): String =
        "$VERSION;${mode.name};$dailyEpochDay;${state.encode()};${history.joinToString(",") { it.encode() }}"

    companion object {
        private const val VERSION = "1"

        /** Parses [text]; null when it is missing, from another format version, or corrupted. */
        fun decode(text: String): SavedGame? {
            val parts = text.split(';', limit = 5)
            if (parts.size != 5 || parts[0] != VERSION) return null
            val mode = runCatching { GameMode.valueOf(parts[1]) }.getOrNull() ?: return null
            val day = parts[2].toLongOrNull() ?: return null
            val state = GameState.decode(parts[3]) ?: return null
            val history = if (parts[4].isEmpty()) {
                emptyList()
            } else {
                parts[4].split(',').map { GameState.decode(it) ?: return null }
            }
            return SavedGame(mode, day, state, history)
        }
    }
}
