package com.spore.gemleap.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spore.gemleap.audio.SoundManager
import com.spore.gemleap.data.StatsRepository
import com.spore.gemleap.model.BoardStyle
import com.spore.gemleap.model.GameStats
import com.spore.gemleap.model.GameTheme
import com.spore.gemleap.model.GameThemes
import com.spore.gemleap.model.ThemeId
import com.spore.gemleap.social.PlayerRank
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    adsRemoved: Boolean = false,
    removeAdsPrice: String? = null,
    onRemoveAds: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    onNewGameStarted: () -> Unit = {},
    onWatchHintAd: (onGranted: () -> Unit) -> Unit = { it() },
    leaderboardAvailable: Boolean = false,
    leaderboardSignedIn: Boolean = false,
    leaderboardRank: PlayerRank? = null,
    rankLoading: Boolean = false,
    onShowLeaderboard: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onStatsOpened: () -> Unit = {},
    onWinsChanged: (totalWins: Int) -> Unit = {},
    onThemeChanged: (GameTheme) -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { StatsRepository(context) }
    val latestOnWinsChanged by rememberUpdatedState(onWinsChanged)
    val controller = remember { PebbleController(repo) { latestOnWinsChanged(it) } }
    val sound = remember { SoundManager(context) }
    val haptics = LocalHapticFeedback.current
    val state = controller.state

    var showStats by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHintPrompt by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(repo.loadSoundOn()) }
    var themeId by remember { mutableStateOf(repo.loadThemeId()) }
    val theme = GameThemes.of(themeId)
    val icons = rememberGemIcons(theme)
    sound.muted = !soundOn

    DisposableEffect(Unit) {
        onDispose { sound.release() }
    }
    LaunchedEffect(theme.id) { onThemeChanged(theme) }

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

    val toggleSound = {
        soundOn = !soundOn
        repo.saveSoundOn(soundOn)
    }
    val daily = controller.mode == GameMode.DAILY

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
            .drawBehind { drawBackdrop(theme) },
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: theme picker, title, sound.
            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                IconButton(onClick = { showSettings = true }, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(icons.themeToggle, "Themes and settings", tint = Color.Unspecified, modifier = Modifier.size(30.dp))
                }
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    GemLeapTitle(theme)
                    Text(
                        "ONE STONE. A LITTLE WONDER.",
                        color = theme.labelText,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(onClick = toggleSound, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        if (soundOn) icons.soundOn else icons.soundOff,
                        contentDescription = if (soundOn) "Mute" else "Unmute",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            // Mode switch, leaderboard trophy, stats.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ModeSwitch(
                    daily = daily,
                    theme = theme,
                    onFree = {
                        controller.startFree()
                        onNewGameStarted()
                    },
                    onDaily = {
                        controller.startDaily()
                        onNewGameStarted()
                    },
                    modifier = Modifier.weight(1f),
                )
                if (leaderboardAvailable) {
                    SquareIconButton(icons.trophy, "Leaderboard", theme, onShowLeaderboard)
                }
                SquareIconButton(icons.stats, "Statistics", theme) {
                    showStats = true
                    onStatsOpened()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                StatBlock("STONES LEFT", state.remaining.toString(), theme, Alignment.Start)
                StatBlock("WIN STREAK", controller.lifetimeStats.currentStreak.toString(), theme, Alignment.End)
            }

            val tagline = if (daily) {
                val dateLabel = LocalDate.ofEpochDay(controller.dailyEpochDay)
                    .format(DateTimeFormatter.ofPattern("MMM d"))
                if (controller.dailySolvedToday) "Daily · $dateLabel · solved ✓" else "Daily challenge · $dateLabel"
            } else {
                "A quiet moment. A clever move."
            }
            Text(
                text = tagline,
                color = theme.secondaryText,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 6.dp),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BoardView(
                    state = state,
                    theme = theme,
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
                        daily = daily,
                        theme = theme,
                    )
                }
            }

            Text(
                "Jump a stone. Leave just one.",
                color = theme.secondaryText,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionTile(icons.hint, "Hint", theme, enabled = !state.isGameOver) {
                    // Paying users get free hints; others watch a rewarded ad.
                    if (adsRemoved) controller.showHint() else showHintPrompt = true
                }
                ActionTile(icons.undo, "Undo", theme, enabled = controller.canUndo, onClick = controller::undo)
                ActionTile(icons.newGame, if (daily) "Retry" else "New", theme, highlighted = true) {
                    controller.restart()
                    onNewGameStarted()
                }
            }

            if (adsRemoved) {
                Spacer(Modifier.height(12.dp))
            } else {
                TextButton(onClick = onRemoveAds) {
                    Text(
                        text = "Remove ads" + (removeAdsPrice?.let { " · $it" } ?: ""),
                        color = theme.mutedText,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }

    if (showHintPrompt) {
        HintDialog(
            theme = theme,
            icons = icons,
            onWatch = {
                showHintPrompt = false
                onWatchHintAd { controller.showHint() }
            },
            onDismiss = { showHintPrompt = false },
        )
    }

    if (showStats) {
        StatsDialog(
            theme = theme,
            icons = icons,
            stats = controller.lifetimeStats,
            daily = controller.dailyStats,
            adsRemoved = adsRemoved,
            leaderboardAvailable = leaderboardAvailable,
            leaderboardSignedIn = leaderboardSignedIn,
            rank = leaderboardRank,
            rankLoading = rankLoading,
            onShowLeaderboard = onShowLeaderboard,
            onSignIn = onSignIn,
            onRestorePurchases = onRestorePurchases,
            onDismiss = { showStats = false },
        )
    }

    if (showSettings) {
        SettingsDialog(
            theme = theme,
            selected = themeId,
            soundOn = soundOn,
            onSelectTheme = {
                themeId = it
                repo.saveThemeId(it)
            },
            onToggleSound = toggleSound,
            onDismiss = { showSettings = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Main screen pieces
// ---------------------------------------------------------------------------

/** "Gem" in the primary text colour, "Leap" in the theme's accent gradient. */
@Composable
private fun GemLeapTitle(theme: GameTheme) {
    Row {
        Text(
            "Gem",
            color = theme.textPrimary,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        )
        Text(
            "Leap",
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(theme.accent, theme.accent2)),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            ),
        )
    }
}

/** Free / Daily segmented switch; the active side is a gradient pill. */
@Composable
private fun ModeSwitch(
    daily: Boolean,
    theme: GameTheme,
    onFree: () -> Unit,
    onDaily: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .background(theme.panel)
            .border(1.dp, theme.panelBorder, shape)
            .padding(4.dp),
    ) {
        ModeSegment("Free", !daily, theme, onFree)
        ModeSegment("Daily", daily, theme, onDaily)
    }
}

@Composable
private fun RowScope.ModeSegment(label: String, selected: Boolean, theme: GameTheme, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(theme.pillStart, theme.pillEnd)) else SolidColor(Color.Transparent),
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(theme.pillText))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = if (selected) theme.pillText else theme.secondaryText,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SquareIconButton(icon: ImageVector, description: String, theme: GameTheme, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(shape)
            .background(theme.panel)
            .border(1.dp, theme.panelBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun StatBlock(label: String, value: String, theme: GameTheme, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(label, color = theme.labelText, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = theme.textPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}

/** Square-ish tile with the icon above its label; [highlighted] marks the primary action. */
@Composable
private fun RowScope.ActionTile(
    icon: ImageVector,
    label: String,
    theme: GameTheme,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(82.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(
                if (highlighted) {
                    Brush.verticalGradient(listOf(theme.pillEnd.copy(alpha = 0.30f), theme.panel))
                } else {
                    SolidColor(theme.panel)
                },
            )
            .border(
                width = if (highlighted) 1.5.dp else 1.dp,
                brush = if (highlighted) Brush.linearGradient(listOf(theme.pillStart, theme.pillEnd)) else SolidColor(theme.panelBorder),
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = theme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GameOverBanner(win: Boolean, remaining: Int, daily: Boolean, theme: GameTheme) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(theme.bannerBg)
            .border(1.dp, Brush.linearGradient(listOf(theme.dialogBorderStart, theme.dialogBorderEnd)), shape)
            .padding(horizontal = 30.dp, vertical = 22.dp),
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
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(theme.accent, theme.accent2)),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Text(
                text = if (win) "You cleared the board." else "$remaining stones remain.",
                color = theme.secondaryText,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Theme-specific background detail: a faint starfield (jewel) or raked rings (zen). */
private fun DrawScope.drawBackdrop(theme: GameTheme) {
    when (theme.boardStyle) {
        BoardStyle.NEON -> {
            val rnd = Random(7)
            repeat(42) {
                val p = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
                val color = if (rnd.nextBoolean()) Color.White else theme.accent
                drawCircle(color.copy(alpha = 0.15f + rnd.nextFloat() * 0.45f), radius = 0.6f + rnd.nextFloat() * 1.8f, center = p)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(theme.accent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.55f),
                    radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width / 2f, size.height * 0.55f),
            )
        }
        BoardStyle.SLATE -> {
            val c = Offset(size.width / 2f, size.height * 0.55f)
            var r = size.width * 0.18f
            while (r < size.height) {
                drawCircle(theme.labelText.copy(alpha = 0.07f), radius = r, center = c, style = Stroke(width = 1.5f))
                r += size.width * 0.16f
            }
        }
        BoardStyle.WOOD -> drawCircle(
            brush = Brush.radialGradient(
                listOf(theme.accent.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width / 2f, size.height * 0.5f),
                radius = size.width * 0.9f,
            ),
            radius = size.width * 0.9f,
            center = Offset(size.width / 2f, size.height * 0.5f),
        )
    }
}

// ---------------------------------------------------------------------------
// Dialogs (styled after the Stitch "cyber violet" modals, recoloured per theme)
// ---------------------------------------------------------------------------

@Composable
private fun GemDialog(theme: GameTheme, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(32.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .shadow(24.dp, shape, ambientColor = theme.dialogBorderStart, spotColor = theme.dialogBorderStart)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(theme.dialogTop, theme.dialogBottom)))
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(theme.dialogBorderStart, theme.dialogBorderStart.copy(alpha = 0.35f), theme.dialogBorderEnd),
                    ),
                    shape,
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 26.dp),
            content = content,
        )
    }
}

@Composable
private fun CtaButton(label: String, theme: GameTheme, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(14.dp, shape, ambientColor = theme.ctaMid, spotColor = theme.ctaMid)
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.ctaStart, theme.ctaMid, theme.ctaEnd)))
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = theme.ctaText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DialogLink(text: String, color: Color, modifier: Modifier = Modifier, bold: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        color = color,
        fontSize = if (bold) 18.sp else 16.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** "A little nudge": offer a rewarded ad in exchange for one legal move. */
@Composable
private fun HintDialog(theme: GameTheme, icons: GemIcons, onWatch: () -> Unit, onDismiss: () -> Unit) {
    GemDialog(theme, onDismiss) {
        Text(
            "A little nudge",
            color = theme.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Box(modifier = Modifier.size(104.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(104.dp)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(theme.accent.copy(alpha = 0.35f), theme.accent.copy(alpha = 0.10f), Color.Transparent),
                    ),
                )
                drawCircle(
                    color = theme.accent.copy(alpha = 0.6f),
                    radius = size.minDimension / 2f * 0.82f,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            Icon(icons.hint, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Watch a short ad to reveal\none legal move.",
            color = theme.secondaryText,
            fontSize = 17.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(26.dp))
        CtaButton("Watch ad for hint", theme, onWatch)
        Spacer(Modifier.height(12.dp))
        DialogLink("Keep thinking", theme.linkSoft, Modifier.align(Alignment.CenterHorizontally), bold = false, onClick = onDismiss)
    }
}

/** "Make it yours": board theme picker plus the sound switch. */
@Composable
private fun SettingsDialog(
    theme: GameTheme,
    selected: ThemeId,
    soundOn: Boolean,
    onSelectTheme: (ThemeId) -> Unit,
    onToggleSound: () -> Unit,
    onDismiss: () -> Unit,
) {
    GemDialog(theme, onDismiss) {
        Text("Make it yours", color = theme.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Choose your board",
            color = theme.mutedText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(18.dp))
        GameThemes.all.forEach { t ->
            ThemeRow(t, selected = t.id == selected, dialogTheme = theme) { onSelectTheme(t.id) }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sound", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            GemSwitch(soundOn, theme, onToggleSound)
        }
        Spacer(Modifier.height(18.dp))
        DialogLink("Done", theme.link, Modifier.align(Alignment.End), onClick = onDismiss)
    }
}

@Composable
private fun ThemeRow(t: GameTheme, selected: Boolean, dialogTheme: GameTheme, onClick: () -> Unit) {
    val d = dialogTheme
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(d.rowSelectedBg, d.rowBg)) else SolidColor(d.rowBg),
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                brush = if (selected) {
                    Brush.horizontalGradient(listOf(d.dialogBorderStart, d.accent, d.dialogBorderEnd))
                } else {
                    SolidColor(d.rowBorder)
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeThumb(t, Modifier.size(52.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            t.label,
            color = if (selected) d.textPrimary else d.secondaryText,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = d.link, modifier = Modifier.size(26.dp))
        }
    }
}

/** Mini board badge: the theme's rim and surface with five of its gems in a plus. */
@Composable
private fun ThemeThumb(t: GameTheme, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        val rim = if (t.boardStyle == BoardStyle.NEON) t.surfaceOuter else t.discOuter
        drawCircle(rim, radius = r, center = c)
        drawCircle(t.surfaceInner, radius = r * 0.84f, center = c)
        if (t.boardStyle == BoardStyle.NEON) {
            drawCircle(t.holeRim, radius = r * 0.9f, center = c, style = Stroke(width = r * 0.06f))
        }
        val step = r * 0.36f
        val dot = r * 0.15f
        val spots = listOf(Offset(0f, -step), Offset(-step, 0f), Offset(0f, 0f), Offset(step, 0f), Offset(0f, step))
        spots.forEachIndexed { i, o ->
            val gem = t.gems[(i * 2 + 1) % t.gems.size].base
            drawCircle(gem, radius = dot, center = c + o)
            drawCircle(Color.White.copy(alpha = 0.5f), radius = dot * 0.35f, center = c + o - Offset(dot * 0.3f, dot * 0.3f))
        }
    }
}

/** Pill switch: gradient track with a white thumb when on, muted when off. */
@Composable
private fun GemSwitch(on: Boolean, theme: GameTheme, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val thumbX by animateDpAsState(if (on) 26.dp else 0.dp, label = "switchThumb")
    Box(
        modifier = Modifier
            .width(60.dp)
            .height(34.dp)
            .clip(shape)
            .background(if (on) Brush.horizontalGradient(listOf(theme.ctaStart, theme.accent2)) else SolidColor(theme.rowBg))
            .border(1.5.dp, if (on) theme.dialogBorderStart else theme.rowBorder, shape)
            .clickable(onClick = onToggle)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = thumbX)
                .size(26.dp)
                .shadow(if (on) 6.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(if (on) Color.White else theme.mutedText),
        )
    }
}

