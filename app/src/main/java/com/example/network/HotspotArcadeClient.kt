package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.example.model.ArcadeGameState
import com.example.model.DiscoveredHost
import com.example.model.GameType
import com.example.model.InputAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket

class HotspotArcadeClient(
    private val context: Context,
    private val scope: CoroutineScope,
    private val hotspotDetector: HotspotDetector,
) {
    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _assignedSlot = MutableStateFlow(1)
    val assignedSlot: StateFlow<Int> = _assignedSlot.asStateFlow()

    private val _pingMs = MutableStateFlow(0)
    val pingMs: StateFlow<Int> = _pingMs.asStateFlow()

    private val _remoteState = MutableStateFlow(ArcadeGameState())
    val remoteState: StateFlow<ArcadeGameState> = _remoteState.asStateFlow()

    private var scanJob: Job? = null
    private var connJob: Job? = null
    private var pingJob: Job? = null
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

    private var nsdManager: NsdManager? = null
    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        stopDiscovery()
        _isScanning.value = true
        _discoveredHosts.value = emptyList()

        // 1. UDP listener + active probe
        scanJob = scope.launch(Dispatchers.IO) {
            var udp: DatagramSocket? = null
            try {
                udp = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(HotspotArcadeServer.UDP_DISCOVERY_PORT))
                    soTimeout = 900
                    broadcast = true
                }
                val probe = "DISCOVER_HOST".toByteArray(Charsets.UTF_8)
                val broadcastAddrs = hotspotDetector.getBroadcastAddresses()
                for (addr in broadcastAddrs) {
                    runCatching {
                        udp.send(
                            DatagramPacket(probe, probe.size, addr, HotspotArcadeServer.UDP_DISCOVERY_PORT)
                        )
                    }
                }

                val buf = ByteArray(512)
                while (isActive && _isScanning.value) {
                    try {
                        val pkt = DatagramPacket(buf, buf.size)
                        udp.receive(pkt)
                        val msg = String(pkt.data, 0, pkt.length, Charsets.UTF_8)
                        if (msg.startsWith("HOTSPOT_ARCADE_HOST|")) {
                            val parts = msg.split("|")
                            val hostName = parts.getOrNull(1) ?: "Hotspot Host"
                            val ip = parts.getOrNull(2) ?: pkt.address.hostAddress ?: continue
                            val tcpPort = parts.getOrNull(3)?.toIntOrNull() ?: HotspotArcadeServer.TCP_PORT
                            val httpPort = parts.getOrNull(4)?.toIntOrNull() ?: HotspotArcadeServer.HTTP_PORT
                            addOrUpdateHost(DiscoveredHost(hostName, ip, tcpPort, httpPort))
                        }
                    } catch (_: Exception) {
                        // Resend probe periodically
                        for (addr in hotspotDetector.getBroadcastAddresses()) {
                            runCatching {
                                udp.send(
                                    DatagramPacket(probe, probe.size, addr, HotspotArcadeServer.UDP_DISCOVERY_PORT)
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { udp?.close() }
            }
        }

        // 2. Android NSD discovery
        startNsdDiscovery()
    }

    fun stopDiscovery() {
        _isScanning.value = false
        scanJob?.cancel()
        scanJob = null
        stopNsdDiscovery()
    }

    private fun addOrUpdateHost(host: DiscoveredHost) {
        val current = _discoveredHosts.value.toMutableList()
        val idx = current.indexOfFirst { it.ipAddress == host.ipAddress }
        if (idx >= 0) {
            current[idx] = host
        } else {
            current.add(host)
        }
        _discoveredHosts.value = current
    }

    fun connect(ipAddress: String, port: Int = HotspotArcadeServer.TCP_PORT, asPlayer2: Boolean) {
        disconnect()
        _isConnecting.value = true
        _connectionError.value = null

        connJob = scope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(ipAddress.trim(), port), 4500)
                s.tcpNoDelay = true
                socket = s

                val w = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                val r = BufferedReader(InputStreamReader(s.getInputStream()))
                writer = w

                val roleCode = if (asPlayer2) "P2" else "TV"
                val myName = "${Build.MANUFACTURER} ${Build.MODEL}".replace(":", " ").trim()
                synchronized(w) {
                    w.write("HELLO:$roleCode:$myName\n")
                    w.flush()
                }

                _isConnecting.value = false
                _isConnected.value = true
                stopDiscovery()

                // Start ping loop
                pingJob = scope.launch(Dispatchers.IO) {
                    while (isActive && _isConnected.value) {
                        runCatching {
                            val now = System.currentTimeMillis()
                            synchronized(w) {
                                w.write("PING:$now\n")
                                w.flush()
                            }
                        }
                        delay(2000L)
                    }
                }

                while (isActive && !s.isClosed) {
                    val line = r.readLine() ?: break
                    when {
                        line.startsWith("WELCOME:") -> {
                            _assignedSlot.value = line.removePrefix("WELCOME:").toIntOrNull() ?: 1
                        }
                        line.startsWith("PONG:") -> {
                            val sent = line.removePrefix("PONG:").toLongOrNull() ?: 0L
                            if (sent > 0L) {
                                _pingMs.value = (System.currentTimeMillis() - sent).toInt().coerceAtLeast(1)
                            }
                        }
                        line.startsWith("STATE:") -> {
                            val json = line.removePrefix("STATE:")
                            ArcadeGameState.fromJsonString(json)?.let { parsed ->
                                _remoteState.value = parsed
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionError.value = e.message ?: "Connection failed"
            } finally {
                _isConnecting.value = false
                _isConnected.value = false
                pingJob?.cancel()
                runCatching { socket?.close() }
                socket = null
                writer = null
            }
        }
    }

    fun sendInput(action: InputAction, value: Float = 0f, pressed: Boolean = true) {
        val w = writer ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                synchronized(w) {
                    w.write("INPUT:${action.wire}:$value:$pressed\n")
                    w.flush()
                }
            }
        }
    }

    fun sendGameSwitch(game: GameType) {
        val w = writer ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                synchronized(w) {
                    w.write("GAME:${game.id}\n")
                    w.flush()
                }
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        connJob?.cancel()
        connJob = null
        runCatching { socket?.close() }
        socket = null
        writer = null
        _isConnected.value = false
        _isConnecting.value = false
    }

    private fun startNsdDiscovery() {
        try {
            val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = manager
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (serviceInfo.serviceType.contains("hotspotarcade")) {
                        @Suppress("DEPRECATION")
                        manager.resolveService(
                            serviceInfo,
                            object : NsdManager.ResolveListener {
                                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                                override fun onServiceResolved(info: NsdServiceInfo) {
                                    @Suppress("DEPRECATION")
                                    val hostIp = info.host?.hostAddress ?: return
                                    addOrUpdateHost(
                                        DiscoveredHost(
                                            hostName = info.serviceName,
                                            ipAddress = hostIp,
                                            tcpPort = info.port,
                                            httpPort = HotspotArcadeServer.HTTP_PORT,
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            nsdDiscoveryListener = listener
            manager.discoverServices(HotspotArcadeServer.NSD_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
        }
    }

    private fun stopNsdDiscovery() {
        try {
            nsdDiscoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {
        } finally {
            nsdDiscoveryListener = null
        }
    }
}
