package com.spore.gemleap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import com.spore.gemleap.model.*
import kotlin.math.sin

/** Identical cell geometry for rendering, touch and accessibility. */
internal class BoardGeometry(val side: Float) {
    val step = side * 0.81f / 7f
    val pad = side * 0.095f
    val marbleR = step * 0.405f
    val holeR = step * 0.385f
    fun centerOf(p: Pos) = Offset(pad + step * (p.c + 0.5f), pad + step * (p.r + 0.5f))
    fun hitTest(point: Offset): Pos? = GameState.PLAYABLE.firstOrNull {
        val d = point - centerOf(it)
        kotlin.math.abs(d.x) < step / 2 && kotlin.math.abs(d.y) < step / 2
    }
}

@Composable
fun BoardView(
    state: GameState, theme: GameTheme, selected: Pos?, destinations: Set<Pos>,
    hint: Move?, pulse: Float, anim: MoveAnim?, animProgress: Float,
    onTapCell: (Pos) -> Unit, modifier: Modifier = Modifier, interactive: Boolean = true, winHalo: Float = 0f,
) {
    val callback by rememberUpdatedState(onTapCell)
    val moving = anim != null && animProgress < 1f
    val enabled = interactive && !moving
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        Canvas(Modifier.fillMaxSize().pointerInput(enabled) {
            if (enabled) detectTapGestures { point -> BoardGeometry(size.width.toFloat()).hitTest(point)?.let(callback) }
        }) {
            val geo = BoardGeometry(size.width)
            drawAtelierBoard(center, size.width * 0.48f, theme)
            GameState.PLAYABLE.forEach { p ->
                drawAtelierSocket(geo.centerOf(p), geo.holeR, theme, p in destinations || p == hint?.to)
            }
            fun gem(p: Pos, index: Int, alpha: Float = 1f, radius: Float = geo.marbleR) {
                drawAtelierGem(geo.centerOf(p), radius, theme.gems[index % theme.gems.size], index % theme.gems.size,
                    theme, p == selected, alpha)
            }
            GameState.PLAYABLE.forEach { p ->
                if (state.isMarble(p) && !(moving && p == anim?.to)) gem(p, state.gemAt(p) ?: 0)
            }
            if (moving && anim != null) {
                gem(anim.jumped, anim.jumpedGem, 1f - animProgress, geo.marbleR * (1f - animProgress * 0.4f))
                val from = geo.centerOf(anim.from)
                val to = geo.centerOf(anim.to)
                val leap = sin(animProgress * Math.PI).toFloat() * geo.marbleR * 0.5f
                val position = from + (to - from) * animProgress - Offset(0f, leap)
                drawAtelierGem(position, geo.marbleR, theme.gems[anim.movingGem % theme.gems.size],
                    anim.movingGem % theme.gems.size, theme)
            }
            if (hint != null && !moving) {
                listOf(hint.from, hint.to).forEach { p ->
                    drawCircle(theme.hintRing.copy(alpha = 0.85f - pulse * 0.3f),
                        geo.marbleR * (1.13f + pulse * 0.12f), geo.centerOf(p), style = Stroke(geo.marbleR * 0.06f))
                }
            }
            if (state.isWin && !moving) {
                state.marbles.firstOrNull()?.let { p ->
                    drawCircle(theme.accent.copy(alpha = 0.6f), geo.marbleR * (1.4f + winHalo * 0.3f), geo.centerOf(p), style = Stroke(geo.marbleR * 0.04f))
                }
            }
        }
        // These non-overlapping semantic cells expose the Canvas to TalkBack.
        if (interactive) {
            val step = maxWidth * (0.81f / 7f)
            val pad = maxWidth * 0.095f
            GameState.PLAYABLE.forEach { p ->
                Box(Modifier.offset(pad + step * p.c, pad + step * p.r).size(step).semantics {
                    contentDescription = "Row ${p.r + 1}, column ${p.c + 1}"
                    stateDescription = when {
                        p == selected -> "Selected stone"
                        p in destinations || p == hint?.to -> "Legal destination"
                        state.isMarble(p) -> "Stone"
                        else -> "Empty socket"
                    }
                    role = Role.Button
                    if (!enabled) disabled()
                    onClick(label = if (state.isMarble(p)) "Select stone" else "Move here") {
                        if (enabled) { callback(p); true } else false
                    }
                })
            }
        }
    }
}
