package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Base64
import com.example.engine.ArcadeGameEngine
import com.example.model.ConnectedClient
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
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class HotspotArcadeServer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val engine: ArcadeGameEngine,
    private val hotspotDetector: HotspotDetector,
) {
    companion object {
        const val HTTP_PORT = 8080
        const val TCP_PORT = 45455
        const val UDP_DISCOVERY_PORT = 45454
        const val NSD_TYPE = "_hotspotarcade._tcp."
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _clients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val clients: StateFlow<List<ConnectedClient>> = _clients.asStateFlow()

    private var tcpServerSocket: ServerSocket? = null
    private var httpServerSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null

    private var jobs = mutableListOf<Job>()
    private val tcpWriters = CopyOnWriteArrayList<TcpClientSession>()
    private val webSocketClients = CopyOnWriteArrayList<WebSocketSession>()

    private var nsdManager: NsdManager? = null
    private var nsdListener: NsdManager.RegistrationListener? = null

    private data class TcpClientSession(
        val client: ConnectedClient,
        val socket: Socket,
        val writer: BufferedWriter,
    )

    private data class WebSocketSession(
        val client: ConnectedClient,
        val socket: Socket,
        val out: OutputStream,
    )

    fun start() {
        if (_isRunning.value) return
        _isRunning.value = true

        jobs += scope.launch(Dispatchers.IO) { runTcpServer() }
        jobs += scope.launch(Dispatchers.IO) { runHttpWebSocketServer() }
        jobs += scope.launch(Dispatchers.IO) { runUdpDiscoveryBeacon() }
        jobs += scope.launch(Dispatchers.IO) { broadcastGameStateLoop() }
        registerNsd()
    }

    fun stop() {
        _isRunning.value = false
        unregisterNsd()
        jobs.forEach { it.cancel() }
        jobs.clear()

        tcpWriters.forEach { runCatching { it.socket.close() } }
        tcpWriters.clear()
        webSocketClients.forEach { runCatching { it.socket.close() } }
        webSocketClients.clear()

        runCatching { tcpServerSocket?.close() }
        runCatching { httpServerSocket?.close() }
        runCatching { udpSocket?.close() }
        tcpServerSocket = null
        httpServerSocket = null
        udpSocket = null

        _clients.value = emptyList()
        engine.setTwoPlayersConnected(false)
    }

    private fun refreshClientsState() {
        val list = buildList {
            tcpWriters.forEach { add(it.client) }
            webSocketClients.forEach { add(it.client) }
        }
        _clients.value = list
        val hasP2 = list.any { it.playerSlot == 2 }
        engine.setTwoPlayersConnected(hasP2)
    }

    private suspend fun runTcpServer() {
        try {
            val server = ServerSocket(TCP_PORT)
            server.reuseAddress = true
            tcpServerSocket = server
            while (scope.isActive && _isRunning.value) {
                val socket = server.accept()
                socket.tcpNoDelay = true
                scope.launch(Dispatchers.IO) { handleTcpClient(socket) }
            }
        } catch (_: Exception) {
        }
    }

    private fun handleTcpClient(socket: Socket) {
        val id = UUID.randomUUID().toString().take(8)
        var session: TcpClientSession? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            val hello = reader.readLine() ?: return
            // Format: HELLO:TV:DeviceName or HELLO:P2:PlayerName
            val parts = hello.split(":", limit = 3)
            val kind = if (parts.getOrNull(1) == "P2") "P2_APP" else "TV_APP"
            val name = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                ?: if (kind == "P2_APP") "Player 2" else "Android TV"
            val slot = if (kind == "P2_APP") 2 else 0

            val client = ConnectedClient(id = id, name = name, kind = kind, playerSlot = slot)
            session = TcpClientSession(client, socket, writer)
            tcpWriters.add(session)
            refreshClientsState()

            // Immediately send current state
            synchronized(writer) {
                writer.write("WELCOME:$slot\n")
                writer.write("STATE:${engine.state.value.toJsonString()}\n")
                writer.flush()
            }

            while (_isRunning.value && !socket.isClosed) {
                val line = reader.readLine() ?: break
                when {
                    line.startsWith("PING:") -> {
                        val ts = line.removePrefix("PING:")
                        synchronized(writer) {
                            writer.write("PONG:$ts\n")
                            writer.flush()
                        }
                    }
                    line.startsWith("INPUT:") -> {
                        // INPUT:action:value:pressed
                        val p = line.split(":")
                        val action = InputAction.fromWire(p.getOrNull(1) ?: "")
                        val value = p.getOrNull(2)?.toFloatOrNull() ?: 0f
                        val pressed = p.getOrNull(3)?.toBooleanStrictOrNull() ?: true
                        if (action != null) {
                            val targetSlot = if (slot == 2) 2 else 1
                            engine.handleInput(targetSlot, action, value, pressed)
                        }
                    }
                    line.startsWith("GAME:") -> {
                        val g = GameType.fromId(line.removePrefix("GAME:"))
                        engine.selectGame(g, autoStart = false)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            session?.let { tcpWriters.remove(it) }
            runCatching { socket.close() }
            refreshClientsState()
        }
    }

    private suspend fun runHttpWebSocketServer() {
        try {
            val server = ServerSocket(HTTP_PORT)
            server.reuseAddress = true
            httpServerSocket = server
            while (scope.isActive && _isRunning.value) {
                val socket = server.accept()
                socket.tcpNoDelay = true
                scope.launch(Dispatchers.IO) { handleHttpOrWebSocket(socket) }
            }
        } catch (_: Exception) {
        }
    }

    private fun handleHttpOrWebSocket(socket: Socket) {
        var wsSession: WebSocketSession? = null
        try {
            val input = socket.getInputStream()
            val out = socket.getOutputStream()
            val headers = readHttpHeaders(input) ?: return
            val requestLine = headers.firstOrNull() ?: return
            val isWsUpgrade = headers.any { it.lowercase().startsWith("upgrade: websocket") }
            val wsKey = headers.firstOrNull { it.lowercase().startsWith("sec-websocket-key:") }
                ?.substringAfter(":")?.trim()

            if (isWsUpgrade && wsKey != null) {
                val accept = computeWebSocketAccept(wsKey)
                val response = buildString {
                    append("HTTP/1.1 101 Switching Protocols\r\n")
                    append("Upgrade: websocket\r\n")
                    append("Connection: Upgrade\r\n")
                    append("Sec-WebSocket-Accept: $accept\r\n\r\n")
                }
                out.write(response.toByteArray(Charsets.UTF_8))
                out.flush()

                val client = ConnectedClient(
                    id = UUID.randomUUID().toString().take(8),
                    name = "Smart TV Browser",
                    kind = "TV_WEB",
                    playerSlot = 0,
                )
                wsSession = WebSocketSession(client, socket, out)
                webSocketClients.add(wsSession)
                refreshClientsState()

                // Send initial state frame
                sendWebSocketTextFrame(out, engine.state.value.toJsonString())

                // Keep reading until browser disconnects
                val buf = ByteArray(1024)
                while (_isRunning.value && !socket.isClosed) {
                    val read = input.read(buf)
                    if (read == -1) break
                    // Handle WebSocket close opcode (0x88)
                    if ((buf[0].toInt() and 0x0F) == 0x8) break
                }
            } else {
                val htmlBytes = if (requestLine.contains("GET /")) {
                    TvWebAsset.buildHtml().toByteArray(Charsets.UTF_8)
                } else {
                    TvWebAsset.buildHtml().toByteArray(Charsets.UTF_8)
                }
                val header = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: text/html; charset=utf-8\r\n")
                    append("Content-Length: ${htmlBytes.size}\r\n")
                    append("Cache-Control: no-store\r\n")
                    append("Connection: close\r\n\r\n")
                }
                out.write(header.toByteArray(Charsets.UTF_8))
                out.write(htmlBytes)
                out.flush()
                socket.close()
            }
        } catch (_: Exception) {
        } finally {
            wsSession?.let {
                webSocketClients.remove(it)
                refreshClientsState()
            }
            runCatching { socket.close() }
        }
    }

    private fun readHttpHeaders(input: InputStream): List<String>? {
        val lines = mutableListOf<String>()
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) return null
            if (b == '\n'.code) {
                val line = sb.toString().trimEnd('\r')
                sb.clear()
                if (line.isEmpty()) break
                lines.add(line)
                if (lines.size > 64) break
            } else {
                sb.append(b.toChar())
            }
            prev = b
        }
        return if (lines.isEmpty() && prev == -1) null else lines
    }

    private fun computeWebSocketAccept(key: String): String {
        val concat = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val sha1 = MessageDigest.getInstance("SHA-1").digest(concat.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sha1, Base64.NO_WRAP)
    }

    private fun sendWebSocketTextFrame(out: OutputStream, text: String) {
        val payload = text.toByteArray(Charsets.UTF_8)
        val header = when {
            payload.size <= 125 -> byteArrayOf(0x81.toByte(), payload.size.toByte())
            payload.size <= 65535 -> byteArrayOf(
                0x81.toByte(),
                126.toByte(),
                ((payload.size shr 8) and 0xFF).toByte(),
                (payload.size and 0xFF).toByte(),
            )
            else -> return
        }
        synchronized(out) {
            out.write(header)
            out.write(payload)
            out.flush()
        }
    }

    private suspend fun broadcastGameStateLoop() {
        var lastJson = ""
        var ticksSinceSend = 0
        while (scope.isActive && _isRunning.value) {
            if (tcpWriters.isNotEmpty() || webSocketClients.isNotEmpty()) {
                val json = engine.state.value.toJsonString()
                if (json != lastJson || ticksSinceSend >= 15) {
                    lastJson = json
                    ticksSinceSend = 0

                    val tcpLine = "STATE:$json\n"
                    val deadTcp = mutableListOf<TcpClientSession>()
                    for (session in tcpWriters) {
                        try {
                            synchronized(session.writer) {
                                session.writer.write(tcpLine)
                                session.writer.flush()
                            }
                        } catch (_: Exception) {
                            deadTcp.add(session)
                        }
                    }
                    if (deadTcp.isNotEmpty()) {
                        deadTcp.forEach {
                            tcpWriters.remove(it)
                            runCatching { it.socket.close() }
                        }
                        refreshClientsState()
                    }

                    val deadWs = mutableListOf<WebSocketSession>()
                    for (ws in webSocketClients) {
                        try {
                            sendWebSocketTextFrame(ws.out, json)
                        } catch (_: Exception) {
                            deadWs.add(ws)
                        }
                    }
                    if (deadWs.isNotEmpty()) {
                        deadWs.forEach {
                            webSocketClients.remove(it)
                            runCatching { it.socket.close() }
                        }
                        refreshClientsState()
                    }
                } else {
                    ticksSinceSend++
                }
            }
            delay(20L)
        }
    }

    private suspend fun runUdpDiscoveryBeacon() {
        try {
            val socket = DatagramSocket(UDP_DISCOVERY_PORT)
            socket.broadcast = true
            socket.soTimeout = 1000
            udpSocket = socket
            val recvBuf = ByteArray(512)

            // Responder coroutine for active client DISCOVER probes
            scope.launch(Dispatchers.IO) {
                while (isActive && _isRunning.value && !socket.isClosed) {
                    try {
                        val packet = DatagramPacket(recvBuf, recvBuf.size)
                        socket.receive(packet)
                        val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        if (msg.startsWith("DISCOVER_HOST")) {
                            val payload = buildAnnouncePayload().toByteArray(Charsets.UTF_8)
                            val reply = DatagramPacket(payload, payload.size, packet.address, packet.port)
                            socket.send(reply)
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            while (scope.isActive && _isRunning.value && !socket.isClosed) {
                val payload = buildAnnouncePayload().toByteArray(Charsets.UTF_8)
                val addrs = hotspotDetector.getBroadcastAddresses()
                for (addr in addrs) {
                    runCatching {
                        socket.send(DatagramPacket(payload, payload.size, addr, UDP_DISCOVERY_PORT))
                    }
                }
                delay(1200L)
            }
        } catch (_: Exception) {
        }
    }

    private fun buildAnnouncePayload(): String {
        val ip = hotspotDetector.inspectNetwork().localIp ?: "192.168.43.1"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".replace("|", " ").trim()
        return "HOTSPOT_ARCADE_HOST|$deviceName|$ip|$TCP_PORT|$HTTP_PORT"
    }

    private fun registerNsd() {
        try {
            val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = manager
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "HotspotArcade-${Build.MODEL.take(12)}"
                serviceType = NSD_TYPE
                port = TCP_PORT
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {}
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            }
            nsdListener = listener
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
        }
    }

    private fun unregisterNsd() {
        try {
            nsdListener?.let { nsdManager?.unregisterService(it) }
        } catch (_: Exception) {
        } finally {
            nsdListener = null
        }
    }
}
