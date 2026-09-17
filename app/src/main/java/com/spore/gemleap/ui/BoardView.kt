package com.spore.gemleap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.spore.gemleap.model.GameState
import com.spore.gemleap.model.GameTheme
import com.spore.gemleap.model.Gem
import com.spore.gemleap.model.Move
import com.spore.gemleap.model.Pos

/** Precomputed pixel geometry derived from the (square) board side length. */
private class BoardGeometry(val side: Float) {
    val pad = side * 0.135f                 // wooden rim thickness
    val gridArea = side - 2 * pad
    val step = gridArea / GameState.SIZE
    val marbleR = step * 0.40f
    val holeR = step * 0.33f
    val center = Offset(side / 2f, side / 2f)

    fun centerOf(p: Pos): Offset =
        Offset(pad + step * (p.c + 0.5f), pad + step * (p.r + 0.5f))

    /** Nearest playable hole to [o], if the tap lands close enough to count. */
    fun hitTest(o: Offset): Pos? {
        var best: Pos? = null
        var bestD = step * 0.55f
        for (p in GameState.PLAYABLE) {
            val d = (centerOf(p) - o).getDistance()
            if (d < bestD) {
                bestD = d
                best = p
            }
        }
        return best
    }
}

@Composable
fun BoardView(
    state: GameState,
    theme: GameTheme,
    selected: Pos?,
    destinations: Set<Pos>,
    hint: Move?,
    pulse: Float,
    anim: MoveAnim?,
    animProgress: Float,
    onTapCell: (Pos) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun gemFor(index: Int): Gem = theme.gems[((index % theme.gems.size) + theme.gems.size) % theme.gems.size]

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val geo = BoardGeometry(size.width.toFloat())
                    geo.hitTest(offset)?.let(onTapCell)
                }
            }
    ) {
        val geo = BoardGeometry(size.width)
        val animating = anim != null && animProgress < 1f
        drawBoard(geo, theme)
        // Holes first, then marbles on top.
        for (p in GameState.PLAYABLE) {
            drawHole(geo.centerOf(p), geo.holeR, highlighted = p in destinations, theme = theme)
        }
        for (p in GameState.PLAYABLE) {
            if (state.isMarble(p)) {
                if (animating && p == anim!!.to) continue
                drawMarble(geo.centerOf(p), geo.marbleR, gemFor(state.gemAt(p) ?: 0), theme, selected = p == selected)
            }
        }
        if (animating) {
            // Jumped stone fades and shrinks away.
            drawMarble(
                geo.centerOf(anim!!.jumped), geo.marbleR, gemFor(anim.jumpedGem), theme,
                selected = false, alpha = 1f - animProgress, scale = 1f - 0.4f * animProgress,
            )
            // Moving stone slides from source to destination.
            val from = geo.centerOf(anim.from)
            val to = geo.centerOf(anim.to)
            val movingCenter = Offset(
                from.x + (to.x - from.x) * animProgress,
                from.y + (to.y - from.y) * animProgress,
            )
            drawMarble(movingCenter, geo.marbleR, gemFor(anim.movingGem), theme, selected = false)
        }
        // Hint overlay: a pulsing ring on the marble to move and its destination.
        if (hint != null && !animating) {
            drawPulseRing(geo.centerOf(hint.from), geo.marbleR, pulse, theme.hintRing)
            drawPulseRing(geo.centerOf(hint.to), geo.marbleR, pulse, theme.hintRing)
        }
    }
}

private fun DrawScope.drawPulseRing(center: Offset, r: Float, pulse: Float, color: Color) {
    val scale = 1.05f + 0.25f * pulse
    val alpha = 0.85f - 0.55f * pulse
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = r * scale,
        center = center,
        style = Stroke(width = r * 0.14f),
    )
}

