package com.spore.gemleap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Lamplight" theme: dark charcoal surfaces with a warm lamp-yellow accent.
// Names kept (WoodDark/Mid/Light) to avoid churn; values are now charcoal tones.
val WoodDark = Color(0xFF0A0B0E)   // darkest background / surface
val WoodMid = Color(0xFF2A2D34)    // board mid tone
val WoodLight = Color(0xFF34383F)  // board light tone
val Amber = Color(0xFFF2C94C)      // warm lamp-glow accent

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = WoodDark,
    background = WoodDark,
    surface = Color(0xFF1E2228),
    onBackground = Color(0xFFE8E4DC),
    onSurface = Color(0xFFE8E4DC),
)

private val LightColors = lightColorScheme(
    primary = WoodMid,
    onPrimary = Color.White,
    background = Color(0xFFF4E9DA),
    surface = Color(0xFFEADBC6),
    onBackground = WoodDark,
    onSurface = WoodDark,
)

@Composable
fun PebbleStoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