@Composable
private fun StatsDialog(
    theme: GameTheme,
    icons: GemIcons,
    stats: GameStats,
    daily: DailyState,
    adsRemoved: Boolean,
    leaderboardAvailable: Boolean,
    leaderboardSignedIn: Boolean,
    rank: PlayerRank?,
    rankLoading: Boolean,
    onShowLeaderboard: () -> Unit,
    onSignIn: () -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit,
) {
    GemDialog(theme, onDismiss) {
        Text("Your stats", color = theme.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        StatRow("Games played", stats.gamesPlayed.toString(), theme)
        StatRow("Games won", stats.gamesWon.toString(), theme)
        StatRow("Win rate", "${stats.winRatePercent}%", theme)
        StatRow("Best result", if (stats.hasHistory) "${stats.bestRemaining} left" else "—", theme)
        StatRow("Current streak", stats.currentStreak.toString(), theme)
        StatRow("Best streak", stats.bestStreak.toString(), theme)
        StatRow("Daily streak", daily.streak.toString(), theme)
        StatRow("Best daily streak", daily.bestStreak.toString(), theme)
        StatRow("Ads", if (adsRemoved) "Removed ✓" else "On", theme)

        if (leaderboardAvailable) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(theme.rowBorder))
            Spacer(Modifier.height(10.dp))
            if (leaderboardSignedIn) {
                StatRow("Your rank", rankText(rank, rankLoading), theme, valueColor = theme.link)
                Spacer(Modifier.height(12.dp))
                LeaderboardButton(icons, theme, onShowLeaderboard)
            } else {
                GoogleSignInButton(onSignIn)
                Text(
                    "Sign in to join the global Total Wins leaderboard.",
                    color = theme.mutedText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!adsRemoved) {
                DialogLink("Restore purchases", theme.linkSoft, bold = false, onClick = onRestorePurchases)
            }
            Spacer(Modifier.weight(1f))
            DialogLink("Close", theme.link, onClick = onDismiss)
        }
    }
}

private fun rankText(rank: PlayerRank?, loading: Boolean): String = when {
    rank == null -> if (loading) "Loading…" else "—"
    rank.rank != null && rank.total != null -> "#%,d of %,d".format(rank.rank, rank.total)
    rank.rank != null -> "#%,d".format(rank.rank)
    else -> "Not ranked yet"
}

@Composable
private fun StatRow(label: String, value: String, theme: GameTheme, valueColor: Color = theme.textPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = theme.secondaryText, fontSize = 16.sp)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LeaderboardButton(icons: GemIcons, theme: GameTheme, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(theme.rowBg)
            .border(1.5.dp, Brush.horizontalGradient(listOf(theme.dialogBorderStart, theme.dialogBorderEnd)), shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icons.trophy, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text("Global leaderboard", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Google's standard light "Sign in with Google" button: white pill, grey outline, G mark. */
@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFF747775), shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(GoogleG, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text("Sign in with Google", color = Color(0xFF1F1F1F), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
