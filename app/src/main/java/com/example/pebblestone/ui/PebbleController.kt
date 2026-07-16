package com.example.pebblestone.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.pebblestone.model.GameState
import com.example.pebblestone.model.Pos

/**
 * Holds the mutable game state for the UI: the current board, the selected
 * marble, and an undo history. Kept deliberately small — all rules live in
 * [GameState].
 */
class PebbleController {
    var state by mutableStateOf(GameState.newGame())
        private set

    var selected by mutableStateOf<Pos?>(null)
        private set

    private val history = ArrayDeque<GameState>()

    val canUndo: Boolean get() = history.isNotEmpty()

    /** Holes the selected marble may legally jump into. */
    val destinations: Set<Pos>
        get() = selected?.let { sel -> state.movesFrom(sel).map { it.to }.toSet() } ?: emptySet()

    fun onTapCell(p: Pos) {
        val s = state
        if (s.isMarble(p)) {
            // Select, or toggle off if tapping the current selection.
            selected = if (selected == p) null else p
            return
        }
        // Tapped an empty hole: commit the move if it's a legal destination.
        val sel = selected ?: return
        val move = s.movesFrom(sel).firstOrNull { it.to == p } ?: return
        history.addLast(s)
        state = s.apply(move)
        selected = null
    }

    fun undo() {
        if (history.isNotEmpty()) {
            state = history.removeLast()
            selected = null
        }
    }

    fun newGame() {
        history.clear()
        state = GameState.newGame()
        selected = null
    }
}
