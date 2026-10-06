package com.spore.gemleap.model

import androidx.compose.ui.graphics.Color

/** Selectable visual themes. */
enum class ThemeId { CLASSIC, LAMPLIGHT, VICTORIAN, ATELIER }

/**
 * A full visual theme: screen colours, board/hole colours, accent, and the
 * marble palette. The renderer reads everything from here so the look can be
 * swapped at runtime.
 */
data class GameTheme(
    val id: ThemeId,
    val label: String,
    // screen
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color,
    val secondaryText: Color,
    val bannerBg: Color,
    // board disk
    val discOuter: Color,
    val discMid: Color,
    val discInner: Color,
    val surfaceOuter: Color,
    val surfaceInner: Color,
    // holes
    val holeCenter: Color,
    val holeMid: Color,
    val holeRim: Color,
    // overlays
    val hintRing: Color,
    // marbles
    val gems: List<Gem>,
)

object GameThemes {
    private val CLASSIC_GEMS = listOf(
        Gem("Onyx", Color(0xFF23232A), 1.15f),
        Gem("Snow Quartz", Color(0xFFF1EEE7), 0.7f),
        Gem("Malachite", Color(0xFF1F7A4D)),
        Gem("Aventurine", Color(0xFF7FB98E), 0.8f),
        Gem("Turquoise", Color(0xFF3FC0C4)),
        Gem("Amazonite", Color(0xFF83D2C0), 0.8f),
        Gem("Lapis Lazuli", Color(0xFF27509B)),
        Gem("Sodalite", Color(0xFF35508A)),
        Gem("Blue Goldstone", Color(0xFF2A2A5A), 1.2f),
        Gem("Amethyst", Color(0xFF8E5AB8)),
        Gem("Rose Quartz", Color(0xFFE9B4C2), 0.75f),
        Gem("Red Jasper", Color(0xFFB23A2E)),
        Gem("Carnelian", Color(0xFFC15A2A)),
        Gem("Tiger Eye", Color(0xFF9A6B2F), 1.1f),
        Gem("Sunstone", Color(0xFFB5763B)),
    )

    private val LAMPLIGHT_GEMS = listOf(
        Gem("Lamp Gold", Color(0xFFE6B84C), 1.15f),
        Gem("Deep Teal", Color(0xFF2E8B8B)),
        Gem("Dusk Rose", Color(0xFFB5687A)),
        Gem("Slate Blue", Color(0xFF3E5C86)),
        Gem("Moss", Color(0xFF6E8B6A), 0.85f),
        Gem("Dusty Plum", Color(0xFF6E5A86)),
        Gem("Ember", Color(0xFFC87A3E)),
        Gem("Bone", Color(0xFFD8D2C4), 0.75f),
        Gem("Rust Red", Color(0xFF9A3A3A), 1.1f),
        Gem("Steel", Color(0xFF4A5560), 0.9f),
        Gem("Olive", Color(0xFF8A8A50), 0.85f),
        Gem("Wine", Color(0xFF7A4A5A)),
        Gem("Fog Teal", Color(0xFF5E8A8A), 0.85f),
        Gem("Indigo", Color(0xFF44487A), 1.1f),
    )

    // A gaslit, aged-brass 1800s look: dark walnut and leather, antique gold
    // accents, and muted period minerals (jet, ivory, garnet, verdigris).
    private val VICTORIAN_GEMS = listOf(
        Gem("Jet", Color(0xFF1C1A18), 1.15f),
        Gem("Ivory", Color(0xFFE8DEC8), 0.7f),
        Gem("Brass", Color(0xFFB08D4C), 1.1f),
        Gem("Garnet", Color(0xFF7E2B2B)),
        Gem("Emerald", Color(0xFF2F6B4A)),
        Gem("Sapphire", Color(0xFF32507E)),
        Gem("Amber", Color(0xFFB07A2E)),
        Gem("Bronze", Color(0xFF8A6A3E), 1.1f),
        Gem("Pearl", Color(0xFFD9D2C2), 0.75f),
        Gem("Amethyst", Color(0xFF6E4C86)),
        Gem("Verdigris", Color(0xFF4E8A7A), 0.9f),
        Gem("Mahogany", Color(0xFF6A3A2A)),
        Gem("Pewter", Color(0xFF6A6E72), 0.9f),
        Gem("Teal Enamel", Color(0xFF3A6E78)),
    )

