package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.ArcadeGameState
import com.example.model.GameType
import com.example.model.InputAction
import com.example.ui.components.ArcadeGameBoardCanvas
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.VoltLime
import kotlin.math.abs

@Composable
fun TvDisplayScreen(
    strings: AppStrings,
    state: ArcadeGameState,
    isRemoteTvMode: Boolean,
    isConnected: Boolean,
    pingMs: Int,
    onBack: () -> Unit,
    onInput: (InputAction, Float, Boolean) -> Unit,
) {
    BackHandler(onBack = onBack)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    val pressed = event.type == KeyEventType.KeyDown
                    val released = event.type == KeyEventType.KeyUp
                    if (!pressed && !released) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { onInput(InputAction.UP, 0f, pressed); true }
                        Key.DirectionDown -> { onInput(InputAction.DOWN, 0f, pressed); true }
                        Key.DirectionLeft -> { onInput(InputAction.LEFT, 0f, pressed); true }
                        Key.DirectionRight -> { onInput(InputAction.RIGHT, 0f, pressed); true }
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (pressed) onInput(InputAction.ACTION_A, 0f, true)
                            true
                        }
                        else -> false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top TV HUD Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MidnightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("tv_display_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.backLabel,
                                tint = ElectricCyan,
                            )
                        }
                        if (isRemoteTvMode) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) VoltLime else CoralAlert)
                            )
                            Text(
                                text = if (isConnected) "${strings.pingLabel}: ${pingMs}ms" else strings.connecting,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isConnected) VoltLime else SolarAmber,
                            )
                        } else {
                            Text(
                                text = when (state.activeGame) {
                                    GameType.PONG -> strings.pongName
                                    GameType.SNAKE -> strings.snakeName
                                    GameType.BRICKS -> strings.bricksName
                                    GameType.TICTACTOE -> strings.ticTacToeName
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan,
                            )
                        }
                    }

                    // Live Score
                    val scoreBadge = when (state.activeGame) {
                        GameType.PONG -> "${state.pong.score1} : ${state.pong.score2}"
                        GameType.SNAKE -> "${strings.scoreLabel} ${state.snake.score}"
                        GameType.BRICKS -> "${strings.scoreLabel} ${state.bricks.score} • ♥ ${state.bricks.lives}"
                        GameType.TICTACTOE -> "X ${state.ticTacToe.xWins} : O ${state.ticTacToe.oWins}"
                    }
                    Text(
                        text = scoreBadge,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )

                    if (!isRemoteTvMode) {
                        Row {
                            IconButton(
                                onClick = { onInput(InputAction.START_PAUSE, 0f, true) },
                                modifier = Modifier.testTag("solo_pause_button"),
                            ) {
                                Icon(
                                    imageVector = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = strings.pauseResume,
                                    tint = VoltLime,
                                )
                            }
                            IconButton(
                                onClick = { onInput(InputAction.RESET, 0f, true) },
                                modifier = Modifier.testTag("solo_reset_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = strings.restartGame,
                                    tint = ArcadeMagenta,
                                )
                            }
                        }
                    }
                }
            }

            // Main Game Board Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ArcadeGameBoardCanvas(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!isRemoteTvMode && state.activeGame == GameType.PONG) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val normY = (change.position.y / size.height.toFloat()).coerceIn(0.10f, 0.90f)
                                        onInput(InputAction.PADDLE_POS, normY, true)
                                    }
                                }
                            } else if (!isRemoteTvMode && state.activeGame == GameType.BRICKS) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val normX = (change.position.x / size.width.toFloat()).coerceIn(0.11f, 0.89f)
                                        onInput(InputAction.PADDLE_POS, normX, true)
                                    }
                                }
                            } else if (!isRemoteTvMode && state.activeGame == GameType.SNAKE) {
                                Modifier.pointerInput(Unit) {
                                    var dx = 0f
                                    var dy = 0f
                                    detectDragGestures(
                                        onDragStart = { dx = 0f; dy = 0f },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dx += amount.x
                                            dy += amount.y
                                            if (abs(dx) > 40f || abs(dy) > 40f) {
                                                if (abs(dx) > abs(dy)) {
                                                    onInput(if (dx > 0) InputAction.RIGHT else InputAction.LEFT, 0f, true)
                                                } else {
                                                    onInput(if (dy > 0) InputAction.DOWN else InputAction.UP, 0f, true)
                                                }
                                                dx = 0f
                                                dy = 0f
                                            }
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            }
                        ),
                    onTapCell = if (!isRemoteTvMode) {
                        { idx -> onInput(InputAction.CELL_TAP, idx.toFloat(), true) }
                    } else null,
                )

                // Overlay prompt when paused or game ended
                val overlayText: String? = when (state.activeGame) {
                    GameType.PONG -> when {
                        state.pong.winner != 0 -> "${strings.playerLabel} ${state.pong.winner} ${strings.youWinLabel}"
                        state.isPaused -> strings.tapStartToPlay
                        else -> null
                    }
                    GameType.SNAKE -> when {
                        state.snake.gameOver -> "${strings.gameOverLabel} (${state.snake.score})"
                        state.isPaused -> strings.tapStartToPlay
                        else -> null
                    }
                    GameType.BRICKS -> when {
                        state.bricks.gameOver -> "${strings.gameOverLabel} (${state.bricks.score})"
                        state.isPaused || state.bricks.ballAttached -> strings.tapStartToPlay
                        else -> null
                    }
                    GameType.TICTACTOE -> when (state.ticTacToe.winner) {
                        1 -> "${strings.youWinLabel} (X)"
                        2 -> "${strings.youWinLabel} (O)"
                        3 -> strings.drawLabel
                        else -> null
                    }
                }

                if (overlayText != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianBg.copy(alpha = 0.88f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = overlayText,
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricCyan,
                            )
                            if (!isRemoteTvMode) {
                                Button(
                                    onClick = { onInput(InputAction.ACTION_A, 0f, true) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("solo_start_overlay_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VoltLime,
                                        contentColor = ObsidianBg,
                                    ),
                                ) {
                                    Text(text = "START", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Solo Mode Quick On-Screen D-Pad Bar
            if (!isRemoteTvMode && state.activeGame != GameType.TICTACTOE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.activeGame == GameType.SNAKE || state.activeGame == GameType.BRICKS) {
                        SoloPadBtn(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", Modifier.weight(1f)) { pressed ->
                            onInput(InputAction.LEFT, 0f, pressed)
                        }
                    }
                    if (state.activeGame == GameType.SNAKE || state.activeGame == GameType.PONG) {
                        SoloPadBtn(Icons.Filled.KeyboardArrowUp, "Up", Modifier.weight(1f)) { pressed ->
                            onInput(InputAction.UP, 0f, pressed)
                        }
                        SoloPadBtn(Icons.Filled.KeyboardArrowDown, "Down", Modifier.weight(1f)) { pressed ->
                            onInput(InputAction.DOWN, 0f, pressed)
                        }
                    }
                    if (state.activeGame == GameType.SNAKE || state.activeGame == GameType.BRICKS) {
                        SoloPadBtn(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", Modifier.weight(1f)) { pressed ->
                            onInput(InputAction.RIGHT, 0f, pressed)
                        }
                    }
                    Button(
                        onClick = { onInput(InputAction.ACTION_A, 0f, true) },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoltLime,
                            contentColor = ObsidianBg,
                        ),
                    ) {
                        Text(text = "A", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SoloPadBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    onHoldChanged: (Boolean) -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.PressInteraction.Press -> onHoldChanged(true)
                is androidx.compose.foundation.interaction.PressInteraction.Release,
                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> onHoldChanged(false)
            }
        }
    }
    Button(
        onClick = {},
        interactionSource = interactionSource,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ElevatedCard,
            contentColor = ElectricCyan,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
    ) {
        Icon(imageVector = icon, contentDescription = desc, modifier = Modifier.size(30.dp))
    }
}
