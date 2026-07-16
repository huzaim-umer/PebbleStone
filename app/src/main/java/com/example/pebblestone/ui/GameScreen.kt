package com.example.pebblestone.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.pebblestone.audio.SoundManager
import com.example.pebblestone.data.StatsRepository
import com.example.pebblestone.model.GameStats
import com.example.pebblestone.ui.theme.Amber
import com.example.pebblestone.ui.theme.WoodDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    adsRemoved: Boolean = false,
    removeAdsPrice: String? = null,
    onRemoveAds: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    onNewGameStarted: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { StatsRepository(context) }
    val controller = remember { PebbleController(repo) }
    val sound = remember { SoundManager(context) }
    val haptics = LocalHapticFeedback.current
    val state = controller.state

    var showStats by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(repo.loadSoundOn()) }
    sound.muted = !soundOn

    DisposableEffect(Unit) {
        onDispose { sound.release() }
    }

    // Haptic tick + move sound on each committed move (final move defers to the win/lose cue).
    LaunchedEffect(controller.moveCount) {
        if (controller.moveCount > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!controller.state.isGameOver) sound.playMove()
        }
    }
    // End-of-game cue.
    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver) {
            if (state.isWin) sound.playWin() else sound.playLose()
        }
    }

    // Move slide/fade animation progress (1f = idle).
    var animProgress by remember { mutableStateOf(1f) }
    LaunchedEffect(controller.lastAnim?.id) {
        if (controller.lastAnim != null) {
            animProgress = 0f
            animate(0f, 1f, animationSpec = tween(220, easing = FastOutSlowInEasing)) { v, _ ->
                animProgress = v
            }
        }
    }

    // Gentle looping pulse (0..1) for hint rings.
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseValue",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Color(0xFF3A2718), 1f to WoodDark))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title with a sound toggle pinned to the right.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Pebble Stone", color = Amber, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = {
                    soundOn = !soundOn
                    repo.saveSoundOn(soundOn)
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    if (soundOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (soundOn) "Mute" else "Unmute",
                    tint = Amber,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Mode toggle + stats.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                ModeChip("Free", controller.mode == GameMode.FREE) { controller.startFree() }
                Spacer(Modifier.width(8.dp))
                ModeChip("Daily", controller.mode == GameMode.DAILY) { controller.startDaily() }
            }
            IconButton(onClick = { showStats = true }) {
                Icon(Icons.Filled.Leaderboard, contentDescription = "Statistics", tint = Amber)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StatChip("Stones", state.remaining.toString())
            Spacer(Modifier.width(10.dp))
            StatChip("Streak", controller.lifetimeStats.currentStreak.toString())
        }

        if (controller.mode == GameMode.DAILY) {
            val dateLabel = LocalDate.ofEpochDay(controller.dailyEpochDay)
                .format(DateTimeFormatter.ofPattern("MMM d"))
            Text(
                text = if (controller.dailySolvedToday) "Daily · $dateLabel · solved ✓" else "Daily · $dateLabel",
                color = Color(0xFFCBB79E),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

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
                anim = controller.lastAnim,
                animProgress = animProgress,
                onTapCell = controller::onTapCell,
            )
            if (state.isGameOver) {
                GameOverBanner(
                    win = state.isWin,
                    remaining = state.remaining,
                    daily = controller.mode == GameMode.DAILY,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

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
                onClick = {
                    controller.restart()
                    onNewGameStarted()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = WoodDark),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (controller.mode == GameMode.DAILY) "Retry" else "New",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (!adsRemoved) {
            TextButton(onClick = onRemoveAds) {
                Text(
                    text = "Remove Ads" + (removeAdsPrice?.let { " · $it" } ?: ""),
                    color = Color(0xFFCBB79E),
                    fontSize = 13.sp,
                )
            }
        }
    }

    if (showStats) {
        StatsDialog(
            stats = controller.lifetimeStats,
            daily = controller.dailyStats,
            adsRemoved = adsRemoved,
            onRestorePurchases = onRestorePurchases,
            onDismiss = { showStats = false },
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Amber else Color(0x33000000)
    val fg = if (selected) WoodDark else Color(0xFFCBB79E)
    Text(
        text = label,
        color = fg,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

@Composable
private fun StatChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color(0xFFCBB79E), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(value, color = Amber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GameOverBanner(win: Boolean, remaining: Int, daily: Boolean) {
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
                    win && daily -> "Daily solved!"
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
private fun StatsDialog(
    stats: GameStats,
    daily: DailyState,
    adsRemoved: Boolean,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            if (!adsRemoved) TextButton(onClick = onRestorePurchases) { Text("Restore") }
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
                StatRow("Daily streak", daily.streak.toString())
                StatRow("Best daily streak", daily.bestStreak.toString())
                StatRow("Ads", if (adsRemoved) "Removed ✓" else "On")
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
