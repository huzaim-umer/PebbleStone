package com.spore.gemleap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import com.spore.gemleap.model.BoardStyle
import com.spore.gemleap.model.GameState
import com.spore.gemleap.model.GameTheme
import com.spore.gemleap.model.Gem
import com.spore.gemleap.model.GemTexture
import com.spore.gemleap.model.Move
import com.spore.gemleap.model.Pos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Precomputed pixel geometry derived from the (square) board side length. */
private class BoardGeometry(val side: Float) {
    val pad = side * 0.135f                 // keeps the playing grid inside the dish
    val gridArea = side - 2 * pad
    val step = gridArea / GameState.SIZE
    val marbleR = step * 0.43f
    val holeR = step * 0.30f
    val center = Offset(side / 2f, side / 2f)

    /** Radius of the whole wooden dish (a little under half the side, leaving room for its shadow). */
    val outerR = side / 2f * 0.965f

    /** Radius of the recessed playing surface inside the raised rim. */
    val surfaceR = outerR * 0.83f

    fun centerOf(p: Pos): Offset =
        Offset(pad + step * (p.c + 0.5f), pad + step * (p.r + 0.5f))

    /** Nearest playable spot to [o], if the tap lands close enough to count. */
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

    Box(
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
        // Static layer: the dish, its grain and the shallow dimples. It reads no
        // animated state, so it is drawn once per theme/size rather than every frame.
        BoardBackdrop(theme)

        // Dynamic layer: legal targets, marbles, move animation and hints.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val geo = BoardGeometry(size.width)
            val animating = anim != null && animProgress < 1f
            for (p in destinations) {
                drawDestination(geo.centerOf(p), geo.holeR, theme)
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
}

@Composable
private fun BoardBackdrop(theme: GameTheme) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val geo = BoardGeometry(size.width)
        drawBoard(geo, theme)
        for (p in GameState.PLAYABLE) {
            drawDimple(geo.centerOf(p), geo.holeR, theme)
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

// ---------------------------------------------------------------------------
// Board: a round wooden dish with a raised, rounded rim around a recessed surface.
// ---------------------------------------------------------------------------

private fun DrawScope.drawBoard(geo: BoardGeometry, theme: GameTheme) = when (theme.boardStyle) {
    BoardStyle.WOOD -> drawWoodBoard(geo, theme)
    BoardStyle.NEON -> drawNeonBoard(geo, theme)
    BoardStyle.SLATE -> drawSlateBoard(geo, theme)
}

/** Soft drop shadow so a dish sits on the background. */
private fun DrawScope.drawDishShadow(geo: BoardGeometry, strength: Float) {
    val shadowC = geo.center + Offset(geo.side * 0.008f, geo.side * 0.014f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = 0.40f * strength),
            0.88f to Color.Black.copy(alpha = 0.27f * strength),
            1f to Color.Transparent,
            center = shadowC,
            radius = geo.outerR * 1.03f,
        ),
        radius = geo.outerR * 1.03f,
        center = shadowC,
    )
}

/**
 * Jewel Atelier: a dark violet case with neon orbital rings, a recessed night-black
 * surface and faint axis guides (from the "circular game board base" asset).
 */
private fun DrawScope.drawNeonBoard(geo: BoardGeometry, theme: GameTheme) {
    val c = geo.center
    val r = geo.outerR
    val side = geo.side

    // Violet aura around the case.
    drawCircle(
        brush = Brush.radialGradient(
            0.80f to theme.accent.copy(alpha = 0.22f),
            1f to Color.Transparent,
            center = c,
            radius = r * 1.06f,
        ),
        radius = r * 1.06f,
        center = c,
    )
    // Case body.
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.discOuter,
            0.65f to theme.discMid,
            0.95f to theme.discInner,
            1f to theme.grainDark,
            center = c,
            radius = r,
        ),
        radius = r,
        center = c,
    )
    // Rim shine from the top.
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.accent.copy(alpha = 0.22f),
            1f to Color.Transparent,
            center = Offset(c.x, c.y - r * 0.6f),
            radius = r * 1.2f,
        ),
        radius = r,
        center = c,
    )
    drawCircle(color = theme.surfaceOuter, radius = r - side * 0.01f, center = c, style = Stroke(width = side * 0.02f))

    // Orbital groove rings.
    drawCircle(
        color = theme.accent2.copy(alpha = 0.45f),
        radius = r * 0.93f,
        center = c,
        style = Stroke(width = side * 0.004f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(side * 0.015f, side * 0.01f))),
    )
    drawCircle(color = theme.holeRim.copy(alpha = 0.85f), radius = r * 0.895f, center = c, style = Stroke(width = side * 0.006f))
    drawCircle(
        color = theme.grainLight.copy(alpha = 0.40f),
        radius = r * 0.865f,
        center = c,
        style = Stroke(width = side * 0.0025f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(side * 0.005f, side * 0.008f))),
    )

    // Recessed playing surface.
    val surfaceR = r * 0.845f
    drawCircle(color = theme.surfaceInner, radius = surfaceR, center = c)
    drawCircle(color = theme.grainDark, radius = surfaceR, center = c, style = Stroke(width = side * 0.005f))

    // Faint celestial axis guides.
    val guide = theme.holeRim.copy(alpha = 0.16f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(side * 0.01f, side * 0.02f))
    drawLine(guide, Offset(c.x, c.y - surfaceR), Offset(c.x, c.y + surfaceR), strokeWidth = side * 0.0025f, pathEffect = dash)
    drawLine(guide, Offset(c.x - surfaceR, c.y), Offset(c.x + surfaceR, c.y), strokeWidth = side * 0.0025f, pathEffect = dash)
}

