package com.spore.gemleap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.spore.gemleap.model.GameTheme
import com.spore.gemleap.model.GameThemes

val WoodDark = Color(0xFF061C20)
val AtelierIvory = Color(0xFFF6F0E5)

@Composable
fun PebbleStoneTheme(
    darkTheme: Boolean = true,
    theme: GameTheme = GameThemes.ATELIER,
    content: @Composable () -> Unit,
) {
    // Every selectable board theme is dark; retained parameter preserves callers.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = theme.accent, onPrimary = theme.bgBottom,
            secondary = theme.hintRing, onSecondary = theme.bgBottom,
            background = theme.bgBottom, surface = theme.bannerBg,
            surfaceVariant = theme.bgTop, onSurfaceVariant = theme.secondaryText,
            onBackground = AtelierIvory, onSurface = AtelierIvory,
            outline = theme.secondaryText.copy(alpha = 0.45f),
        ), content = content,
    )
}
