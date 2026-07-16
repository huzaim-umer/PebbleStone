package com.example.pebblestone.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pebblestone.data.StatsRepository
import com.example.pebblestone.model.GameStats
import com.example.pebblestone.ui.theme.Amber
import com.example.pebblestone.ui.theme.WoodDark

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val controller = remember { PebbleController(StatsRepository(context)) }
    val haptics = LocalHapticFeedback.current
    val state = controller.state

    var showStats by remember { mutableStateOf(false) }

    // Fire a light haptic tick whenever a move is committed.
    LaunchedEffect(controller.moveCount) {
        if (controller.moveCount > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Gentle looping pulse (0..1) used to animate hint rings.
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseValue",
    )

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            StatusBar(remaining = state.remaining)
            Spacer(Modifier.width(10.dp))
            StreakChip(streak = controller.lifetimeStats.currentStreak)
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = { showStats = true }) {
                Icon(Icons.Filled.Leaderboard, contentDescription = "Statistics", tint = Amber)
            }
        }

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
                hint = controller.hint,
                pulse = pulse,
                onTapCell = controller::onTapCell,
            )
            if (state.isGameOver) {
                GameOverBanner(win = state.isWin, remaining = state.remaining)
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = controller::showHint,
                enabled = !state.isGameOver,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Hint")
            }
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
                Text("New", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showStats) {
        StatsDialog(stats = controller.lifetimeStats, onDismiss = { showStats = false })
    }
}

@Composable
private fun StatusBar(remaining: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Stones", color = Color(0xFFCBB79E), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            remaining.toString(),
            color = Amber,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StreakChip(streak: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Streak", color = Color(0xFFCBB79E), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            streak.toString(),
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

@Composable
private fun StatsDialog(stats: GameStats, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Your stats") },
        text = {
            Column {
                StatRow("Games played", stats.gamesPlayed.toString())
                StatRow("Games won", stats.gamesWon.toString())
                StatRow("Win rate", "${stats.winRatePercent}%")
                StatRow("Best result", if (stats.hasHistory) "${stats.bestRemaining} left" else "—")
                StatRow("Current streak", stats.currentStreak.toString())
                StatRow("Best streak", stats.bestStreak.toString())
            }
        },
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
