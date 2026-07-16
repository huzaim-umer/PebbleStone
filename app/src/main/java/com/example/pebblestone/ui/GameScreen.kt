package com.example.pebblestone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pebblestone.ui.theme.Amber
import com.example.pebblestone.ui.theme.WoodDark

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val controller = remember { PebbleController() }
    val state = controller.state

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF3A2718),
                    1f to WoodDark,
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Pebble Stone",
            color = Amber,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Jump a stone over its neighbour into an empty hole. Clear the board down to one.",
            color = Color(0xFFCBB79E),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
        )

        Spacer(Modifier.height(16.dp))

        StatusBar(remaining = state.remaining)

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BoardView(
                state = state,
                selected = controller.selected,
                destinations = controller.destinations,
                onTapCell = controller::onTapCell,
            )
            if (state.isGameOver) {
                GameOverBanner(win = state.isWin, remaining = state.remaining)
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = controller::undo,
                enabled = controller.canUndo,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Undo")
            }
            Button(
                onClick = controller::newGame,
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = WoodDark),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New Game", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusBar(remaining: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Stones left", color = Color(0xFFCBB79E), fontSize = 14.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            remaining.toString(),
            color = Amber,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GameOverBanner(win: Boolean, remaining: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xEE1E140A))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    win -> "Perfect!"
                    remaining <= 3 -> "So close!"
                    else -> "No moves left"
                },
                color = Amber,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (win) "You cleared the board." else "$remaining stones remain.",
                color = Color(0xFFCBB79E),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
