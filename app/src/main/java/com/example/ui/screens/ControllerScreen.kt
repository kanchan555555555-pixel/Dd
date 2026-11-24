package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.ArcadeGameState
import com.example.model.ConnectedClient
import com.example.model.GameType
import com.example.model.InputAction
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.VoltLime
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerScreen(
    strings: AppStrings,
    state: ArcadeGameState,
    playerSlot: Int,
    pingMs: Int,
    isHost: Boolean,
    connectedClients: List<ConnectedClient>,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onToggleHaptics: () -> Unit,
    onSelectGame: (GameType) -> Unit,
    onInput: (InputAction, Float, Boolean) -> Unit,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.gamepadTitle,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val badge = if (isHost) {
                            "${strings.playerLabel} 1 • TV: ${connectedClients.size}"
                        } else {
                            "${strings.playerLabel} $playerSlot • ${strings.pingLabel}: ${pingMs}ms"
                        }
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("controller_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backLabel,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleHaptics,
                        modifier = Modifier.testTag("toggle_haptics_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Vibration,
                            contentDescription = if (hapticsEnabled) strings.hapticsOn else strings.hapticsOff,
                            tint = if (hapticsEnabled) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = ElectricCyan,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Game Switcher Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val games = listOf(
                        GameType.PONG to strings.pongName,
                        GameType.SNAKE to strings.snakeName,
                        GameType.BRICKS to strings.bricksName,
                        GameType.TICTACTOE to strings.ticTacToeName,
                    )
                    games.forEach { (type, label) ->
                        val selected = state.activeGame == type
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectGame(type) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("switch_game_${type.id}"),
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = ObsidianBg,
                                containerColor = MidnightSurface,
                                labelColor = Color.White,
                            ),
                        )
                    }
                }

                // Live Mini-Scoreboard HUD
                LiveScoreHud(strings = strings, state = state)

                // Start / Pause & Reset Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onInput(InputAction.START_PAUSE, 0f, true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("controller_start_pause_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isPaused) VoltLime else SolarAmber,
                            contentColor = ObsidianBg,
                        ),
                    ) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.pauseResume,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    OutlinedButton(
                        onClick = { onInput(InputAction.RESET, 0f, true) },
                        modifier = Modifier
                            .weight(0.8f)
                            .height(48.dp)
                            .testTag("controller_reset_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = ArcadeMagenta,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.restartGame,
                            color = Color.White,
                        )
                    }
                }

                // Game-Specific Tactile Controller Zone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state.activeGame) {
                        GameType.PONG -> PongPadController(
                            strings = strings,
                            initialPos = if (playerSlot == 2) state.pong.p2Y else state.pong.p1Y,
                            onInput = onInput,
                        )
                        GameType.BRICKS -> BricksPadController(
                            strings = strings,
                            initialPos = state.bricks.paddleX,
                            onInput = onInput,
                        )
                        GameType.SNAKE -> SnakeDpadController(
                            strings = strings,
                            onInput = onInput,
                        )
                        GameType.TICTACTOE -> TicTacToeTapController(
                            strings = strings,
                            state = state,
                            onInput = onInput,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveScoreHud(strings: AppStrings, state: ArcadeGameState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state.activeGame) {
                GameType.PONG -> {
                    Text(
                        text = "${strings.pongName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan,
                    )
                    Text(
                        text = "${state.pong.score1}  :  ${state.pong.score2}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                }
                GameType.SNAKE -> {
                    Text(
                        text = strings.snakeName,
                        style = MaterialTheme.typography.titleMedium,
                        color = VoltLime,
                    )
                    Text(
                        text = "${strings.scoreLabel}: ${state.snake.score}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                GameType.BRICKS -> {
                    Text(
                        text = "${strings.levelLabel} ${state.bricks.level}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SolarAmber,
                    )
                    Text(
                        text = "${strings.scoreLabel}: ${state.bricks.score} • ${strings.livesLabel}: ${state.bricks.lives}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
                GameType.TICTACTOE -> {
                    Text(
                        text = "X: ${state.ticTacToe.xWins}   O: ${state.ticTacToe.oWins}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ArcadeMagenta,
                    )
                    val turnText = when (state.ticTacToe.winner) {
                        1 -> "${strings.youWinLabel} (X)"
                        2 -> "${strings.youWinLabel} (O)"
                        3 -> strings.drawLabel
                        else -> "${strings.playerLabel} ${if (state.ticTacToe.currentTurn == 1) "X" else "O"}"
                    }
                    Text(
                        text = turnText,
                        style = MaterialTheme.typography.titleMedium,
                        color = VoltLime,
                    )
                }
            }
        }
    }
}

@Composable
private fun PongPadController(
    strings: AppStrings,
    initialPos: Float,
    onInput: (InputAction, Float, Boolean) -> Unit,
) {
    var sliderY by remember { mutableFloatStateOf(initialPos) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = strings.touchSliderHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Vertical Smooth Touch Trackpad
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MidnightSurface)
                    .border(2.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val norm = (change.position.y / size.height.toFloat()).coerceIn(0.10f, 0.90f)
                            sliderY = norm
                            onInput(InputAction.PADDLE_POS, norm, true)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val norm = (offset.y / size.height.toFloat()).coerceIn(0.10f, 0.90f)
                            sliderY = norm
                            onInput(InputAction.PADDLE_POS, norm, true)
                        }
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                val trackH = constraints.maxHeight.toFloat()
                val knobPx = (trackH * 0.16f).coerceAtLeast(80f)
                val yOff = ((sliderY * trackH) - knobPx / 2f).coerceIn(12f, (trackH - knobPx - 12f).coerceAtLeast(12f))
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, yOff.roundToInt()) }
                        .fillMaxWidth(0.75f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ElectricCyan),
                )
            }

            // Hold Up / Down + A Action Button
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HoldDirectionButton(
                    icon = Icons.Filled.KeyboardArrowUp,
                    contentDesc = "Up",
                    accent = ElectricCyan,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("pong_up_button"),
                    onHoldChanged = { pressed -> onInput(InputAction.UP, 0f, pressed) },
                )
                Button(
                    onClick = { onInput(InputAction.ACTION_A, 0f, true) },
                    modifier = Modifier
                        .height(58.dp)
                        .fillMaxWidth()
                        .testTag("pong_action_a_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltLime,
                        contentColor = ObsidianBg,
                    ),
                ) {
                    Text(text = "A • START", fontWeight = FontWeight.Bold)
                }
                HoldDirectionButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDesc = "Down",
                    accent = ArcadeMagenta,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("pong_down_button"),
                    onHoldChanged = { pressed -> onInput(InputAction.DOWN, 0f, pressed) },
                )
            }
        }
    }
}