/** Zen Stone: a matte dark slate dish with a soft lip and faint concentric rings. */
private fun DrawScope.drawSlateBoard(geo: BoardGeometry, theme: GameTheme) {
    val c = geo.center
    val r = geo.outerR
    val side = geo.side
    drawDishShadow(geo, strength = 0.8f)

    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.discOuter,
            0.7f to theme.discMid,
            1f to theme.discInner,
            center = c + Offset(-r * 0.2f, -r * 0.25f),
            radius = r * 1.4f,
        ),
        radius = r,
        center = c,
    )
    val surfaceR = geo.surfaceR
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.surfaceOuter,
            1f to theme.surfaceInner,
            center = c + Offset(-surfaceR * 0.15f, -surfaceR * 0.2f),
            radius = surfaceR * 1.4f,
        ),
        radius = surfaceR,
        center = c,
    )
    // Faint concentric rings, like raked stone.
    for (f in listOf(0.92f, 0.62f)) {
        drawCircle(color = theme.grainLight.copy(alpha = 0.35f), radius = surfaceR * f, center = c, style = Stroke(width = side * 0.0025f))
    }

    val lightTL = Offset(c.x - r, c.y - r)
    val lightBR = Offset(c.x + r, c.y + r)
    drawCircle(
        brush = Brush.linearGradient(listOf(Color(0x66000000), Color.Transparent, Color(0x22FFFFFF)), lightTL, lightBR),
        radius = surfaceR - side * 0.008f,
        center = c,
        style = Stroke(width = side * 0.016f),
    )
    drawCircle(
        brush = Brush.linearGradient(listOf(Color(0x40FFFFFF), Color.Transparent, Color(0x55000000)), lightTL, lightBR),
        radius = r - side * 0.008f,
        center = c,
        style = Stroke(width = side * 0.014f),
    )
}

