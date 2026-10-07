package com.spore.gemleap.model

import androidx.compose.ui.graphics.Color

/** Selectable visual themes. */
enum class ThemeId { JEWEL, CLASSIC, ZEN }

/** How the board itself is drawn: turned wood, a neon-ringed jewel case, or matte slate. */
enum class BoardStyle { WOOD, NEON, SLATE }

/**
 * A full visual theme: screen colours, board/hole colours, dialog colours and the
 * marble palette. The renderer and every screen read from here, so the whole look
 * can be swapped at runtime.
 */
data class GameTheme(
    val id: ThemeId,
    val label: String,
    val isLight: Boolean,
    val boardStyle: BoardStyle,
    // screen
    val bgTop: Color,
    val bgBottom: Color,
    /** Title "Gem", big numbers, dialog titles. */
    val textPrimary: Color,
    /** "Leap" gradient start, icons, highlights. */
    val accent: Color,
    /** "Leap" gradient end, secondary icon colour. */
    val accent2: Color,
    /** Small-caps labels (STONES LEFT, subtitle). */
    val labelText: Color,
    /** Taglines, captions, dialog body text. */
    val secondaryText: Color,
    /** Least prominent text (Remove ads, dialog subtitles). */
    val mutedText: Color,
    /** Surface of buttons, tiles and the mode switch. */
    val panel: Color,
    val panelBorder: Color,
    /** Selected Free/Daily pill. */
    val pillStart: Color,
    val pillEnd: Color,
    val pillText: Color,
    val bannerBg: Color,
    // board disk
    val discOuter: Color,
    val discMid: Color,
    val discInner: Color,
    val surfaceOuter: Color,
    val surfaceInner: Color,
    // wood grain streaks (WOOD) or decorative rings (NEON / SLATE)
    val grainDark: Color,
    val grainLight: Color,
    // holes
    val holeCenter: Color,
    val holeMid: Color,
    val holeRim: Color,
    // overlays
    val hintRing: Color,
    /** Selected-marble glow and legal-destination markers. */
    val selectRing: Color,
    // dialogs
    val dialogTop: Color,
    val dialogBottom: Color,
    val dialogBorderStart: Color,
    val dialogBorderEnd: Color,
    val ctaStart: Color,
    val ctaMid: Color,
    val ctaEnd: Color,
    val ctaText: Color,
    /** Primary text action (Done, Close) and checkmarks. */
    val link: Color,
    /** Secondary text action (Keep thinking, Restore). */
    val linkSoft: Color,
    val rowBg: Color,
    val rowSelectedBg: Color,
    val rowBorder: Color,
    // marbles
    val gems: List<Gem>,
)

object GameThemes {
    private val CLASSIC_GEMS = listOf(
        Gem("Onyx", Color(0xFF23232A), 1.15f),
        Gem("Snow Quartz", Color(0xFFF1EEE7), 0.7f),
        Gem("Malachite", Color(0xFF1F7A4D), texture = GemTexture.BANDS),
        Gem("Aventurine", Color(0xFF7FB98E), 0.8f),
        Gem("Turquoise", Color(0xFF3FC0C4), texture = GemTexture.VEINS),
        Gem("Amazonite", Color(0xFF83D2C0), 0.8f),
        Gem("Lapis Lazuli", Color(0xFF27509B), texture = GemTexture.CLOUDY),
        Gem("Dalmatian Jasper", Color(0xFFE2DCC0), 0.8f, GemTexture.SPECKLE),
        Gem("Blue Goldstone", Color(0xFF2A2A5A), 1.2f),
        Gem("Amethyst", Color(0xFF8E5AB8), texture = GemTexture.CLOUDY),
        Gem("Rose Quartz", Color(0xFFE9B4C2), 0.75f),
        Gem("Red Jasper", Color(0xFFB23A2E)),
        Gem("Carnelian", Color(0xFFC15A2A)),
        Gem("Tiger Eye", Color(0xFF9A6B2F), 1.1f),
        Gem("Sunstone", Color(0xFFB5763B)),
    )