private fun DrawScope.drawBoard(geo: BoardGeometry, theme: GameTheme) {
    val side = geo.side
    // Board disk with a faint lit sheen from the top-left.
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.discOuter,
            0.6f to theme.discMid,
            1f to theme.discInner,
            center = Offset(side * 0.42f, side * 0.38f),
            radius = side * 0.62f,
        ),
        radius = side / 2f,
        center = geo.center,
    )
    // Outer rim highlight + shadow to give the edge some roundness.
    drawCircle(
        color = Color(0x33FFFFFF),
        radius = side / 2f - side * 0.006f,
        center = geo.center,
        style = Stroke(width = side * 0.012f),
    )
    drawCircle(
        color = Color(0x55000000),
        radius = side / 2f - side * 0.03f,
        center = geo.center,
        style = Stroke(width = side * 0.02f),
    )
    // Recessed circular playing surface.
    val innerR = geo.gridArea * 0.62f
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.surfaceOuter,
            1f to theme.surfaceInner,
            center = geo.center,
            radius = innerR,
        ),
        radius = innerR,
        center = geo.center,
    )
    drawCircle(
        color = Color(0x66000000),
        radius = innerR,
        center = geo.center,
        style = Stroke(width = side * 0.01f),
    )
}

private fun DrawScope.drawHole(center: Offset, r: Float, highlighted: Boolean, theme: GameTheme) {
    // Recessed hole: dark centre fading to a lit lower rim.
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.holeCenter,
            0.7f to theme.holeMid,
            1f to theme.holeRim,
            center = center - Offset(0f, r * 0.3f),
            radius = r * 1.4f,
        ),
        radius = r,
        center = center,
    )
    if (highlighted) {
        // Soft accent target ring for a legal destination.
        drawCircle(color = theme.accent.copy(alpha = 0.33f), radius = r * 1.15f, center = center)
        drawCircle(
            color = theme.accent,
            radius = r * 0.98f,
            center = center,
            style = Stroke(width = r * 0.16f),
        )
    }
}

private fun DrawScope.drawMarble(
    center: Offset,
    r: Float,
    gem: Gem,
    theme: GameTheme,
    selected: Boolean,
    alpha: Float = 1f,
    scale: Float = 1f,
) {
    val radius = (if (selected) r * 1.06f else r) * scale

    if (selected) {
        drawCircle(color = theme.accent.copy(alpha = 0.53f), radius = radius * 1.28f, center = center, alpha = alpha)
        drawCircle(color = theme.accent.copy(alpha = 0.27f), radius = radius * 1.5f, center = center, alpha = alpha)
    }

    // Contact shadow on the board beneath the marble.
    drawCircle(
        color = Color(0x55000000),
        radius = radius * 0.92f,
        center = center + Offset(radius * 0.14f, radius * 0.22f),
        alpha = alpha,
    )

    // The sphere: bright highlight offset toward top-left, dark rim.
    val hi = gem.base.lighten(0.55f * gem.sheen)
    val lo = gem.base.darken(0.42f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to hi,
            0.35f to gem.base,
            1f to lo,
            center = center - Offset(radius * 0.35f, radius * 0.38f),
            radius = radius * 1.35f,
        ),
        radius = radius,
        center = center,
        alpha = alpha,
    )
    // Defining rim.
    drawCircle(
        color = gem.base.darken(0.55f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.06f),
        alpha = alpha,
    )
    // Specular highlight dot.
    drawCircle(
        color = Color.White.copy(alpha = 0.85f * gem.sheen.coerceAtMost(1.2f)),
        radius = radius * 0.20f,
        center = center - Offset(radius * 0.34f, radius * 0.40f),
        alpha = alpha,
    )
    // Faint lower bounce light for depth.
    drawCircle(
        color = gem.base.lighten(0.25f).copy(alpha = 0.35f),
        radius = radius * 0.30f,
        center = center + Offset(radius * 0.28f, radius * 0.34f),
        alpha = alpha,
    )
}
