package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.i18n.stringsFor
import com.example.model.AppScreen
import com.example.model.DeviceRole
import com.example.ui.screens.ConnectTvScreen
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.DiscoverHostScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TvDisplayScreen
import com.example.ui.theme.HotspotArcadeTheme
import com.example.viewmodel.ArcadeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HotspotArcadeTheme {
                HotspotArcadeApp()
            }
        }
    }
}

@Composable
fun HotspotArcadeApp(viewModel: ArcadeViewModel = viewModel()) {
    val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val strings = stringsFor(language)

    val highScores by viewModel.highScores.collectAsStateWithLifecycle()
    val isServerRunning by viewModel.isServerRunning.collectAsStateWithLifecycle()
    val connectedClients by viewModel.connectedClients.collectAsStateWithLifecycle()
    val hotspotInfo by viewModel.hotspotInfo.collectAsStateWithLifecycle()
    val hotspotError by viewModel.hotspotError.collectAsStateWithLifecycle()
    val deviceRole by viewModel.deviceRole.collectAsStateWithLifecycle()
    val discoverAsP2 by viewModel.discoverAsPlayer2.collectAsStateWithLifecycle()
    val discoveredHosts by viewModel.discoveredHosts.collectAsStateWithLifecycle()
    val isScanningHosts by viewModel.isScanningHosts.collectAsStateWithLifecycle()
    val isClientConnected by viewModel.isClientConnected.collectAsStateWithLifecycle()
    val clientError by viewModel.clientError.collectAsStateWithLifecycle()
    val clientPingMs by viewModel.clientPingMs.collectAsStateWithLifecycle()
    val assignedSlot by viewModel.assignedPlayerSlot.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val gameState by viewModel.activeGameState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "screen_transition",
    ) { target ->
        when (target) {
            AppScreen.HOME -> HomeScreen(
                strings = strings,
                language = language,
                highScores = highScores,
                isServerRunning = isServerRunning,
                connectedClients = connectedClients,
                onToggleLanguage = viewModel::toggleLanguage,
                onHostForTv = viewModel::openConnectTvWizard,
                onTvScreenMode = { viewModel.openDiscoverHost(asPlayer2 = false) },
                onJoinAsPlayer2 = { viewModel.openDiscoverHost(asPlayer2 = true) },
                onOpenGamepad = viewModel::openGamepadFromHost,
                onPlayOnTv = viewModel::playGameOnTv,
                onPlaySolo = viewModel::playSolo,
            )

            AppScreen.CONNECT_TV -> ConnectTvScreen(
                strings = strings,
                hotspotInfo = hotspotInfo,
                hotspotError = hotspotError,
                isServerRunning = isServerRunning,
                connectedClients = connectedClients,
                onBack = viewModel::navigateBack,
                onRefreshIp = viewModel::refreshHotspotInfo,
                onStartLocalHotspot = viewModel::startLocalOnlyHotspot,
                onStopLocalHotspot = viewModel::stopLocalOnlyHotspot,
                onToggleServer = viewModel::toggleServer,
                onOpenGamepad = viewModel::openGamepadFromHost,
            )

            AppScreen.DISCOVER_HOST -> DiscoverHostScreen(
                strings = strings,
                asPlayer2 = discoverAsP2,
                defaultGatewayIp = viewModel.guessGatewayIp(),
                discoveredHosts = discoveredHosts,
                isScanning = isScanningHosts,
                connectionError = clientError,
                onBack = viewModel::navigateBack,
                onRescan = viewModel::rescanHosts,
                onConnectToHost = viewModel::connectToHost,
            )

            AppScreen.CONTROLLER -> ControllerScreen(
                strings = strings,
                state = gameState,
                playerSlot = if (deviceRole == DeviceRole.PLAYER_2_CONTROLLER) assignedSlot else 1,
                pingMs = clientPingMs,
                isHost = deviceRole == DeviceRole.HOST_CONTROLLER,
                connectedClients = connectedClients,
                hapticsEnabled = hapticsEnabled,
                onBack = {
                    if (deviceRole == DeviceRole.PLAYER_2_CONTROLLER) {
                        viewModel.disconnectClient()
                    } else {
                        viewModel.navigateBack()
                    }
                },
                onToggleHaptics = viewModel::toggleHaptics,
                onSelectGame = viewModel::selectGame,
                onInput = viewModel::sendControllerInput,
            )

            AppScreen.TV_DISPLAY -> TvDisplayScreen(
                strings = strings,
                state = gameState,
                isRemoteTvMode = deviceRole == DeviceRole.TV_DISPLAY,
                isConnected = isClientConnected,
                pingMs = clientPingMs,
                onBack = {
                    if (deviceRole == DeviceRole.TV_DISPLAY) {
                        viewModel.disconnectClient()
                    } else {
                        viewModel.navigateBack()
                    }
                },
                onInput = viewModel::sendControllerInput,
            )
        }
    }
}
