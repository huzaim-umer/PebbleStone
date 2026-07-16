package com.example.pebblestone.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Blend [this] toward white by [amount] (0f..1f). */
fun Color.lighten(amount: Float): Color = lerp(this, Color.White, amount.coerceIn(0f, 1f))

/** Blend [this] toward black by [amount] (0f..1f). */
fun Color.darken(amount: Float): Color = lerp(this, Color.Black, amount.coerceIn(0f, 1f))
