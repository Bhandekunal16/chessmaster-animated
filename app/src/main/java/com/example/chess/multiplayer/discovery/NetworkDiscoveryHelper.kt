package com.example.chess.multiplayer.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.example.chess.multiplayer.model.DiscoveredHost
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.*
import java.util.Collections

object NetworkDiscoveryHelper {

    const val DEFAULT_PORT = 8888
    const val BEACON_PORT = 8889
    private const val BEACON_MAGIC = "GM_CHESS_BEACON"

    /**
     * Resolves the primary local IPv4 address of the device on the Wi-Fi/LAN interface.
     */
    fun getLocalIpAddress(context: Context? = null): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Prefer wlan / en / eth interfaces
            val preferredInterfaces = interfaces.filter { iface ->
                !iface.isLoopback && iface.isUp && (iface.name.contains("wlan", ignoreCase = true) ||
                        iface.name.contains("eth", ignoreCase = true) ||
                        iface.name.contains("en", ignoreCase = true))
            }
            val targetInterfaces = preferredInterfaces.ifEmpty { interfaces.filter { !it.isLoopback && it.isUp } }

            for (iface in targetInterfaces) {
                val addresses = Collections.list(iface.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host != "127.0.0.1" && !host.startsWith("169.254")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to WifiManager if network interfaces iteration fails
        }

        if (context != null) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    return String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                }
            } catch (_: Exception) {
                // Ignore fallback failure
            }
        }

        return "127.0.0.1"
    }

    /**
     * Generates a readable room code representing the host's subnet address.
     * E.g. IP 192.168.1.42 -> "CHESS-1042"
     */
    fun generateRoomCode(ip: String, port: Int = DEFAULT_PORT): String {
        val parts = ip.split(".")
        return if (parts.size == 4) {
            val p2 = parts[2].toIntOrNull() ?: 0
            val p3 = parts[3].toIntOrNull() ?: 0
            val codeNum = (p2 * 1000) + p3
            "CHESS-%04d".format(codeNum)
        } else {
            "CHESS-%04d".format((1000..9999).random())
        }
    }

    /**
     * Resolves a target address from user input.
     * Supports:
     * - Direct IP: "192.168.1.42" or "192.168.1.42:8888"
     * - Subnet room code: "CHESS-1042" or "1042" on local subnet prefix (e.g. 192.168.)
     */
    fun resolveTargetAddress(input: String, localIp: String, defaultPort: Int = DEFAULT_PORT): Pair<String, Int>? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Format: "192.168.1.42:8888" or "192.168.1.42"
        if (trimmed.contains(".")) {
            val parts = trimmed.split(":")
            val ip = parts[0].trim()
            val port = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: defaultPort else defaultPort
            return Pair(ip, port)
        }

        // Format: "CHESS-1042" or "1042"
        val codeDigits = trimmed.removePrefix("CHESS-").removePrefix("chess-").toIntOrNull()
        if (codeDigits != null) {
            val localParts = localIp.split(".")
            if (localParts.size == 4) {
                val p2 = codeDigits / 1000
                val p3 = codeDigits % 1000
                val targetIp = "${localParts[0]}.${localParts[1]}.$p2.$p3"
                return Pair(targetIp, defaultPort)
            }
        }

        return null
    }

    /**
     * Broadcasts UDP beacon on local Wi-Fi to advertise the host to other devices.
     */
    fun startBroadcastingBeacon(
        scope: CoroutineScope,
        roomCode: String,
        port: Int,
        hostName: String,
        localIp: String
    ): Job {
        return scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true

                val payload = JSONObject().apply {
                    put("magic", BEACON_MAGIC)
                    put("code", roomCode)
                    put("ip", localIp)
                    put("port", port)
                    put("host", hostName)
                }.toString().toByteArray(Charsets.UTF_8)

                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(payload, payload.size, broadcastAddr, BEACON_PORT)

                while (isActive) {
                    try {
                        socket.send(packet)
                    } catch (_: Exception) {
                        // Ignore transient UDP send failures
                    }
                    delay(1500L)
                }
            } catch (_: Exception) {
                // Discovery broadcast stopped
            } finally {
                socket?.close()
            }
        }
    }

    /**
     * Listens for local UDP beacons broadcast by hosts on the same Wi-Fi network.
     */
    fun startListeningForBeacons(
        scope: CoroutineScope,
        onHostDiscovered: (DiscoveredHost) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(BEACON_PORT).apply {
                    broadcast = true
                    soTimeout = 2000
                }
                val buffer = ByteArray(1024)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val json = JSONObject(text)
                        if (json.optString("magic") == BEACON_MAGIC) {
                            val ip = json.optString("ip", packet.address.hostAddress ?: "")
                            val port = json.optInt("port", DEFAULT_PORT)
                            val code = json.optString("code", "")
                            val host = json.optString("host", "Grandmaster Host")
                            if (ip.isNotEmpty()) {
                                onHostDiscovered(
                                    DiscoveredHost(
                                        ip = ip,
                                        port = port,
                                        roomCode = code,
                                        hostName = host,
                                        lastSeenMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        // Regular timeout to allow cancellation check
                    } catch (_: Exception) {
                        // Ignore malformed UDP packet
                    }
                }
            } catch (_: Exception) {
                // Discovery listener stopped
            } finally {
                socket?.close()
            }
        }
    }
}
