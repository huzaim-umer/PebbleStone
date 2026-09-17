package com.spore.gemleap.model

import androidx.compose.ui.graphics.Color

/**
 * A stylised marble. [base] is the mid tone; the renderer derives a lighter
 * highlight and darker rim from it to fake a polished sphere. [sheen] scales the
 * specular highlight (glassy stones get a brighter dot than matte ones).
 */
data class Gem(
    val name: String,
    val base: Color,
    val sheen: Float = 1f,
)

/** Marble-slot bookkeeping. Actual colours live per-theme in [GameThemes]. */
object GemPalette {
    /** Number of distinct marble slots a board is coloured from (indices are hashed mod this). */
    const val SLOTS = 14
}