    val CLASSIC = GameTheme(
        id = ThemeId.CLASSIC,
        label = "Classic Wood",
        bgTop = Color(0xFF3A2718), bgBottom = Color(0xFF2A1B10),
        accent = Color(0xFFE0A458), secondaryText = Color(0xFFCBB79E),
        bannerBg = Color(0xEE1E140A),
        discOuter = Color(0xFF9A6A3E), discMid = Color(0xFF6B4526), discInner = Color(0xFF3A2614),
        surfaceOuter = Color(0xFF7A5330), surfaceInner = Color(0xFF4A3018),
        holeCenter = Color(0xFF14100A), holeMid = Color(0xFF241A0F), holeRim = Color(0xFF5A3E22),
        hintRing = Color(0xFF6BD37A),
        gems = CLASSIC_GEMS,
    )

    val LAMPLIGHT = GameTheme(
        id = ThemeId.LAMPLIGHT,
        label = "Lamplight",
        bgTop = Color(0xFF1A1D22), bgBottom = Color(0xFF0A0B0E),
        accent = Color(0xFFF2C94C), secondaryText = Color(0xFFA8ADB5),
        bannerBg = Color(0xEE14161C),
        discOuter = Color(0xFF3A3F48), discMid = Color(0xFF2A2D34), discInner = Color(0xFF14161B),
        surfaceOuter = Color(0xFF2E323A), surfaceInner = Color(0xFF16181E),
        holeCenter = Color(0xFF08090C), holeMid = Color(0xFF12141A), holeRim = Color(0xFF2C313A),
        hintRing = Color(0xFF6BD37A),
        gems = LAMPLIGHT_GEMS,
    )

    val VICTORIAN = GameTheme(
        id = ThemeId.VICTORIAN,
        label = "Victorian",
        bgTop = Color(0xFF2E2A22), bgBottom = Color(0xFF1A1710),
        accent = Color(0xFFC8A860), secondaryText = Color(0xFFB7A98C),
        bannerBg = Color(0xEE181309),
        discOuter = Color(0xFF6E5A3E), discMid = Color(0xFF4E3F2A), discInner = Color(0xFF2E2418),
        surfaceOuter = Color(0xFF5A4A32), surfaceInner = Color(0xFF342916),
        holeCenter = Color(0xFF100B06), holeMid = Color(0xFF1E160D), holeRim = Color(0xFF4A3A24),
        hintRing = Color(0xFF8FB36E),
        gems = VICTORIAN_GEMS,
    )

    val ATELIER = GameTheme(
        id = ThemeId.ATELIER, label = "Jewel Atelier",
        bgTop = Color(0xFF123B3A), bgBottom = Color(0xFF061C20),
        accent = Color(0xFFEACB88), secondaryText = Color(0xFFB8CEC7),
        bannerBg = Color(0xFF123B3A),
        discOuter = Color(0xFFAD8052), discMid = Color(0xFF523527), discInner = Color(0xFF201811),
        surfaceOuter = Color(0xFF76503B), surfaceInner = Color(0xFF38271E),
        holeCenter = Color(0xFF100F0D), holeMid = Color(0xFF241D16), holeRim = Color(0xFF997453),
        hintRing = Color(0xFF8BF0C4),
        gems = listOf(
            Gem("Malachite", Color(0xFF20B984)), Gem("Lapis", Color(0xFF387CE0)),
            Gem("Turquoise", Color(0xFF42C6C5)), Gem("Amber", Color(0xFFE1A244)),
            Gem("Amethyst", Color(0xFF9673D6)), Gem("Pearl", Color(0xFFE4E2CB), 0.7f),
        ),
    )

    val all = listOf(ATELIER, CLASSIC, LAMPLIGHT, VICTORIAN)

    fun of(id: ThemeId): GameTheme = when (id) {
        ThemeId.ATELIER -> ATELIER
        ThemeId.LAMPLIGHT -> LAMPLIGHT
        ThemeId.VICTORIAN -> VICTORIAN
        ThemeId.CLASSIC -> CLASSIC
    }
}
