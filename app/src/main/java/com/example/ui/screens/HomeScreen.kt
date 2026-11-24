package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.ConnectedClient
import com.example.model.GameType
import com.example.model.HighScores
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.VoltLime

@Composable
fun HomeScreen(
    strings: AppStrings,
    language: AppLanguage,
    highScores: HighScores,
    isServerRunning: Boolean,
    connectedClients: List<ConnectedClient>,
    onToggleLanguage: () -> Unit,
    onHostForTv: () -> Unit,
    onTvScreenMode: () -> Unit,
    onJoinAsPlayer2: () -> Unit,
    onOpenGamepad: () -> Unit,
    onPlayOnTv: (GameType) -> Unit,
    onPlaySolo: (GameType) -> Unit,
) {
    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Top Header Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricCyan.copy(alpha = 0.14f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WifiTethering,
                                    contentDescription = strings.appTitle,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WifiOff,
                                        contentDescription = null,
                                        tint = VoltLime,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = strings.offlineBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VoltLime,
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .testTag("language_toggle_button")
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onToggleLanguage),
                            color = ElevatedCard,
                            shape = RoundedCornerShape(999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = "Language",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "EN" else "বাংলা",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }

                // Hero Banner & Connection Actions
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, ElevatedBorder, RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_arcade),
                            contentDescription = strings.heroTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(225.dp),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ObsidianBg.copy(alpha = 0.25f),
                                            ObsidianBg.copy(alpha = 0.85f),
                                            ObsidianBg.copy(alpha = 0.96f),
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = strings.heroTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                            )
                            Text(
                                text = strings.heroSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onHostForTv,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("host_for_tv_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CastConnected,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.hostForTvBtn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onTvScreenMode,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("tv_screen_mode_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ArcadeMagenta.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MidnightSurface.copy(alpha = 0.85f),
                                        contentColor = ArcadeMagenta,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tv,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.tvScreenModeBtn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }

                                OutlinedButton(
                                    onClick = onJoinAsPlayer2,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("join_p2_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MidnightSurface.copy(alpha = 0.85f),
                                        contentColor = VoltLime,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.GroupAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.joinP2Btn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Server Status Banner
                item {
                    AnimatedVisibility(visible = isServerRunning) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = ElevatedCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.55f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(VoltLime)
                                    )
                                    Column {
                                        Text(
                                            text = strings.serverRunning,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = VoltLime,
                                        )
                                        Text(
                                            text = "${strings.connectedDevices}: ${connectedClients.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Button(
                                    onClick = onOpenGamepad,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("home_open_gamepad_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VoltLime,
                                        contentColor = ObsidianBg,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Gamepad,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.openGamepad,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                // Games Section Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.gamesSectionTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = strings.gamesSectionSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Game Cards
                item {
                    GameCardItem(
                        title = strings.pongName,
                        description = strings.pongDesc,
                        playerBadge = strings.players1to2,
                        bestLabel = "${strings.bestScore}: ${highScores.pongBest}",
                        accentColor = ElectricCyan,
                        drawableRes = R.drawable.img_game_pong,
                        strings = strings,
                        testTagPrefix = "pong",
                        onPlayOnTv = { onPlayOnTv(GameType.PONG) },
                        onPlaySolo = { onPlaySolo(GameType.PONG) },
                    )
                }

                item {
                    GameCardItem(
                        title = strings.snakeName,
                        description = strings.snakeDesc,
                        playerBadge = strings.players1,
                        bestLabel = "${strings.bestScore}: ${highScores.snakeBest}",
                        accentColor = VoltLime,
                        drawableRes = R.drawable.img_game_snake,
                        strings = strings,
                        testTagPrefix = "snake",
                        onPlayOnTv = { onPlayOnTv(GameType.SNAKE) },
                        onPlaySolo = { onPlaySolo(GameType.SNAKE) },
                    )
                }

                item {
                    GameCardItem(
                        title = strings.bricksName,
                        description = strings.bricksDesc,
                        playerBadge = strings.players1,
                        bestLabel = "${strings.bestScore}: ${highScores.bricksBest}",
                        accentColor = SolarAmber,
                        drawableRes = R.drawable.img_game_bricks,
                        strings = strings,
                        testTagPrefix = "bricks",
                        onPlayOnTv = { onPlayOnTv(GameType.BRICKS) },
                        onPlaySolo = { onPlaySolo(GameType.BRICKS) },
                    )
                }

                item {
                    GameCardItem(
                        title = strings.ticTacToeName,
                        description = strings.ticTacToeDesc,
                        playerBadge = strings.players1to2,
                        bestLabel = "${strings.bestScore}: ${highScores.ticTacToeWins}",
                        accentColor = ArcadeMagenta,
                        drawableRes = null,
                        strings = strings,
                        testTagPrefix = "tictactoe",
                        onPlayOnTv = { onPlayOnTv(GameType.TICTACTOE) },
                        onPlaySolo = { onPlaySolo(GameType.TICTACTOE) },
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun GameCardItem(
    title: String,
    description: String,
    playerBadge: String,
    bestLabel: String,
    accentColor: Color,
    drawableRes: Int?,
    strings: AppStrings,
    testTagPrefix: String,
    onPlayOnTv: () -> Unit,
    onPlaySolo: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_card_$testTagPrefix"),
        shape = RoundedCornerShape(20.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(ObsidianBg)
            ) {
                if (drawableRes != null) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    TicTacToeArtworkBanner(modifier = Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MidnightSurface.copy(alpha = 0.85f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianBg.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = playerBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianBg.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                    ) {
                        Text(
                            text = bestLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = accentColor,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onPlayOnTv,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("play_tv_$testTagPrefix"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = ObsidianBg,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.playOnTv,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    FilledTonalButton(
                        onClick = onPlaySolo,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("play_solo_$testTagPrefix"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ElevatedCard,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.playSolo,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicTacToeArtworkBanner(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(ObsidianBg)) {
        val board = size.height * 0.78f
        val x0 = (size.width - board) / 2f
        val y0 = (size.height - board) / 2f
        val cell = board / 3f
        for (i in 1..2) {
            drawLine(ElevatedBorder, Offset(x0 + i * cell, y0), Offset(x0 + i * cell, y0 + board), strokeWidth = 4f)
            drawLine(ElevatedBorder, Offset(x0, y0 + i * cell), Offset(x0 + board, y0 + i * cell), strokeWidth = 4f)
        }
        val pad = cell * 0.25f
        // Draw glowing X in center
        val cx = x0 + 1.5f * cell
        val cy = y0 + 1.5f * cell
        drawLine(ElectricCyan, Offset(cx - pad, cy - pad), Offset(cx + pad, cy + pad), strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(ElectricCyan, Offset(cx + pad, cy - pad), Offset(cx - pad, cy + pad), strokeWidth = 8f, cap = StrokeCap.Round)
        // Draw glowing O top-left
        drawCircle(ArcadeMagenta, radius = pad, center = Offset(x0 + 0.5f * cell, y0 + 0.5f * cell), style = Stroke(8f))
        // Draw glowing O bottom-right
        drawCircle(VoltLime, radius = pad, center = Offset(x0 + 2.5f * cell, y0 + 2.5f * cell), style = Stroke(8f))
    }
}
