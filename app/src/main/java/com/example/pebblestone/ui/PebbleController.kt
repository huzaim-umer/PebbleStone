package com.example.pebblestone.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.pebblestone.data.StatsRepository
import com.example.pebblestone.model.GameState
import com.example.pebblestone.model.Move
import com.example.pebblestone.model.Pos

/**
 * Holds the mutable game state for the UI: the current board, the selected
 * marble, undo history, a hint, and lifetime stats. All rules live in
 * [GameState]; this class only coordinates state and persistence.
 */
class PebbleController(private val stats: StatsRepository) {
    var state by mutableStateOf(GameState.newGame())
        private set

    var selected by mutableStateOf<Pos?>(null)
        private set

    /** A suggested legal move, shown until the player acts. */
    var hint by mutableStateOf<Move?>(null)
        private set

    /** Lifetime statistics; recomposes UI when a game is recorded. */
    var lifetimeStats by mutableStateOf(stats.load())
        private set

    /** Increments on every committed move — the UI observes this to fire haptics. */
    var moveCount by mutableStateOf(0)
        private set

    private val history = ArrayDeque<GameState>()
    private var recordedThisGame = false

    val canUndo: Boolean get() = history.isNotEmpty()

    /** Holes the selected marble may legally jump into. */
    val destinations: Set<Pos>
        get() = selected?.let { sel -> state.movesFrom(sel).map { it.to }.toSet() } ?: emptySet()

    fun onTapCell(p: Pos) {
        hint = null
        val s = state
        if (s.isMarble(p)) {
            selected = if (selected == p) null else p
            return
        }
        val sel = selected ?: return
        val move = s.movesFrom(sel).firstOrNull { it.to == p } ?: return
        history.addLast(s)
        state = s.apply(move)
        selected = null
        moveCount++
        recordIfFinished()
    }

    /** Reveals one legal move (also the natural rewarded-ad hook later). */
    fun showHint() {
        selected = null
        hint = state.allMoves().firstOrNull()
    }

    fun undo() {
        if (history.isNotEmpty()) {
            state = history.removeLast()
            selected = null
            hint = null
        }
    }

    fun newGame() {
        history.clear()
        state = GameState.newGame()
        selected = null
        hint = null
        recordedThisGame = false
    }

    private fun recordIfFinished() {
        if (!recordedThisGame && state.isGameOver) {
            recordedThisGame = true
            lifetimeStats = stats.record(won = state.isWin, remaining = state.remaining)
        }
    }
}