    // Vivid, glassy jewels that glow against the dark violet case.
    private val JEWEL_GEMS = listOf(
        Gem("Aquamarine", Color(0xFF22D3EE), 1.2f, GemTexture.VEINS),
        Gem("Moonstone", Color(0xFFF1F5F9), 1.0f),
        Gem("Citrine", Color(0xFFF59E0B), 1.15f, GemTexture.CLOUDY),
        Gem("Sapphire", Color(0xFF2563EB), 1.2f),
        Gem("Emerald", Color(0xFF10B981), 1.15f),
        Gem("Amethyst", Color(0xFFA855F7), 1.2f),
        Gem("Ruby", Color(0xFFEF4444), 1.15f),
        Gem("Pink Tourmaline", Color(0xFFEC4899), 1.1f),
    )

    // Soft, matte river stones on dark slate.
    private val ZEN_GEMS = listOf(
        Gem("Slate", Color(0xFF4E6A8A), 0.55f),
        Gem("Pearl", Color(0xFFF2F0EA), 0.5f),
        Gem("Sage", Color(0xFF5E8A6A), 0.55f),
        Gem("Ochre", Color(0xFFD9A441), 0.6f),
        Gem("Blush", Color(0xFFE8B4B8), 0.55f),
    )

    val JEWEL = GameTheme(
        id = ThemeId.JEWEL,
        label = "Jewel Atelier",
        isLight = false,
        boardStyle = BoardStyle.NEON,
        bgTop = Color(0xFF17123A), bgBottom = Color(0xFF07061A),
        textPrimary = Color(0xFFFFFFFF),
        accent = Color(0xFFC084FC), accent2 = Color(0xFF38BDF8),
        labelText = Color(0xFFD8B4FE), secondaryText = Color(0xFFCBD5E1), mutedText = Color(0xFF8B8FB3),
        panel = Color(0xFF110B26), panelBorder = Color(0xFF2E2152),
        pillStart = Color(0xFF38BDF8), pillEnd = Color(0xFF8B5CF6), pillText = Color(0xFFFFFFFF),
        bannerBg = Color(0xEE120926),
        discOuter = Color(0xFF160F2E), discMid = Color(0xFF0C071D), discInner = Color(0xFF080414),
        surfaceOuter = Color(0xFF382163), surfaceInner = Color(0xFF090515),
        grainDark = Color(0xFF251642), grainLight = Color(0xFFC084FC),
        holeCenter = Color(0xFF06050B), holeMid = Color(0xFF0D0B1A), holeRim = Color(0xFFA855F7),
        hintRing = Color(0xFF38BDF8),
        selectRing = Color(0xFFF0ABFC),
        dialogTop = Color(0xFF1E133D), dialogBottom = Color(0xFF090416),
        dialogBorderStart = Color(0xFFC084FC), dialogBorderEnd = Color(0xFF38BDF8),
        ctaStart = Color(0xFFC084FC), ctaMid = Color(0xFF9333EA), ctaEnd = Color(0xFF6366F1),
        ctaText = Color(0xFFFFFFFF),
        link = Color(0xFF38BDF8), linkSoft = Color(0xFFA78BFA),
        rowBg = Color(0xFF110924), rowSelectedBg = Color(0xFF2E1650), rowBorder = Color(0xFF3A2160),
        gems = JEWEL_GEMS,
    )

