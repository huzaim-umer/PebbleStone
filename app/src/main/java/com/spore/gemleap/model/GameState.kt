package com.spore.gemleap.model

/** A board coordinate on the 7x7 grid. */
data class Pos(val r: Int, val c: Int)

/** A peg-solitaire jump: the [from] marble leaps [over] a neighbour into the empty [to]. */
data class Move(val from: Pos, val over: Pos, val to: Pos)

/**
 * Immutable snapshot of an English peg-solitaire board (33-hole cross).
 *
 * A cell is playable when it falls inside the plus shape; the four 2x2 corners
 * of the 7x7 grid are off-board. Marbles jump orthogonally over an adjacent
 * marble into an empty hole two cells away, removing the jumped marble.
 */
class GameState private constructor(
    val marbles: Set<Pos>,
    val gems: Map<Pos, Int>,
) {
    val remaining: Int get() = marbles.size

    fun isMarble(p: Pos): Boolean = p in marbles

    fun gemAt(p: Pos): Int? = gems[p]

    /** Every legal jump currently available from [from]. */
    fun movesFrom(from: Pos): List<Move> {
        if (from !in marbles) return emptyList()
        val result = ArrayList<Move>(4)
        for ((dr, dc) in DIRECTIONS) {
            val over = Pos(from.r + dr, from.c + dc)
            val to = Pos(from.r + 2 * dr, from.c + 2 * dc)
            if (isPlayable(to) && over in marbles && to !in marbles) {
                result.add(Move(from, over, to))
            }
        }
        return result
    }

    /** All legal jumps on the board. */
    fun allMoves(): List<Move> = marbles.flatMap { movesFrom(it) }

    fun hasMoves(): Boolean = marbles.any { movesFrom(it).isNotEmpty() }

    val isGameOver: Boolean get() = !hasMoves()

    val isWin: Boolean get() = remaining == 1

    /** Returns the board after applying [move]; the jumped marble is removed. */
    fun apply(move: Move): GameState {
        val newMarbles = HashSet(marbles).apply {
            remove(move.from)
            remove(move.over)
            add(move.to)
        }
        val gem = gems[move.from]
        val newGems = HashMap(gems).apply {
            remove(move.from)
            remove(move.over)
            if (gem != null) put(move.to, gem)
        }
        return GameState(newMarbles, newGems)
    }

    companion object {
        const val SIZE = 7
        val CENTER = Pos(3, 3)

        private val DIRECTIONS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

        /** True when (r,c) is inside the cross-shaped board. */
        fun isPlayable(p: Pos): Boolean {
            val r = p.r
            val c = p.c
            if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) return false
            val inArm = (c in 2..4) || (r in 2..4)
            return inArm
        }

        /** All 33 playable holes, row-major. */
        val PLAYABLE: List<Pos> = buildList {
            for (r in 0 until SIZE) for (c in 0 until SIZE) {
                val p = Pos(r, c)
                if (isPlayable(p)) add(p)
            }
        }

        /**
         * A new board with [emptyHole] vacant (centre by default). Every playable
         * single-vacancy start is solvable down to one stone, so any hole is fair.
         * [seed] varies the gemstone colouring between games without affecting play.
         */
        fun newGame(emptyHole: Pos = CENTER, seed: Long = 0L): GameState {
            val marbles = PLAYABLE.filter { it != emptyHole }.toSet()
            // Deterministic but visually varied gemstone assignment (spatial hash + seed).
            val palette = GemPalette.SLOTS
            val s = seed.toInt() * -0x61c88647 // golden-ratio mix
            val gems = marbles.associateWith { p ->
                val h = (p.r * 73856093) xor (p.c * 19349663) xor (p.r * p.c * 83492791) xor s
                (h and 0x7fffffff) % palette
            }
            return GameState(marbles, gems)
        }
    }
}
