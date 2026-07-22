package com.spore.gemleap.model

import androidx.compose.ui.graphics.Color

/**
 * A stylised semi-precious stone. [base] is the mid tone; the renderer derives a
 * lighter highlight and darker rim from it to fake a polished sphere. [sheen]
 * scales the specular highlight (glassy stones get a brighter dot than matte ones).
 */
data class Gem(
    val name: String,
    val base: Color,
    val sheen: Float = 1f,
)

/** Gemstone marbles inspired by the photographed board. */
object GemPalette {
    val gems: List<Gem> = listOf(
        Gem("Onyx", Color(0xFF23232A), sheen = 1.15f),
        Gem("Snow Quartz", Color(0xFFF1EEE7), sheen = 0.7f),
        Gem("Malachite", Color(0xFF1F7A4D)),
        Gem("Aventurine", Color(0xFF7FB98E), sheen = 0.8f),
        Gem("Turquoise", Color(0xFF3FC0C4)),
        Gem("Amazonite", Color(0xFF83D2C0), sheen = 0.8f),
        Gem("Lapis Lazuli", Color(0xFF27509B)),
        Gem("Sodalite", Color(0xFF35508A)),
        Gem("Blue Goldstone", Color(0xFF2A2A5A), sheen = 1.2f),
        Gem("Amethyst", Color(0xFF8E5AB8)),
        Gem("Rose Quartz", Color(0xFFE9B4C2), sheen = 0.75f),
        Gem("Red Jasper", Color(0xFFB23A2E)),
        Gem("Carnelian", Color(0xFFC15A2A)),
        Gem("Tiger Eye", Color(0xFF9A6B2F), sheen = 1.1f),
        Gem("Sunstone", Color(0xFFB5763B)),
    )

    val count: Int get() = gems.size

    operator fun get(index: Int): Gem = gems[((index % count) + count) % count]
}