private fun DrawScope.drawWoodBoard(geo: BoardGeometry, theme: GameTheme) {
    val side = geo.side
    val c = geo.center
    val outerR = geo.outerR
    val surfaceR = geo.surfaceR

    // Soft drop shadow so the dish sits on the background.
    val shadowC = c + Offset(side * 0.008f, side * 0.014f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0x66000000),
            0.88f to Color(0x44000000),
            1f to Color.Transparent,
            center = shadowC,
            radius = outerR * 1.03f,
        ),
        radius = outerR * 1.03f,
        center = shadowC,
    )

    // Wood disk, lit from the top-left.
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.discOuter,
            0.55f to theme.discMid,
            1f to theme.discInner,
            center = c + Offset(-outerR * 0.25f, -outerR * 0.3f),
            radius = outerR * 1.5f,
        ),
        radius = outerR,
        center = c,
    )

    // Recessed playing surface.
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.surfaceOuter,
            1f to theme.surfaceInner,
            center = c + Offset(-surfaceR * 0.2f, -surfaceR * 0.25f),
            radius = surfaceR * 1.5f,
        ),
        radius = surfaceR,
        center = c,
    )

    // Grain runs continuously across rim and surface, like one turned piece of wood.
    clipPath(Path().apply { addOval(Rect(c, outerR)) }) {
        drawGrain(geo, theme)
    }

    // Darken the surface toward its edge for a soft, hollowed feel.
    drawCircle(
        brush = Brush.radialGradient(
            0.62f to Color.Transparent,
            1f to Color(0x47000000),
            center = c,
            radius = surfaceR,
        ),
        radius = surfaceR,
        center = c,
    )

    val lightTL = Offset(c.x - outerR, c.y - outerR)
    val lightBR = Offset(c.x + outerR, c.y + outerR)

    // Raised ridge between rim and surface: lit on the top-left, shaded on the bottom-right.
    val ridgeR = (surfaceR + outerR) / 2f
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0x40FFFFFF), Color.Transparent, Color(0x4A000000)),
            start = lightTL,
            end = lightBR,
        ),
        radius = ridgeR,
        center = c,
        style = Stroke(width = side * 0.018f),
    )

    // Inner wall of the recess: shadow on the top-left, a catch of light on the bottom-right.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0x99000000), Color.Transparent, Color(0x38FFFFFF)),
            start = lightTL,
            end = lightBR,
        ),
        radius = surfaceR - side * 0.012f,
        center = c,
        style = Stroke(width = side * 0.026f),
    )
    // Thin dark groove where the surface meets the rim.
    drawCircle(
        color = Color(0x66000000),
        radius = surfaceR + side * 0.004f,
        center = c,
        style = Stroke(width = side * 0.006f),
    )

    // Rounded outer lip.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0x66FFFFFF), Color.Transparent, Color(0x73000000)),
            start = lightTL,
            end = lightBR,
        ),
        radius = outerR - side * 0.011f,
        center = c,
        style = Stroke(width = side * 0.022f),
    )
    drawCircle(
        color = Color(0x80000000),
        radius = outerR - side * 0.002f,
        center = c,
        style = Stroke(width = side * 0.004f),
    )
}

