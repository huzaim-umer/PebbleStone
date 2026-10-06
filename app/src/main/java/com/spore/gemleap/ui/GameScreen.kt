package com.spore.gemleap.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spore.gemleap.audio.SoundManager
import com.spore.gemleap.data.StatsRepository
import com.spore.gemleap.model.*
import com.spore.gemleap.ui.theme.PebbleStoneTheme
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
    onWatchHintAd: (onGranted: () -> Unit) -> Unit = { it() },
    hintAdReady: Boolean = false,
    hintAdLoading: Boolean = false,
) {
    val context = LocalContext.current
    val repo = remember { StatsRepository(context) }
    val controller = remember { PebbleController(repo) }
    val sound = remember { SoundManager(context) }
    val haptics = LocalHapticFeedback.current
    var showStats by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(repo.loadSoundOn()) }
    var themeId by remember { mutableStateOf(repo.loadThemeId()) }
    val theme = GameThemes.of(themeId)
    SideEffect { sound.muted = !soundOn }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    LaunchedEffect(controller.moveCount) {
        if (controller.moveCount > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!controller.state.isGameOver) sound.playMove()
        }
    }
    LaunchedEffect(controller.state.isGameOver) {
        if (controller.state.isGameOver) {
            if (controller.state.isWin) sound.playWin() else sound.playLose()
        }
    }
    var progress by remember { mutableStateOf(1f) }
    LaunchedEffect(controller.lastAnim?.id) {
        if (controller.lastAnim != null) {
            progress = 0f
            animate(0f, 1f, animationSpec = tween(220, easing = FastOutSlowInEasing)) { v, _ -> progress = v }
        } else progress = 1f
    }
    // No persistent idle animation. Compose's animation clock honors system scale.
    var pulse by remember { mutableStateOf(0f) }
    LaunchedEffect(controller.hint) {
        pulse = 0f
        if (controller.hint != null) repeat(3) {
            animate(0f, 1f, animationSpec = tween(700)) { v, _ -> pulse = v }
            animate(1f, 0f, animationSpec = tween(700)) { v, _ -> pulse = v }
        }
    }
    var winHalo by remember { mutableStateOf(0f) }
    LaunchedEffect(controller.state.isWin) {
        if (controller.state.isWin) animate(1f, 0f, animationSpec = tween(650)) { v, _ -> winHalo = v }
        else winHalo = 0f
    }
    val busy = controller.lastAnim != null && progress < 1f
    PebbleStoneTheme(theme = theme) {
        GameSurface(
            modifier = modifier, state = controller.state, theme = theme, mode = controller.mode,
            streak = if (controller.mode == GameMode.DAILY) controller.dailyStats.streak else controller.lifetimeStats.currentStreak,
            date = if (controller.mode == GameMode.DAILY) LocalDate.ofEpochDay(controller.dailyEpochDay) else LocalDate.now(),
            dailySolved = controller.dailySolvedToday,
            selected = controller.selected, destinations = controller.destinations, hint = controller.hint,
            anim = controller.lastAnim, progress = progress, pulse = pulse, winHalo = winHalo,
            canUndo = controller.canUndo, soundOn = soundOn, adsRemoved = adsRemoved,
            price = removeAdsPrice, busy = busy,
            onCell = controller::onTapCell,
            onMode = { if (it != controller.mode) { if (it == GameMode.FREE) controller.startFree() else controller.startDaily() } },
            onSettings = { showSettings = true }, onStats = { showStats = true },
            onSound = { soundOn = !soundOn; repo.saveSoundOn(soundOn) },
            onHint = { if (adsRemoved) controller.showHint() else showHint = true },
            onUndo = controller::undo,
            onNew = { controller.restart(); onNewGameStarted() },
            onPurchase = onRemoveAds,
        )
        if (showSettings) SettingsDialog(theme, soundOn,
            onSelect = { themeId = it; repo.saveThemeId(it) },
            onSound = { soundOn = !soundOn; repo.saveSoundOn(soundOn) },
            onDismiss = { showSettings = false })
        if (showStats) StatsDialog(controller.lifetimeStats, controller.dailyStats, adsRemoved,
            onRestorePurchases, onDismiss = { showStats = false })
        if (showHint) HintDialog(hintAdReady, hintAdLoading, onDismiss = { showHint = false }, onConfirm = {
            showHint = false
            // The existing manager grants a free hint when an ad is unavailable.
            onWatchHintAd { controller.showHint() }
        })
    }
}

