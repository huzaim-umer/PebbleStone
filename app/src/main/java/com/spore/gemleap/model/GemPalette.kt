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

/**
 * "Lamplight" palette - muted, atmospheric stones for a dark, moody board, with a
 * few warm lamp-lit standouts (amber gold, bone) glowing against the shadow.
 */
object GemPalette {
    val gems: List<Gem> = listOf(
        Gem("Lamp Gold", Color(0xFFE6B84C), sheen = 1.15f),
        Gem("Deep Teal", Color(0xFF2E8B8B)),
        Gem("Dusk Rose", Color(0xFFB5687A)),
        Gem("Slate Blue", Color(0xFF3E5C86)),
        Gem("Moss", Color(0xFF6E8B6A), sheen = 0.85f),
        Gem("Dusty Plum", Color(0xFF6E5A86)),
        Gem("Ember", Color(0xFFC87A3E)),
        Gem("Bone", Color(0xFFD8D2C4), sheen = 0.75f),
        Gem("Rust Red", Color(0xFF9A3A3A), sheen = 1.1f),
        Gem("Steel", Color(0xFF4A5560), sheen = 0.9f),
        Gem("Olive", Color(0xFF8A8A50), sheen = 0.85f),
        Gem("Wine", Color(0xFF7A4A5A)),
        Gem("Fog Teal", Color(0xFF5E8A8A), sheen = 0.85f),
        Gem("Indigo", Color(0xFF44487A), sheen = 1.1f),
    )

    val count: Int get() = gems.size

    operator fun get(index: Int): Gem = gems[((index % count) + count) % count]
}
