package com.spore.gemleap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.spore.gemleap.model.*
import com.spore.gemleap.ui.theme.PebbleStoneTheme
import java.time.LocalDate

/** All fixtures are replayed legal moves; no production rule or constructor changes. */
private fun solvedFixture(): GameState {
    var board = GameState.newGame()
    val moves = listOf(
        Triple(4, 9, 16),
        Triple(7, 8, 9),
        Triple(0, 3, 8),
        Triple(2, 1, 0),
        Triple(9, 8, 7),
        Triple(6, 7, 8),
        Triple(10, 5, 2),
        Triple(12, 11, 10),
        Triple(15, 8, 3),
        Triple(0, 3, 8),
        Triple(13, 14, 15),
        Triple(15, 8, 3),
        Triple(17, 10, 5),
        Triple(2, 5, 10),
        Triple(19, 18, 17),
        Triple(17, 10, 5),
        Triple(27, 22, 15),
        Triple(20, 21, 22),
        Triple(22, 15, 8),
        Triple(3, 8, 15),
        Triple(15, 16, 17),
        Triple(24, 17, 10),
        Triple(5, 10, 17),
        Triple(26, 25, 24),
        Triple(23, 24, 25),
        Triple(32, 29, 24),
        Triple(17, 24, 29),
        Triple(30, 31, 32),
        Triple(32, 29, 24),
        Triple(25, 24, 23),
        Triple(23, 28, 31)
    )
    moves.forEach { (a, b, c) ->
        val move = Move(GameState.PLAYABLE[a], GameState.PLAYABLE[b], GameState.PLAYABLE[c])
        check(move in board.allMoves())
        board = board.apply(move)
    }
    return board
}

private fun stuckFixture(): GameState {
    var board = GameState.newGame()
    while (board.hasMoves()) board = board.apply(board.allMoves().first())
    return board
}

@Preview(name = "01 Free play", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun FreePreview() = PebbleStoneTheme { GameSurface(GameState.newGame(), GameThemes.ATELIER) }

@Preview(name = "02 Selected", widthDp = 360, heightDp = 800)
@Composable
private fun SelectedPreview() = PebbleStoneTheme {
    GameSurface(GameState.newGame(), GameThemes.ATELIER, selected = Pos(1, 3), destinations = setOf(Pos(3, 3)))
}

@Preview(name = "03 Hint", widthDp = 360, heightDp = 800)
@Composable
private fun HintPreview() = PebbleStoneTheme {
    GameSurface(GameState.newGame(), GameThemes.ATELIER, hint = Move(Pos(1, 3), Pos(2, 3), Pos(3, 3)), pulse = 0.4f)
}

@Preview(name = "04 Daily", widthDp = 360, heightDp = 800)
@Composable
private fun DailyPreview() = PebbleStoneTheme {
    val date = LocalDate.of(2026, 10, 6)
    val day = date.toEpochDay()
    GameSurface(GameState.newGame(GameState.PLAYABLE[(day % 33).toInt()], day), GameThemes.ATELIER,
        mode = GameMode.DAILY, date = date)
}

@Preview(name = "05 Beautifully solved", widthDp = 360, heightDp = 800)
@Composable
private fun WinPreview() = PebbleStoneTheme { GameSurface(solvedFixture(), GameThemes.ATELIER, canUndo = true) }

@Preview(name = "06 No moves", widthDp = 360, heightDp = 800)
@Composable
private fun StuckPreview() = PebbleStoneTheme { GameSurface(stuckFixture(), GameThemes.ATELIER, canUndo = true) }

@Preview(name = "07 Settings", widthDp = 360, heightDp = 800)
@Composable
private fun SettingsPreview() = PebbleStoneTheme { SettingsDialog(GameThemes.ATELIER, true, {}, {}, {}) }

@Preview(name = "08 Statistics empty", widthDp = 360, heightDp = 800)
@Composable
private fun StatsPreview() = PebbleStoneTheme { StatsDialog(GameStats(), DailyState(), false, {}, {}) }

@Preview(name = "09 Rewarded hint", widthDp = 360, heightDp = 800)
@Composable
private fun RewardPreview() = PebbleStoneTheme { HintDialog(true, false, {}, {}) }

@Preview(name = "10 Unavailable ad", widthDp = 360, heightDp = 800)
@Composable
private fun UnavailablePreview() = PebbleStoneTheme { HintDialog(false, false, {}, {}) }

@Preview(name = "11 Loading ad", widthDp = 360, heightDp = 800)
@Composable
private fun LoadingPreview() = PebbleStoneTheme { HintDialog(false, true, {}, {}) }

@Preview(name = "12 Small phone large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun LargeTextPreview() = PebbleStoneTheme { GameSurface(GameState.newGame(), GameThemes.ATELIER) }

@Preview(name = "13 Tablet", widthDp = 800, heightDp = 1280)
@Composable
private fun TabletPreview() = PebbleStoneTheme { GameSurface(GameState.newGame(), GameThemes.ATELIER) }