/** Stateless presentation also used by Android Studio previews. */
@Composable
internal fun GameSurface(
    state: GameState, theme: GameTheme, mode: GameMode = GameMode.FREE, streak: Int = 0,
    date: LocalDate = LocalDate.now(), dailySolved: Boolean = false,
    selected: Pos? = null, destinations: Set<Pos> = emptySet(), hint: Move? = null,
    anim: MoveAnim? = null, progress: Float = 1f, pulse: Float = 0f, winHalo: Float = 0f,
    canUndo: Boolean = false, soundOn: Boolean = true, adsRemoved: Boolean = false,
    price: String? = null, busy: Boolean = false, modifier: Modifier = Modifier,
    onCell: (Pos) -> Unit = {}, onMode: (GameMode) -> Unit = {},
    onSettings: () -> Unit = {}, onStats: () -> Unit = {}, onSound: () -> Unit = {},
    onHint: () -> Unit = {}, onUndo: () -> Unit = {}, onNew: () -> Unit = {}, onPurchase: () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))) {
        val viewportHeight = maxHeight
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, "Settings", tint = theme.accent)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GemLeap", color = MaterialTheme.colorScheme.onSurface, fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text("ONE STONE. A LITTLE WONDER.", color = theme.secondaryText, fontSize = 9.sp,
                        letterSpacing = 1.sp, textAlign = TextAlign.Center)
                }
                IconButton(onClick = onSound) {
                    Icon(if (soundOn) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                        if (soundOn) "Mute sound" else "Enable sound", tint = theme.accent)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ModeTab("Free", mode == GameMode.FREE, !busy, Modifier.weight(1f)) { onMode(GameMode.FREE) }
                ModeTab("Daily", mode == GameMode.DAILY, !busy, Modifier.weight(1f)) { onMode(GameMode.DAILY) }
                IconButton(onClick = onStats) { Icon(Icons.Outlined.BarChart, "Statistics", tint = theme.accent) }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("STONES LEFT", state.remaining.toString())
                Metric(if (mode == GameMode.DAILY) "DAILY STREAK" else "WIN STREAK", streak.toString(), end = true)
            }
            Text(
                if (mode == GameMode.DAILY) date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) + if (dailySolved) " · Solved ✓" else ""
                else "A quiet moment. A clever move.",
                color = theme.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            BoardView(state, theme, selected, destinations, hint, pulse, anim, progress, onCell,
                Modifier.widthIn(max = 520.dp).fillMaxWidth(), winHalo = winHalo)
            if (state.isGameOver && !busy) {
                ResultPanel(state.isWin, state.remaining, mode == GameMode.DAILY)
            } else {
                Text(when {
                    hint != null -> "Follow the marked stone to its destination"
                    selected != null -> "Tap a marked socket to leap"
                    else -> "Jump a stone. Leave just one."
                }, color = theme.secondaryText, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Hint", Icons.Outlined.Lightbulb, !state.isGameOver && !busy, false, Modifier.weight(1f), onHint)
                ActionButton("Undo", Icons.AutoMirrored.Outlined.Undo, canUndo && !busy, false, Modifier.weight(1f), onUndo)
                ActionButton(if (mode == GameMode.DAILY || state.isGameOver && !state.isWin) "Retry" else "New",
                    Icons.Outlined.Refresh, !busy, true, Modifier.weight(1f), onNew)
            }
            if (!adsRemoved) {
                TextButton(onClick = onPurchase, enabled = price != null, modifier = Modifier.padding(top = 4.dp)) {
                    Text(if (price != null) "Remove ads · $price" else "Remove ads · Price unavailable", fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            } else Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clip(RoundedCornerShape(16.dp)).selectable(selected, enabled = enabled, role = Role.Tab, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Box(Modifier.heightIn(min = 48.dp).padding(12.dp), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, end: Boolean = false) {
    Column(horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 30.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, enabled: Boolean, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = if (primary) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
    Button(onClick, modifier.heightIn(min = 68.dp), enabled = enabled, shape = RoundedCornerShape(18.dp), colors = colors,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        border = if (primary || !enabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun ResultPanel(win: Boolean, remaining: Int, daily: Boolean) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (win) "Beautifully solved" else if (remaining <= 3) "So close" else "No moves remaining",
                color = MaterialTheme.colorScheme.primary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(if (win) if (daily) "One stone remains. Daily complete." else "One stone remains. A perfect finish."
                else "$remaining stones remain. Undo a move or try again.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
internal fun SettingsDialog(theme: GameTheme, soundOn: Boolean, onSelect: (ThemeId) -> Unit, onSound: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Make it yours") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose your board", color = MaterialTheme.colorScheme.onSurfaceVariant)
                GameThemes.all.forEach { choice ->
                    Surface(shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (choice.id == theme.id) theme.accent else theme.secondaryText.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().selectable(choice.id == theme.id, role = Role.RadioButton, onClick = { onSelect(choice.id) })) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            BoardView(GameState.newGame(), choice, null, emptySet(), null, 0f, null, 1f, {}, Modifier.size(64.dp), interactive = false)
                            Text(choice.label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.Medium)
                            if (choice.id == theme.id) Icon(Icons.Outlined.Check, "Selected", tint = theme.accent)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sound")
                    Switch(checked = soundOn, onCheckedChange = { onSound() })
                }
            }
        })
}

@Composable
internal fun StatsDialog(stats: GameStats, daily: DailyState, adsRemoved: Boolean, onRestore: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Your quiet progress") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { if (!adsRemoved) TextButton(onClick = onRestore) { Text("Restore purchases") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!stats.hasHistory) Text("Your first finish starts the story. Play a board to see your progress here.")
                Metric("WIN RATE", if (stats.hasHistory) "${stats.winRatePercent}%" else "—")
                StatRow("Games won", stats.gamesWon.toString())
                StatRow("Games played", stats.gamesPlayed.toString())
                StatRow("Best result", if (stats.hasHistory) "${stats.bestRemaining} left" else "—")
                HorizontalDivider()
                StatRow("Current streak", stats.currentStreak.toString())
                StatRow("Best streak", stats.bestStreak.toString())
                StatRow("Daily streak", daily.streak.toString())
                StatRow("Best daily streak", daily.bestStreak.toString())
                StatRow("Ads", if (adsRemoved) "Removed" else "On")
            }
        })
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun HintDialog(ready: Boolean, loading: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Outlined.Lightbulb, null) },
        title = { Text("A little nudge") },
        text = { Text(when {
            ready -> "Watch a short ad to reveal one legal move."
            loading -> "An ad is still loading. You can get a free hint now."
            else -> "No ad is available. This hint is on us."
        }) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(if (ready) "Watch ad for hint" else "Get free hint") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep thinking") } })
}
