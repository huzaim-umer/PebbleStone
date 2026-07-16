package com.example.pebblestone.ui

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
import com.example.pebblestone.model.GameState
import com.example.pebblestone.model.Gem
import com.example.pebblestone.model.GemPalette
import com.example.pebblestone.model.Move
import com.example.pebblestone.model.Pos

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
    selected: Pos?,
    destinations: Set<Pos>,
    hint: Move?,
    pulse: Float,
    onTapCell: (Pos) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        drawBoard(geo)
        // Holes first, then marbles on top.
        for (p in GameState.PLAYABLE) {
            drawHole(geo.centerOf(p), geo.holeR, highlighted = p in destinations)
        }
        for (p in GameState.PLAYABLE) {
            if (state.isMarble(p)) {
                val gem = GemPalette[state.gemAt(p) ?: 0]
                drawMarble(geo.centerOf(p), geo.marbleR, gem, selected = p == selected)
            }
        }
        // Hint overlay: a pulsing ring on the marble to move and its destination.
        if (hint != null) {
            drawPulseRing(geo.centerOf(hint.from), geo.marbleR, pulse)
            drawPulseRing(geo.centerOf(hint.to), geo.marbleR, pulse)
        }
    }
}

private fun DrawScope.drawPulseRing(center: Offset, r: Float, pulse: Float) {
    val scale = 1.05f + 0.25f * pulse
    val alpha = 0.85f - 0.55f * pulse
    drawCircle(
        color = Color(0xFF6BD37A).copy(alpha = alpha),
        radius = r * scale,
        center = center,
        style = Stroke(width = r * 0.14f),
    )
}

private fun DrawScope.drawBoard(geo: BoardGeometry) {
    val side = geo.side
    // Full wooden disk with a warm radial sheen.
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF9A6A3E),
            0.6f to Color(0xFF6B4526),
            1f to Color(0xFF3A2614),
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
            0f to Color(0xFF7A5330),
            1f to Color(0xFF4A3018),
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

private fun DrawScope.drawHole(center: Offset, r: Float, highlighted: Boolean) {
    // Recessed hole: dark centre fading to a lit lower rim.
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF14100A),
            0.7f to Color(0xFF241A0F),
            1f to Color(0xFF5A3E22),
            center = center - Offset(0f, r * 0.3f),
            radius = r * 1.4f,
        ),
        radius = r,
        center = center,
    )
    if (highlighted) {
        // Soft amber target ring for a legal destination.
        drawCircle(
            color = Color(0x55E0A458),
            radius = r * 1.15f,
            center = center,
        )
        drawCircle(
            color = Color(0xFFE0A458),
            radius = r * 0.98f,
            center = center,
            style = Stroke(width = r * 0.16f),
        )
    }
}

private fun DrawScope.drawMarble(center: Offset, r: Float, gem: Gem, selected: Boolean) {
    val radius = if (selected) r * 1.06f else r

    if (selected) {
        drawCircle(color = Color(0x88E0A458), radius = radius * 1.28f, center = center)
        drawCircle(color = Color(0x44E0A458), radius = radius * 1.5f, center = center)
    }

    // Contact shadow on the board beneath the marble.
    drawCircle(
        color = Color(0x55000000),
        radius = radius * 0.92f,
        center = center + Offset(radius * 0.14f, radius * 0.22f),
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
    )
    // Defining rim.
    drawCircle(
        color = gem.base.darken(0.55f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.06f),
    )
    // Specular highlight dot.
    drawCircle(
        color = Color.White.copy(alpha = 0.85f * gem.sheen.coerceAtMost(1.2f)),
        radius = radius * 0.20f,
        center = center - Offset(radius * 0.34f, radius * 0.40f),
    )
    // Faint lower bounce light for depth.
    drawCircle(
        color = gem.base.lighten(0.25f).copy(alpha = 0.35f),
        radius = radius * 0.30f,
        center = center + Offset(radius * 0.28f, radius * 0.34f),
    )
}
