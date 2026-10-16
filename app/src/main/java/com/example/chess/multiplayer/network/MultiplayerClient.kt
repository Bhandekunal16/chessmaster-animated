package com.example.chess.multiplayer.network

import com.example.chess.multiplayer.model.MultiplayerMessage
import com.example.chess.multiplayer.model.MultiplayerMessageSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class MultiplayerClient(
    private val hostIp: String,
    private val port: Int,
    private val onConnected: () -> Unit,
    private val onMessageReceived: (message: MultiplayerMessage) -> Unit,
    private val onDisconnected: (reason: String) -> Unit,
    private val onError: (error: Throwable) -> Unit
) {

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val sendMutex = Mutex()
    private var clientJob: Job? = null
    private var isRunning = false

    fun start(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true

        clientJob = scope.launch(Dispatchers.IO) {
            var s: Socket? = null
            try {
                s = Socket()
                s.tcpNoDelay = true
                // Connect with 5-second timeout
                s.connect(InetSocketAddress(hostIp, port), 5000)
                socket = s

                val cReader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val cWriter = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
                reader = cReader
                writer = cWriter

                onConnected()

                // Read incoming messages
                while (isActive && isRunning && !s.isClosed) {
                    val line = cReader.readLine() ?: break
                    if (line.isBlank()) continue

                    val message = MultiplayerMessageSerializer.deserialize(line)
                    if (message != null) {
                        onMessageReceived(message)
                    }
                }

                if (isRunning) {
                    onDisconnected("Disconnected from host")
                }
            } catch (e: SocketTimeoutException) {
                if (isRunning) {
                    onError(SocketTimeoutException("Connection timed out. Ensure host is running and connected to the same Wi-Fi."))
                }
            } catch (e: Exception) {
                if (isRunning) {
                    onError(e)
                }
            } finally {
                stop()
            }
        }
    }

    suspend fun sendMessage(message: MultiplayerMessage): Boolean {
        return sendMutex.withLock {
            try {
                val w = writer ?: return@withLock false
                val raw = MultiplayerMessageSerializer.serialize(message)
                w.write(raw)
                w.newLine()
                w.flush()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (_: Exception) {}
        reader = null
        writer = null
        socket = null
        clientJob?.cancel()
        clientJob = null
    }
}