@Composable
private fun BricksPadController(
    strings: AppStrings,
    initialPos: Float,
    onInput: (InputAction, Float, Boolean) -> Unit,
) {
    var sliderX by remember { mutableFloatStateOf(initialPos) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = strings.touchSliderHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Horizontal Touch Trackpad
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MidnightSurface)
                .border(2.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val norm = (change.position.x / size.width.toFloat()).coerceIn(0.11f, 0.89f)
                        sliderX = norm
                        onInput(InputAction.PADDLE_POS, norm, true)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val norm = (offset.x / size.width.toFloat()).coerceIn(0.11f, 0.89f)
                        sliderX = norm
                        onInput(InputAction.PADDLE_POS, norm, true)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackW = constraints.maxWidth.toFloat()
            val knobW = (trackW * 0.22f).coerceAtLeast(72f)
            val xOff = ((sliderX * trackW) - knobW / 2f).coerceIn(12f, (trackW - knobW - 12f).coerceAtLeast(12f))
            Box(
                modifier = Modifier
                    .offset { IntOffset(xOff.roundToInt(), 0) }
                    .width(72.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ElectricCyan),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HoldDirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDesc = "Left",
                accent = ElectricCyan,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("bricks_left_button"),
                onHoldChanged = { pressed -> onInput(InputAction.LEFT, 0f, pressed) },
            )
            Button(
                onClick = { onInput(InputAction.ACTION_A, 0f, true) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("bricks_launch_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoltLime,
                    contentColor = ObsidianBg,
                ),
            ) {
                Text(text = "A • LAUNCH", fontWeight = FontWeight.Bold)
            }
            HoldDirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDesc = "Right",
                accent = ArcadeMagenta,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("bricks_right_button"),
                onHoldChanged = { pressed -> onInput(InputAction.RIGHT, 0f, pressed) },
            )
        }
    }
}

