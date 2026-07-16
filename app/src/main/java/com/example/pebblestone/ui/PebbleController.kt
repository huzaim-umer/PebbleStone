package com.example.pebblestone.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.pebblestone.data.StatsRepository
import com.example.pebblestone.model.GameState
import com.example.pebblestone.model.Move
import com.example.pebblestone.model.Pos
import java.time.LocalDate
import kotlin.random.Random

/**
 * Holds the mutable game state for the UI: the current board, selection, undo
 * history, hint, active game mode, move animation, and persisted stats. All
 * rules live in [GameState]; this class coordinates state and persistence.
 */
class PebbleController(private val stats: StatsRepository) {
    var state by mutableStateOf(GameState.newGame())
        private set

    var selected by mutableStateOf<Pos?>(null)
        private set

    var hint by mutableStateOf<Move?>(null)
        private set

    var mode by mutableStateOf(GameMode.FREE)
        private set

    /** The most recent jump, for the slide/fade animation. */
    var lastAnim by mutableStateOf<MoveAnim?>(null)
        private set

    var lifetimeStats by mutableStateOf(stats.load())
        private set

    var dailyStats by mutableStateOf(stats.loadDaily())
        private set

    /** Increments on every committed move — the UI observes this to fire haptics/sound. */
    var moveCount by mutableStateOf(0)
        private set

    private val history = ArrayDeque<GameState>()
    private var recordedThisGame = false
    private var animId = 0

    /** Epoch-day of the currently loaded daily board (valid when [mode] is DAILY). */
    var dailyEpochDay: Long = 0L
        private set

    val canUndo: Boolean get() = history.isNotEmpty()

    val destinations: Set<Pos>
        get() = selected?.let { sel -> state.movesFrom(sel).map { it.to }.toSet() } ?: emptySet()

    /** True once today's daily has been solved. */
    val dailySolvedToday: Boolean
        get() = dailyStats.solvedOn(todayEpochDay())

    fun onTapCell(p: Pos) {
        hint = null
        val s = state
        if (s.isMarble(p)) {
            selected = if (selected == p) null else p
            return
        }
        val sel = selected ?: return
        val move = s.movesFrom(sel).firstOrNull { it.to == p } ?: return
        val movingGem = s.gemAt(sel) ?: 0
        val jumpedGem = s.gemAt(move.over) ?: 0
        history.addLast(s)
        state = s.apply(move)
        selected = null
        lastAnim = MoveAnim(++animId, move.from, move.to, move.over, movingGem, jumpedGem)
        moveCount++
        recordIfFinished()
    }

    fun showHint() {
        selected = null
        hint = state.allMoves().firstOrNull()
    }

    fun undo() {
        if (history.isNotEmpty()) {
            state = history.removeLast()
            selected = null
            hint = null
            lastAnim = null
        }
    }

    /** Restart in the current mode (reshuffled free board, or the same daily board). */
    fun restart() {
        if (mode == GameMode.DAILY) startDaily() else startFree()
    }

    fun startFree() {
        mode = GameMode.FREE
        val empty = GameState.PLAYABLE[Random.nextInt(GameState.PLAYABLE.size)]
        loadBoard(GameState.newGame(empty, Random.nextLong()))
    }

    fun startDaily() {
        mode = GameMode.DAILY
        val day = todayEpochDay()
        dailyEpochDay = day
        val empty = GameState.PLAYABLE[(day % GameState.PLAYABLE.size).toInt()]
        loadBoard(GameState.newGame(empty, day))
    }

    private fun loadBoard(board: GameState) {
        history.clear()
        state = board
        selected = null
        hint = null
        lastAnim = null
        recordedThisGame = false
    }

    private fun recordIfFinished() {
        if (recordedThisGame || !state.isGameOver) return
        recordedThisGame = true
        lifetimeStats = stats.record(won = state.isWin, remaining = state.remaining)
        if (mode == GameMode.DAILY && state.isWin) {
            dailyStats = stats.recordDailyWin(dailyEpochDay)
        }
    }

    private fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
}
