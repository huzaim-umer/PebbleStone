package com.example.pebblestone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pebblestone.ui.GameScreen
import com.example.pebblestone.ui.theme.PebbleStoneTheme
import com.example.pebblestone.ui.theme.WoodDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PebbleStoneTheme(darkTheme = true) {
                Surface(color = WoodDark) {
                    GameScreen(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    )
                }
            }
        }
    }
}
