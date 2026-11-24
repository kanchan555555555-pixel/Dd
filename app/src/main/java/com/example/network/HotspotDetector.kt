package com.example.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.model.HotspotInfo
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class HotspotDetector(private val context: Context) {

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun inspectNetwork(): HotspotInfo {
        val candidates = mutableListOf<Pair<String, String>>() // ifaceName -> ip
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return HotspotInfo()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("127.")) {
                            candidates.add(nif.name.lowercase() to ip)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return HotspotInfo()
        }

        // Prioritize hotspot / soft-AP / Wi-Fi LAN interfaces
        val prioritized = candidates.sortedWith(
            compareByDescending<Pair<String, String>> { (iface, ip) ->
                when {
                    iface.contains("ap") || iface.contains("swlan") || iface.contains("softap") -> 100
                    ip.startsWith("192.168.43.") || ip.startsWith("192.168.137.") -> 95
                    iface.startsWith("wlan") && ip.startsWith("192.168.") -> 85
                    iface.startsWith("wlan") -> 75
                    ip.startsWith("192.168.") || ip.startsWith("10.") -> 50
                    else -> 10
                }
            }
        )

        val best = prioritized.firstOrNull()
        val hotspotLikely = prioritized.any { (iface, ip) ->
            iface.contains("ap") || iface.contains("swlan") || ip.startsWith("192.168.43.") || ip.endsWith(".1")
        }

        val activeRes = reservation
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activeRes?.softApConfiguration
        } else {
            @Suppress("DEPRECATION")
            activeRes?.wifiConfiguration
        }

        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            activeRes?.softApConfiguration?.ssid
        } else {
            @Suppress("DEPRECATION")
            activeRes?.wifiConfiguration?.SSID
        }

        val pass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activeRes?.softApConfiguration?.passphrase
        } else {
            @Suppress("DEPRECATION")
            activeRes?.wifiConfiguration?.preSharedKey
        }

        return HotspotInfo(
            localIp = best?.second,
            candidateIps = prioritized.map { it.second }.distinct(),
            localOnlyActive = config != null,
            localOnlySsid = ssid,
            localOnlyPassword = pass,
            isHotspotInterfaceDetected = hotspotLikely || config != null,
        )
    }

    fun getBroadcastAddresses(): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                for (ifAddr in nif.interfaceAddresses) {
                    val addr = ifAddr.address
                    if (addr is Inet4Address && ifAddr.broadcast != null) {
                        result.add(ifAddr.broadcast)
                    }
                }
            }
        } catch (_: Exception) {
        }
        try {
            result.add(InetAddress.getByName("255.255.255.255"))
        } catch (_: Exception) {
        }
        return result.distinct()
    }

    fun guessHotspotGatewayIp(): String {
        val info = inspectNetwork()
        val ip = info.localIp ?: return "192.168.43.1"
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.1" else "192.168.43.1"
    }

    @SuppressLint("MissingPermission")
    fun startLocalOnlyHotspot(
        onStarted: (HotspotInfo) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onError("Local-only hotspot requires Android 8.0+")
            return
        }
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            onError("Wi-Fi service unavailable")
            return
        }
        try {
            reservation?.close()
            reservation = null
            wifiManager.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                        reservation = res
                        onStarted(inspectNetwork())
                    }

                    override fun onStopped() {
                        reservation = null
                        onStarted(inspectNetwork())
                    }

                    override fun onFailed(reason: Int) {
                        reservation = null
                        onError("Hotspot failed (code $reason)")
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (se: SecurityException) {
            onError(se.message ?: "Permission required")
        } catch (e: Exception) {
            onError(e.message ?: "Failed to start local hotspot")
        }
    }

    fun stopLocalOnlyHotspot() {
        try {
            reservation?.close()
        } catch (_: Exception) {
        } finally {
            reservation = null
        }
    }
}