    val CLASSIC = GameTheme(
        id = ThemeId.CLASSIC,
        label = "Classic Wood",
        isLight = false,
        boardStyle = BoardStyle.WOOD,
        bgTop = Color(0xFF3A2718), bgBottom = Color(0xFF1C130B),
        textPrimary = Color(0xFFF6E7D2),
        accent = Color(0xFFF0C27B), accent2 = Color(0xFFD9823F),
        labelText = Color(0xFFE8C18A), secondaryText = Color(0xFFCBB79E), mutedText = Color(0xFF9C8670),
        panel = Color(0xFF2B1C11), panelBorder = Color(0xFF5A3E22),
        pillStart = Color(0xFFF0C27B), pillEnd = Color(0xFFC8763A), pillText = Color(0xFF2A1B10),
        bannerBg = Color(0xEE1E140A),
        discOuter = Color(0xFFA5622F), discMid = Color(0xFF7A4423), discInner = Color(0xFF4A2512),
        surfaceOuter = Color(0xFF9C5A2C), surfaceInner = Color(0xFF7A4423),
        grainDark = Color(0xFF2E1408), grainLight = Color(0xFFCB8C54),
        holeCenter = Color(0xFF14100A), holeMid = Color(0xFF241A0F), holeRim = Color(0xFF5A3E22),
        hintRing = Color(0xFF6BD37A),
        selectRing = Color(0xFFF0C27B),
        dialogTop = Color(0xFF3D2919), dialogBottom = Color(0xFF1A110A),
        dialogBorderStart = Color(0xFFF0C27B), dialogBorderEnd = Color(0xFF8A5A2B),
        ctaStart = Color(0xFFF5D199), ctaMid = Color(0xFFE0A458), ctaEnd = Color(0xFFB5763B),
        ctaText = Color(0xFF2A1B10),
        link = Color(0xFFF0C27B), linkSoft = Color(0xFFCBB79E),
        rowBg = Color(0xFF26180E), rowSelectedBg = Color(0xFF4A3018), rowBorder = Color(0xFF5A3E22),
        gems = CLASSIC_GEMS,
    )

    val ZEN = GameTheme(
        id = ThemeId.ZEN,
        label = "Zen Stone",
        isLight = true,
        boardStyle = BoardStyle.SLATE,
        bgTop = Color(0xFFF4F2ED), bgBottom = Color(0xFFE4E0D7),
        textPrimary = Color(0xFF1F2420),
        accent = Color(0xFF2B302C), accent2 = Color(0xFF6E8B74),
        labelText = Color(0xFF6B6A64), secondaryText = Color(0xFF55544E), mutedText = Color(0xFF8E8B83),
        panel = Color(0xFFEAE6DE), panelBorder = Color(0xFFD5CFC3),
        pillStart = Color(0xFF2B302C), pillEnd = Color(0xFF1F2420), pillText = Color(0xFFFFFFFF),
        bannerBg = Color(0xF2F8F6F1),
        discOuter = Color(0xFF2E332F), discMid = Color(0xFF222622), discInner = Color(0xFF161916),
        surfaceOuter = Color(0xFF1D211E), surfaceInner = Color(0xFF131613),
        grainDark = Color(0xFF0B0D0B), grainLight = Color(0xFF3A403B),
        holeCenter = Color(0xFF050605), holeMid = Color(0xFF0E100E), holeRim = Color(0xFF353B36),
        hintRing = Color(0xFFD9A441),
        selectRing = Color(0xFFE8C47A),
        dialogTop = Color(0xFFFBFAF7), dialogBottom = Color(0xFFECE8E0),
        dialogBorderStart = Color(0xFFCFC8BA), dialogBorderEnd = Color(0xFFB4AC9C),
        ctaStart = Color(0xFF3B453E), ctaMid = Color(0xFF2B302C), ctaEnd = Color(0xFF1F2420),
        ctaText = Color(0xFFFFFFFF),
        link = Color(0xFF2B302C), linkSoft = Color(0xFF6B6A64),
        rowBg = Color(0xFFF3F0EA), rowSelectedBg = Color(0xFFE6E1D6), rowBorder = Color(0xFFDDD6C9),
        gems = ZEN_GEMS,
    )

    val all = listOf(JEWEL, CLASSIC, ZEN)

    fun of(id: ThemeId): GameTheme = when (id) {
        ThemeId.JEWEL -> JEWEL
        ThemeId.CLASSIC -> CLASSIC
        ThemeId.ZEN -> ZEN
    }
}