/** Diagonal wavy streaks, deterministic so the board looks the same every frame and launch. */
private fun DrawScope.drawGrain(geo: BoardGeometry, theme: GameTheme) {
    val c = geo.center
    val r = geo.outerR
    val rnd = Random(11)
    val angle = Math.toRadians(28.0)
    val dx = cos(angle).toFloat()
    val dy = sin(angle).toFloat()
    val px = -dy
    val py = dx

    fun streak(width: Float, color: Color, amp: Float, waves: Float) {
        val t = (rnd.nextFloat() * 2f - 1f) * r * 1.05f
        val phase = rnd.nextFloat() * 6.2832f
        val path = Path()
        val segs = 28
        for (i in 0..segs) {
            val s = -r * 1.1f + 2.2f * r * i / segs
            val off = t + amp * sin(s / r * waves + phase)
            val x = c.x + dx * s + px * off
            val y = c.y + dy * s + py * off
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = width, cap = StrokeCap.Round))
    }

    // Broad soft figure first, then the fine lines on top.
    repeat(9) {
        val dark = rnd.nextBoolean()
        streak(
            width = geo.side * (0.04f + rnd.nextFloat() * 0.05f),
            color = (if (dark) theme.grainDark else theme.grainLight).copy(alpha = 0.05f + rnd.nextFloat() * 0.05f),
            amp = r * (0.04f + rnd.nextFloat() * 0.06f),
            waves = 1.2f + rnd.nextFloat() * 1.8f,
        )
    }
    repeat(72) {
        val dark = rnd.nextFloat() < 0.62f
        streak(
            width = geo.side * (0.003f + rnd.nextFloat() * 0.011f),
            color = (if (dark) theme.grainDark else theme.grainLight).copy(alpha = 0.08f + rnd.nextFloat() * 0.22f),
            amp = r * (0.02f + rnd.nextFloat() * 0.05f),
            waves = 1.5f + rnd.nextFloat() * 2.5f,
        )
    }
}

/** An empty position: a shallow dimple (wood), a neon socket (jewel) or a slate pit. */
private fun DrawScope.drawDimple(center: Offset, r: Float, theme: GameTheme) {
    when (theme.boardStyle) {
        BoardStyle.WOOD -> drawWoodDimple(center, r, theme)
        BoardStyle.NEON -> {
            // From the "socket empty" asset: black well, violet ring, tiny centre spark.
            drawCircle(color = theme.holeMid, radius = r * 1.12f, center = center)
            drawCircle(color = theme.holeCenter, radius = r * 0.95f, center = center)
            drawCircle(color = theme.holeRim.copy(alpha = 0.6f), radius = r * 0.92f, center = center, style = Stroke(width = r * 0.08f))
            drawCircle(color = theme.grainLight.copy(alpha = 0.8f), radius = r * 0.08f, center = center)
        }
        BoardStyle.SLATE -> {
            drawCircle(color = theme.holeCenter, radius = r * 0.78f, center = center)
            drawCircle(color = theme.holeRim, radius = r * 0.78f, center = center, style = Stroke(width = r * 0.08f))
        }
    }
}

private fun DrawScope.drawWoodDimple(center: Offset, r: Float, theme: GameTheme) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to theme.holeCenter.copy(alpha = 0.55f),
            0.75f to theme.holeMid.copy(alpha = 0.30f),
            1f to Color.Transparent,
            center = center + Offset(-r * 0.1f, -r * 0.15f),
            radius = r * 1.15f,
        ),
        radius = r * 1.1f,
        center = center,
    )
    // Faint lit lip on the lower-right edge.
    drawArc(
        color = Color(0x30FFFFFF),
        startAngle = 20f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = center - Offset(r, r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = r * 0.12f, cap = StrokeCap.Round),
    )
}

/** Marks a legal landing spot for the selected marble. */
private fun DrawScope.drawDestination(center: Offset, r: Float, theme: GameTheme) {
    drawCircle(color = theme.selectRing.copy(alpha = 0.33f), radius = r * 1.15f, center = center)
    drawCircle(
        color = theme.selectRing,
        radius = r * 0.98f,
        center = center,
        style = Stroke(width = r * 0.16f),
    )
}

