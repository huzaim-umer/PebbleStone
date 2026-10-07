package com.spore.gemleap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.spore.gemleap.model.GameTheme

/**
 * Line icons from the GemLeap UI kit (48x48 viewport), recoloured per theme:
 * [GameTheme.accent] for the main stroke and [GameTheme.accent2] for the detail.
 * Draw them with `tint = Color.Unspecified` so both colours survive.
 */
class GemIcons(
    val hint: ImageVector,
    val undo: ImageVector,
    val newGame: ImageVector,
    val soundOn: ImageVector,
    val soundOff: ImageVector,
    val stats: ImageVector,
    val themeToggle: ImageVector,
    val trophy: ImageVector,
)

@Composable
fun rememberGemIcons(theme: GameTheme): GemIcons = remember(theme.id) {
    val a = theme.accent
    val b = theme.accent2
    val hi = if (theme.isLight) theme.accent else Color.White
    GemIcons(
        hint = icon(
            "hint",
            Stroke("M24 6a11 11 0 0 0-8 18.5V29a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-4.5A11 11 0 0 0 24 6z", a, fill = a),
            Stroke("M19 36h10M21 40h6", b),
            Stroke("M24 13v6M21 16h6", hi, width = 2f),
        ),
        undo = icon(
            "undo",
            Stroke("M14 20H30a9 9 0 0 1 9 9v1a9 9 0 0 1-9 9H17", a),
            Stroke("M21 13l-7 7 7 7", b),
        ),
        newGame = icon(
            "newGame",
            Stroke("M38 24a14 14 0 1 1-4.1-9.9L38 18", b),
            Stroke("M38 10v8h-8", a),
        ),
        soundOn = icon(
            "soundOn",
            Stroke("M12 28h6l8 7V13l-8 7h-6a2 2 0 0 0-2 2v4a2 2 0 0 0 2 2z", a, fill = a),
            Stroke("M32 18a8 8 0 0 1 0 12M37 13a15 15 0 0 1 0 22", b),
        ),
        soundOff = icon(
            "soundOff",
            Stroke("M12 28h6l8 7V13l-8 7h-6a2 2 0 0 0-2 2v4a2 2 0 0 0 2 2z", a, fill = a),
            Stroke("M32 19l10 10M42 19l-10 10", b),
        ),
        stats = icon(
            "stats",
            Stroke("M10 28h4v12h-4z", b, fill = b, fillAlpha = 0.85f, width = 2f),
            Stroke("M22 18h4v22h-4z", a, fill = a, fillAlpha = 0.85f, width = 2f),
            Stroke("M34 9h4v31h-4z", b, fill = b, fillAlpha = 0.85f, width = 2f),
        ),
        themeToggle = icon(
            "theme",
            Stroke("M24 16a8 8 0 1 1 0 16a8 8 0 1 1 0-16z", a, fill = a),
            Stroke(
                "M24 6v4M24 38v4M6 24h4M38 24h4M11.3 11.3l2.8 2.8M33.9 33.9l2.8 2.8M11.3 36.7l2.8-2.8M33.9 14.1l2.8-2.8",
                b,
            ),
        ),
        trophy = icon(
            "trophy",
            Stroke("M15 8h18v9a9 9 0 0 1-18 0z", a, fill = a),
            Stroke("M15 11h-5a5 5 0 0 0 6 7M33 11h5a5 5 0 0 1-6 7", b),
            Stroke("M24 26v7M17 40h14M19 33h10", b),
        ),
    )
}

/** The multicolour Google "G" mark, for the Sign in with Google button. */
val GoogleG: ImageVector by lazy {
    ImageVector.Builder("googleG", 20.dp, 20.dp, 48f, 48f).apply {
        fun part(d: String, color: Long) = addPath(addPathNodes(d), fill = SolidColor(Color(color)))
        part("M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z", 0xFFEA4335)
        part("M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z", 0xFF4285F4)
        part("M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z", 0xFFFBBC05)
        part("M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z", 0xFF34A853)
    }.build()
}

private class Stroke(
    val d: String,
    val color: Color,
    val width: Float = 2.5f,
    val fill: Color? = null,
    val fillAlpha: Float = 0.2f,
)

private fun icon(name: String, vararg parts: Stroke): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 48f, 48f).apply {
        for (p in parts) {
            addPath(
                pathData = addPathNodes(p.d),
                fill = p.fill?.let { SolidColor(it) },
                fillAlpha = p.fillAlpha,
                stroke = SolidColor(p.color),
                strokeLineWidth = p.width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