@Composable
private fun SnakeDpadController(
    strings: AppStrings,
    onInput: (InputAction, Float, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDx = 0f
                var totalDy = 0f
                detectDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        totalDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y
                        if (abs(totalDx) > 48f || abs(totalDy) > 48f) {
                            if (abs(totalDx) > abs(totalDy)) {
                                onInput(if (totalDx > 0) InputAction.RIGHT else InputAction.LEFT, 0f, true)
                            } else {
                                onInput(if (totalDy > 0) InputAction.DOWN else InputAction.UP, 0f, true)
                            }
                            totalDx = 0f
                            totalDy = 0f
                        }
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.dpadHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DpadTapButton(
                icon = Icons.Filled.KeyboardArrowUp,
                desc = "Up",
                accent = ElectricCyan,
                modifier = Modifier.testTag("snake_up_button"),
                onTap = { onInput(InputAction.UP, 0f, true) },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DpadTapButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    desc = "Left",
                    accent = ElectricCyan,
                    modifier = Modifier.testTag("snake_left_button"),
                    onTap = { onInput(InputAction.LEFT, 0f, true) },
                )
                HoldDirectionButton(
                    icon = Icons.Filled.PlayArrow,
                    contentDesc = "Boost",
                    accent = VoltLime,
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("snake_boost_button"),
                    onHoldChanged = { pressed -> onInput(InputAction.ACTION_A, 0f, pressed) },
                )
                DpadTapButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    desc = "Right",
                    accent = ElectricCyan,
                    modifier = Modifier.testTag("snake_right_button"),
                    onTap = { onInput(InputAction.RIGHT, 0f, true) },
                )
            }
            DpadTapButton(
                icon = Icons.Filled.KeyboardArrowDown,
                desc = "Down",
                accent = ElectricCyan,
                modifier = Modifier.testTag("snake_down_button"),
                onTap = { onInput(InputAction.DOWN, 0f, true) },
            )
        }
    }
}

@Composable
private fun TicTacToeTapController(
    strings: AppStrings,
    state: ArcadeGameState,
    onInput: (InputAction, Float, Boolean) -> Unit,
) {
    val t = state.ticTacToe
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.tapCellHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (r in 0..2) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (c in 0..2) {
                        val idx = r * 3 + c
                        val cellVal = t.cells[idx]
                        val isWinCell = t.winLine.contains(idx)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onInput(InputAction.CELL_TAP, idx.toFloat(), true) }
                                .testTag("ttt_cell_$idx"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isWinCell) VoltLime.copy(alpha = 0.2f) else MidnightSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isWinCell) VoltLime else ElevatedBorder,
                            ),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (cellVal) {
                                        1 -> "X"
                                        2 -> "O"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (cellVal == 1) ElectricCyan else ArcadeMagenta,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DpadTapButton(
    icon: ImageVector,
    desc: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(20.dp),
        color = ElevatedCard,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.65f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = accent,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun HoldDirectionButton(
    icon: ImageVector,
    contentDesc: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onHoldChanged: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> onHoldChanged(true)
                is PressInteraction.Release, is PressInteraction.Cancel -> onHoldChanged(false)
            }
        }
    }

    Button(
        onClick = {},
        interactionSource = interactionSource,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ElevatedCard,
            contentColor = accent,
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.65f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            modifier = Modifier.size(36.dp),
        )
    }
}
