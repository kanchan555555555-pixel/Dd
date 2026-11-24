package com.example.viewmodel

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArcadePreferences
import com.example.engine.ArcadeGameEngine
import com.example.i18n.AppLanguage
import com.example.model.AppScreen
import com.example.model.ArcadeGameState
import com.example.model.ConnectedClient
import com.example.model.DeviceRole
import com.example.model.DiscoveredHost
import com.example.model.GameType
import com.example.model.HighScores
import com.example.model.HotspotInfo
import com.example.model.InputAction
import com.example.network.HotspotArcadeClient
import com.example.network.HotspotArcadeServer
import com.example.network.HotspotDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArcadeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = ArcadePreferences(application)
    private val hotspotDetector = HotspotDetector(application)
    private val engine = ArcadeGameEngine(viewModelScope)
    private val server = HotspotArcadeServer(application, viewModelScope, engine, hotspotDetector)
    private val client = HotspotArcadeClient(application, viewModelScope, hotspotDetector)

    private val _screenStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.HOME))
    val currentScreen: StateFlow<AppScreen> = _screenStack.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppScreen.HOME,
    ).let {
        MutableStateFlow(AppScreen.HOME).apply {
            viewModelScope.launch {
                _screenStack.collect { stack -> value = stack.lastOrNull() ?: AppScreen.HOME }
            }
        }
    }

    private val _deviceRole = MutableStateFlow(DeviceRole.HOST_CONTROLLER)
    val deviceRole: StateFlow<DeviceRole> = _deviceRole.asStateFlow()

    private val _discoverAsPlayer2 = MutableStateFlow(false)
    val discoverAsPlayer2: StateFlow<Boolean> = _discoverAsPlayer2.asStateFlow()

    private val _hotspotInfo = MutableStateFlow(hotspotDetector.inspectNetwork())
    val hotspotInfo: StateFlow<HotspotInfo> = _hotspotInfo.asStateFlow()

    private val _hotspotError = MutableStateFlow<String?>(null)
    val hotspotError: StateFlow<String?> = _hotspotError.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    val language: StateFlow<AppLanguage> = preferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.BN)

    val highScores: StateFlow<HighScores> = preferences.highScoresFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, HighScores())

    val isServerRunning: StateFlow<Boolean> = server.isRunning
    val connectedClients: StateFlow<List<ConnectedClient>> = server.clients

    val discoveredHosts: StateFlow<List<DiscoveredHost>> = client.discoveredHosts
    val isScanningHosts: StateFlow<Boolean> = client.isScanning
    val isClientConnected: StateFlow<Boolean> = client.isConnected
    val isClientConnecting: StateFlow<Boolean> = client.isConnecting
    val clientError: StateFlow<String?> = client.connectionError
    val clientPingMs: StateFlow<Int> = client.pingMs
    val assignedPlayerSlot: StateFlow<Int> = client.assignedSlot

    val activeGameState: StateFlow<ArcadeGameState> = combine(
        _deviceRole,
        engine.state,
        client.remoteState,
    ) { role, local, remote ->
        when (role) {
            DeviceRole.TV_DISPLAY, DeviceRole.PLAYER_2_CONTROLLER -> remote
            DeviceRole.HOST_CONTROLLER, DeviceRole.SOLO_LOCAL -> local
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ArcadeGameState())

    private var lastEventHandled = 0L

    init {
        engine.startLoop()
        viewModelScope.launch {
            activeGameState.collect { st ->
                if (st.eventId != lastEventHandled && st.eventId > 0L) {
                    lastEventHandled = st.eventId
                    if (st.lastEvent == "hit" || st.lastEvent == "score" || st.lastEvent == "win") {
                        triggerHaptic(st.lastEvent == "win" || st.lastEvent == "score")
                    }
                    persistHighScores(st)
                }
            }
        }
    }

    private fun persistHighScores(st: ArcadeGameState) {
        viewModelScope.launch {
            when (st.activeGame) {
                GameType.PONG -> {
                    val best = maxOf(st.pong.score1, st.pong.score2)
                    if (best > 0) preferences.recordPongScore(best)
                }
                GameType.SNAKE -> {
                    if (st.snake.score > 0) preferences.recordSnakeScore(st.snake.score)
                }
                GameType.BRICKS -> {
                    if (st.bricks.score > 0) preferences.recordBricksScore(st.bricks.score)
                }
                GameType.TICTACTOE -> {
                    if (st.lastEvent == "win" && st.ticTacToe.winner == 1) {
                        preferences.incrementTicTacToeWin()
                    }
                }
            }
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
            preferences.setLanguage(next)
        }
    }

    fun toggleHaptics() {
        _hapticsEnabled.value = !_hapticsEnabled.value
    }

    fun navigateTo(screen: AppScreen) {
        val current = _screenStack.value
        if (current.lastOrNull() != screen) {
            _screenStack.value = current + screen
        }
    }

    fun navigateBack() {
        val current = _screenStack.value
        if (current.size > 1) {
            val popped = current.last()
            if (popped == AppScreen.DISCOVER_HOST) {
                client.stopDiscovery()
            }
            _screenStack.value = current.dropLast(1)
        }
    }

    fun openConnectTvWizard() {
        _deviceRole.value = DeviceRole.HOST_CONTROLLER
        refreshHotspotInfo()
        if (!server.isRunning.value) {
            server.start()
        }
        navigateTo(AppScreen.CONNECT_TV)
    }

    fun openDiscoverHost(asPlayer2: Boolean) {
        _discoverAsPlayer2.value = asPlayer2
        _deviceRole.value = if (asPlayer2) DeviceRole.PLAYER_2_CONTROLLER else DeviceRole.TV_DISPLAY
        refreshHotspotInfo()
        client.startDiscovery()
        navigateTo(AppScreen.DISCOVER_HOST)
    }

    fun playGameOnTv(game: GameType) {
        _deviceRole.value = DeviceRole.HOST_CONTROLLER
        refreshHotspotInfo()
        if (!server.isRunning.value) {
            server.start()
        }
        engine.selectGame(game, autoStart = false)
        if (connectedClients.value.isNotEmpty()) {
            navigateTo(AppScreen.CONTROLLER)
        } else {
            navigateTo(AppScreen.CONNECT_TV)
        }
    }

    fun playSolo(game: GameType) {
        _deviceRole.value = DeviceRole.SOLO_LOCAL
        engine.selectGame(game, autoStart = false)
        navigateTo(AppScreen.TV_DISPLAY)
    }

    fun openGamepadFromHost() {
        _deviceRole.value = DeviceRole.HOST_CONTROLLER
        if (!server.isRunning.value) {
            server.start()
        }
        navigateTo(AppScreen.CONTROLLER)
    }

    fun refreshHotspotInfo() {
        _hotspotInfo.value = hotspotDetector.inspectNetwork()
    }

    fun guessGatewayIp(): String = hotspotDetector.guessHotspotGatewayIp()

    fun toggleServer() {
        if (server.isRunning.value) {
            server.stop()
        } else {
            server.start()
            refreshHotspotInfo()
        }
    }

    fun startLocalOnlyHotspot() {
        _hotspotError.value = null
        hotspotDetector.startLocalOnlyHotspot(
            onStarted = { info ->
                _hotspotInfo.value = info
                if (!server.isRunning.value) server.start()
            },
            onError = { err ->
                _hotspotError.value = err
            },
        )
    }

    fun stopLocalOnlyHotspot() {
        hotspotDetector.stopLocalOnlyHotspot()
        refreshHotspotInfo()
    }

    fun clearHotspotError() {
        _hotspotError.value = null
    }

    fun rescanHosts() {
        client.startDiscovery()
    }

    fun connectToHost(ipAddress: String, tcpPort: Int = HotspotArcadeServer.TCP_PORT) {
        val asP2 = _discoverAsPlayer2.value
        _deviceRole.value = if (asP2) DeviceRole.PLAYER_2_CONTROLLER else DeviceRole.TV_DISPLAY
        client.connect(ipAddress, tcpPort, asPlayer2 = asP2)
        navigateTo(if (asP2) AppScreen.CONTROLLER else AppScreen.TV_DISPLAY)
    }

    fun disconnectClient() {
        client.disconnect()
        _deviceRole.value = DeviceRole.HOST_CONTROLLER
        _screenStack.value = listOf(AppScreen.HOME)
    }

    fun selectGame(game: GameType) {
        when (_deviceRole.value) {
            DeviceRole.HOST_CONTROLLER, DeviceRole.SOLO_LOCAL -> engine.selectGame(game, autoStart = false)
            DeviceRole.PLAYER_2_CONTROLLER, DeviceRole.TV_DISPLAY -> client.sendGameSwitch(game)
        }
    }

    fun sendControllerInput(action: InputAction, value: Float = 0f, pressed: Boolean = true) {
        when (_deviceRole.value) {
            DeviceRole.HOST_CONTROLLER, DeviceRole.SOLO_LOCAL -> {
                engine.handleInput(playerSlot = 1, action = action, value = value, pressed = pressed)
            }
            DeviceRole.PLAYER_2_CONTROLLER, DeviceRole.TV_DISPLAY -> {
                client.sendInput(action = action, value = value, pressed = pressed)
            }
        }
    }

    fun triggerHaptic(strong: Boolean = false) {
        if (!_hapticsEnabled.value) return
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Vibrator::class.java)
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val dur = if (strong) 45L else 18L
                val amp = if (strong) 180 else 90
                vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (strong) 40L else 16L)
            }
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.stopLoop()
        server.stop()
        client.disconnect()
        client.stopDiscovery()
        hotspotDetector.stopLocalOnlyHotspot()
    }
}
