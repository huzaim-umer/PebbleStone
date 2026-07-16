package com.example.pebblestone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WoodDark = Color(0xFF2A1B10)
val WoodMid = Color(0xFF6B4526)
val WoodLight = Color(0xFF9A6A3E)
val Amber = Color(0xFFE0A458)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = WoodDark,
    background = WoodDark,
    surface = Color(0xFF3A2718),
    onBackground = Color(0xFFF3E7D8),
    onSurface = Color(0xFFF3E7D8),
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
