package com.spore.gemleap.data

import com.spore.gemleap.model.GameState
import com.spore.gemleap.model.Pos
import com.spore.gemleap.ui.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedGameTest {
    private fun playedGame(): Pair<GameState, GameState> {
        val start = GameState.newGame(Pos(0, 2), seed = 12345L)
        val move = start.allMoves().first()
        return start to start.apply(move)
    }

    @Test
    fun boardSurvivesEncodeDecode() {
        val (start, _) = playedGame()
        val restored = GameState.decode(start.encode())
        assertNotNull(restored)
        assertEquals(start.marbles, restored!!.marbles)
        assertEquals(start.gems, restored.gems)
    }

    @Test
    fun savedGameKeepsBoardHistoryAndMode() {
        val (start, after) = playedGame()
        val saved = SavedGame(GameMode.DAILY, 20_000L, after, listOf(start))

        val restored = SavedGame.decode(saved.encode())

        assertNotNull(restored)
        assertEquals(GameMode.DAILY, restored!!.mode)
        assertEquals(20_000L, restored.dailyEpochDay)
        assertEquals(after.encode(), restored.state.encode())
        assertEquals(listOf(start.encode()), restored.history.map { it.encode() })
        assertTrue(restored.state.remaining == start.remaining - 1)
    }

    @Test
    fun emptyHistoryIsAllowed() {
        val (start, _) = playedGame()
        val restored = SavedGame.decode(SavedGame(GameMode.FREE, 0L, start, emptyList()).encode())
        assertNotNull(restored)
        assertTrue(restored!!.history.isEmpty())
    }

    @Test
    fun corruptedOrForeignDataIsRejected() {
        val (start, _) = playedGame()
        val good = SavedGame(GameMode.FREE, 0L, start, emptyList()).encode()

        assertNull(SavedGame.decode(""))
        assertNull(SavedGame.decode("2" + good.drop(1)))             // other format version
        assertNull(SavedGame.decode(good.replace("FREE", "WEIRD")))  // unknown mode
        assertNull(SavedGame.decode(good.replace(";0;", ";x;")))     // bad day
        assertNull(SavedGame.decode("1;FREE;0;abc;"))                // board too short
        assertNull(GameState.decode("z".repeat(33)))                 // gem slot out of range
    }
}
