package com.spore.gemleap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.spore.gemleap.model.GameTheme
import com.spore.gemleap.model.Gem
import com.spore.gemleap.model.ThemeId
import kotlin.math.sin

/** Unit-space paths are built once, never randomized or rebuilt during animation. */
private object MineralPaths {
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(-1f, -1f, 1f, 1f)) }
    val bands = List(9) { i ->
        Path().apply {
            for (j in 0..40) {
                val x = -1.2f + j * 0.06f
                val y = -1.1f + i * 0.27f + sin(x * 4f + i * 0.7f) * 0.13f
                if (j == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }
    val grain = List(24) { i ->
        Path().apply {
            for (j in 0..48) {
                val x = -1f + j / 24f
                val y = -1f + i / 12f + sin(x * 5f + i * 0.51f) * 0.024f
                if (j == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }
    val veins = List(4) { i -> Path().apply {
        moveTo(-1f, -0.75f + i * 0.48f)
        cubicTo(-0.5f, -0.9f + i * 0.5f, -0.1f, 0.3f - i * 0.2f, 1f, -0.4f + i * 0.44f)
    } }
    val flecks = List(17) { i ->
        Offset(((i * 37 % 91) / 50f) - 0.9f, ((i * 61 % 87) / 50f) - 0.86f)
    }
}

/** Reusable board material shared by gameplay and theme thumbnails. */
internal fun DrawScope.drawAtelierBoard(center: Offset, radius: Float, theme: GameTheme) {
    drawCircle(Color.Black.copy(alpha = 0.18f), radius * 1.018f, center + Offset(0f, radius * 0.028f))
    drawCircle(Brush.linearGradient(listOf(theme.discOuter, theme.discMid, theme.discInner),
        center - Offset(radius, radius), center + Offset(radius, radius)), radius, center)
    drawCircle(theme.accent.copy(alpha = 0.58f), radius * 0.969f, center, style = Stroke(radius * 0.006f))
    drawCircle(Brush.radialGradient(listOf(theme.surfaceOuter, theme.surfaceInner),
        center - Offset(radius * 0.35f, radius * 0.4f), radius * 1.6f), radius * 0.925f, center)
    withTransform({ translate(center.x, center.y); scale(radius * 0.925f, radius * 0.925f, Offset.Zero) }) {
        clipPath(MineralPaths.circle) {
            MineralPaths.grain.forEachIndexed { i, path ->
                drawPath(path, if (i % 3 == 0) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.09f), style = Stroke(0.003f))
            }
        }
    }
    drawCircle(Color.Black.copy(alpha = 0.32f), radius * 0.925f, center, style = Stroke(radius * 0.012f))
    drawArc(Color.White.copy(alpha = 0.19f), 205f, 95f, false,
        center - Offset(radius * 0.987f, radius * 0.987f), Size(radius * 1.974f, radius * 1.974f), style = Stroke(radius * 0.006f))
}

internal fun DrawScope.drawAtelierSocket(center: Offset, radius: Float, theme: GameTheme, target: Boolean = false) {
    drawCircle(theme.holeRim.copy(alpha = 0.7f), radius * 1.08f, center + Offset(0f, radius * 0.05f))
    drawCircle(Brush.radialGradient(listOf(theme.holeCenter, theme.holeMid), center - Offset(0f, radius * 0.25f), radius * 1.2f), radius, center)
    if (target) {
        drawCircle(theme.hintRing, radius * 1.12f, center, style = Stroke(radius * 0.11f))
        drawCircle(theme.hintRing, radius * 0.22f, center)
    }
}

internal fun DrawScope.drawAtelierGem(center: Offset, radius: Float, gem: Gem, material: Int,
    theme: GameTheme, selected: Boolean = false, alpha: Float = 1f) {
    val lifted = if (selected) center - Offset(0f, radius * 0.09f) else center
    if (selected) {
        drawCircle(theme.accent.copy(alpha = alpha), radius * 1.22f, center, style = Stroke(radius * 0.065f))
    }
    drawCircle(Color.Black.copy(alpha = 0.36f * alpha), radius * 0.97f, center + Offset(radius * 0.12f, radius * 0.23f))
    withTransform({ translate(lifted.x, lifted.y); scale(radius, radius, Offset.Zero) }) {
        clipPath(MineralPaths.circle) {
            drawCircle(Brush.radialGradient(listOf(gem.base.lighten(0.38f), gem.base, gem.base.darken(0.64f)),
                Offset(-0.35f, -0.42f), 1.65f), 1f, Offset.Zero, alpha = alpha)
            if (theme.id == ThemeId.ATELIER) when (material % 6) {
                0 -> MineralPaths.bands.forEachIndexed { i, path -> drawPath(path,
                    (if (i % 2 == 0) Color(0xFF063F32) else Color(0xFF9DF2C4)).copy(alpha = 0.30f * alpha), style = Stroke(0.07f)) }
                1 -> MineralPaths.flecks.forEachIndexed { i, pt -> drawCircle(Color(0xFFEACB88).copy(alpha = 0.65f * alpha),
                    if (i % 3 == 0) 0.028f else 0.015f, pt) }
                2 -> MineralPaths.veins.forEach { drawPath(it, Color(0xFF165D60).copy(alpha = 0.38f * alpha), style = Stroke(0.023f)) }
                3 -> { drawCircle(Color(0xFFFFE6A1).copy(alpha = 0.24f * alpha), 0.6f, Offset(-0.17f, -0.05f))
                    MineralPaths.veins.take(2).forEach { drawPath(it, Color(0xFF7A380E).copy(alpha = 0.19f * alpha), style = Stroke(0.05f)) } }
                4 -> MineralPaths.bands.filterIndexed { i, _ -> i % 2 == 0 }.forEach {
                    drawPath(it, Color(0xFFE6CEFF).copy(alpha = 0.18f * alpha), style = Stroke(0.13f)) }
                5 -> drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f * alpha), Color.Transparent),
                    Offset(-0.15f, -0.25f), 1.2f), 1f, Offset.Zero)
            }
            drawOval(Color.White.copy(alpha = 0.33f * alpha), Offset(-0.63f, -0.72f), Size(0.68f, 0.32f))
            drawOval(Color.White.copy(alpha = 0.78f * alpha), Offset(-0.49f, -0.65f), Size(0.27f, 0.15f))
            drawArc(gem.base.lighten(0.4f).copy(alpha = 0.3f * alpha), 35f, 65f, false,
                Offset(-0.8f, -0.8f), Size(1.6f, 1.6f), style = Stroke(0.055f))
        }
        drawCircle(gem.base.darken(0.52f).copy(alpha = alpha), 1f, Offset.Zero, style = Stroke(0.035f))
    }
}