// ---------------------------------------------------------------------------
// Marbles: polished gemstones with a soft sheen, a crisp highlight and mineral texture.
// ---------------------------------------------------------------------------

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
        if (theme.boardStyle == BoardStyle.NEON) {
            drawSelectionReticle(center, radius, theme, alpha)
        } else {
            drawCircle(color = theme.selectRing.copy(alpha = 0.53f), radius = radius * 1.28f, center = center, alpha = alpha)
            drawCircle(color = theme.selectRing.copy(alpha = 0.27f), radius = radius * 1.5f, center = center, alpha = alpha)
        }
    }

    // Jewels glow softly in their own colour.
    if (theme.boardStyle == BoardStyle.NEON) {
        drawCircle(
            brush = Brush.radialGradient(
                0.55f to gem.base.copy(alpha = 0.45f),
                1f to Color.Transparent,
                center = center,
                radius = radius * 1.4f,
            ),
            radius = radius * 1.4f,
            center = center,
            alpha = alpha,
        )
    }

    // Soft, tight contact shadow on the wood.
    val shadowC = center + Offset(radius * 0.10f, radius * 0.18f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0x80000000),
            0.7f to Color(0x40000000),
            1f to Color.Transparent,
            center = shadowC,
            radius = radius * 1.12f,
        ),
        radius = radius * 1.12f,
        center = shadowC,
        alpha = alpha,
    )

    // Sphere body, brightest toward the light.
    val lightAt = center + Offset(-radius * 0.30f, -radius * 0.34f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to gem.base.lighten(0.30f * gem.sheen.coerceAtMost(1.2f)),
            0.45f to gem.base,
            1f to gem.base.darken(0.50f),
            center = lightAt,
            radius = radius * 1.4f,
        ),
        radius = radius,
        center = center,
        alpha = alpha,
    )

    drawGemTexture(gem, center, radius, alpha)

    // Edge falloff so every stone reads as a round volume, whatever its pattern.
    drawCircle(
        brush = Brush.radialGradient(
            0.6f to Color.Transparent,
            1f to Color(0x59000000),
            center = center - Offset(radius * 0.12f, radius * 0.14f),
            radius = radius * 1.02f,
        ),
        radius = radius,
        center = center,
        alpha = alpha,
    )

    // Lit lower-right edge from light bouncing off the wood.
    drawArc(
        color = gem.base.lighten(0.45f).copy(alpha = 0.28f),
        startAngle = 20f,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = center - Offset(radius * 0.92f, radius * 0.92f),
        size = Size(radius * 1.84f, radius * 1.84f),
        style = Stroke(width = radius * 0.06f, cap = StrokeCap.Round),
        alpha = alpha,
    )

    // Broad soft sheen, like a window reflected in polished stone.
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.42f * gem.sheen.coerceAtMost(1.2f)),
            1f to Color.Transparent,
            center = lightAt,
            radius = radius * 0.62f,
        ),
        radius = radius * 0.62f,
        center = lightAt,
        alpha = alpha,
    )

    // Crisp specular highlight.
    drawCircle(
        color = Color.White.copy(alpha = 0.9f * gem.sheen.coerceAtMost(1.2f)),
        radius = radius * 0.15f,
        center = center - Offset(radius * 0.36f, radius * 0.40f),
        alpha = alpha,
    )

    // Defining rim.
    drawCircle(
        color = gem.base.darken(0.6f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.05f),
        alpha = alpha,
    )
}

/**
 * Jewel Atelier selection: magenta glow, dashed pink ring, cyan inner ring and four
 * white corner brackets (from the "selected stone ring" asset).
 */
private fun DrawScope.drawSelectionReticle(center: Offset, radius: Float, theme: GameTheme, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0.55f to theme.selectRing.copy(alpha = 0f),
            0.8f to theme.selectRing.copy(alpha = 0.55f),
            1f to theme.accent2.copy(alpha = 0f),
            center = center,
            radius = radius * 1.55f,
        ),
        radius = radius * 1.55f,
        center = center,
        alpha = alpha,
    )
    drawCircle(
        color = theme.selectRing,
        radius = radius * 1.38f,
        center = center,
        style = Stroke(width = radius * 0.07f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(radius * 0.22f, radius * 0.16f))),
        alpha = alpha,
    )
    drawCircle(color = theme.accent2.copy(alpha = 0.85f), radius = radius * 1.2f, center = center, style = Stroke(width = radius * 0.04f), alpha = alpha)
    val a = radius * 0.98f
    val l = radius * 0.32f
    val bracket = Path().apply {
        for ((sx, sy) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) {
            moveTo(center.x + sx * a, center.y + sy * (a - l))
            lineTo(center.x + sx * a, center.y + sy * a)
            lineTo(center.x + sx * (a - l), center.y + sy * a)
        }
    }
    drawPath(bracket, color = Color.White, style = Stroke(width = radius * 0.08f, cap = StrokeCap.Round), alpha = alpha)
}

