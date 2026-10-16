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
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class MultiplayerServer(
    private val port: Int,
    private val onClientConnected: (clientAddress: String) -> Unit,
    private val onMessageReceived: (message: MultiplayerMessage) -> Unit,
    private val onClientDisconnected: (reason: String) -> Unit,
    private val onError: (error: Throwable) -> Unit
) {

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val sendMutex = Mutex()
    private var serverJob: Job? = null
    private var isRunning = false

    fun start(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val sSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                serverSocket = sSocket

                while (isActive && isRunning) {
                    try {
                        val client = sSocket.accept()
                        client.tcpNoDelay = true
                        client.soTimeout = 0 // Keep-alive handled by ping/pong
                        clientSocket = client

                        val cReader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                        val cWriter = BufferedWriter(OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8))
                        reader = cReader
                        writer = cWriter

                        onClientConnected(client.inetAddress.hostAddress ?: "Unknown")

                        // Read incoming messages loop
                        while (isActive && !client.isClosed) {
                            val line = cReader.readLine() ?: break
                            if (line.isBlank()) continue

                            val message = MultiplayerMessageSerializer.deserialize(line)
                            if (message != null) {
                                onMessageReceived(message)
                            }
                        }

                        // Client disconnected
                        if (isRunning) {
                            onClientDisconnected("Client closed connection")
                        }
                    } catch (e: SocketException) {
                        if (isRunning) {
                            onClientDisconnected("Socket error: ${e.message}")
                        }
                        break
                    } catch (e: Exception) {
                        if (isRunning) {
                            onError(e)
                        }
                        break
                    } finally {
                        closeClient()
                    }
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

    private fun closeClient() {
        try {
            reader?.close()
            writer?.close()
            clientSocket?.close()
        } catch (_: Exception) {}
        reader = null
        writer = null
        clientSocket = null
    }

    fun stop() {
        isRunning = false
        closeClient()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }
}