/** Fixed unit-circle speckle positions (x, y, size) so Dalmatian-style stones never shimmer. */
private val SPECKS: List<Triple<Float, Float, Float>> = run {
    val rnd = Random(5)
    List(20) {
        Triple(rnd.nextFloat() * 1.7f - 0.85f, rnd.nextFloat() * 1.7f - 0.85f, 0.05f + rnd.nextFloat() * 0.06f)
    }
}

/** Veins as (startY, ctrlX, ctrlY, endY) across a unit circle, left edge to right edge. */
private val VEINS: List<FloatArray> = listOf(
    floatArrayOf(-0.35f, -0.15f, 0.35f, 0.15f),
    floatArrayOf(0.30f, 0.20f, -0.30f, 0.55f),
    floatArrayOf(-0.75f, 0.10f, -0.05f, -0.30f),
)

private fun DrawScope.drawGemTexture(gem: Gem, center: Offset, radius: Float, alpha: Float) {
    if (gem.texture == GemTexture.NONE) return
    val isLight = gem.base.luminance() > 0.35f
    val mark = if (isLight) gem.base.darken(0.65f) else gem.base.lighten(0.55f)
    clipPath(Path().apply { addOval(Rect(center, radius)) }) {
        when (gem.texture) {
            GemTexture.NONE -> Unit
            GemTexture.SPECKLE -> for ((x, y, s) in SPECKS) {
                drawCircle(
                    color = gem.base.darken(0.85f).copy(alpha = 0.9f),
                    radius = radius * s,
                    center = center + Offset(x * radius, y * radius),
                    alpha = alpha,
                )
            }
            GemTexture.VEINS -> for (v in VEINS) {
                val path = Path().apply {
                    moveTo(center.x - radius, center.y + v[0] * radius)
                    quadraticTo(center.x + v[1] * radius, center.y + v[2] * radius, center.x + radius, center.y + v[3] * radius)
                }
                drawPath(path, color = mark.copy(alpha = 0.6f), style = Stroke(width = radius * 0.06f, cap = StrokeCap.Round), alpha = alpha)
            }
            GemTexture.BANDS -> {
                val origin = center + Offset(radius * 0.30f, radius * 0.25f)
                listOf(0.30f, 0.55f, 0.80f, 1.05f).forEachIndexed { i, f ->
                    val c = if (i % 2 == 0) gem.base.darken(0.40f).copy(alpha = 0.6f) else gem.base.lighten(0.22f).copy(alpha = 0.45f)
                    drawCircle(color = c, radius = radius * f, center = origin, style = Stroke(width = radius * 0.11f), alpha = alpha)
                }
            }
            GemTexture.CLOUDY -> {
                fun blob(dx: Float, dy: Float, rr: Float, color: Color) {
                    val at = center + Offset(dx * radius, dy * radius)
                    drawCircle(
                        brush = Brush.radialGradient(0f to color, 1f to Color.Transparent, center = at, radius = radius * rr),
                        radius = radius * rr,
                        center = at,
                        alpha = alpha,
                    )
                }
                blob(-0.30f, -0.10f, 0.55f, gem.base.lighten(0.35f).copy(alpha = 0.40f))
                blob(0.35f, 0.30f, 0.50f, gem.base.darken(0.40f).copy(alpha = 0.40f))
                blob(0.10f, -0.45f, 0.40f, gem.base.lighten(0.25f).copy(alpha = 0.30f))
            }
        }
    }
}
