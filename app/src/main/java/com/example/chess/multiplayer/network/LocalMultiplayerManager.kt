package com.example.chess.multiplayer.network

import android.content.Context
import com.example.chess.model.PieceType
import com.example.chess.multiplayer.discovery.NetworkDiscoveryHelper
import com.example.chess.multiplayer.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

class LocalMultiplayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val _sessionState = MutableStateFlow(MultiplayerSessionState())
    val sessionState: StateFlow<MultiplayerSessionState> = _sessionState.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private var server: MultiplayerServer? = null
    private var client: MultiplayerClient? = null
    private var beaconBroadcastJob: Job? = null
    private var beaconListenJob: Job? = null
    private var heartbeatJob: Job? = null

    private var currentSequence = 0L

    // Callbacks for ChessViewModel integration
    var onOpponentMoveReceived: ((fromFile: Int, fromRank: Int, toFile: Int, toRank: Int, promo: PieceType?) -> Unit)? = null
    var onGameStateSyncReceived: ((snapshotJson: String) -> Unit)? = null
    var onDrawOfferedReceived: (() -> Unit)? = null
    var onDrawAcceptedReceived: (() -> Unit)? = null
    var onDrawRejectedReceived: (() -> Unit)? = null
    var onOpponentResignedReceived: (() -> Unit)? = null
    var onOpponentDisconnectedReceived: ((reason: String) -> Unit)? = null

    init {
        val ip = NetworkDiscoveryHelper.getLocalIpAddress(context)
        _sessionState.value = _sessionState.value.copy(localIp = ip)
    }

    // ==================== Discovery ====================

    fun startDiscovery() {
        stopDiscovery()
        _discoveredHosts.value = emptyList()
        beaconListenJob = NetworkDiscoveryHelper.startListeningForBeacons(scope) { host ->
            val current = _discoveredHosts.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.ip == host.ip && it.port == host.port }
            if (existingIndex >= 0) {
                current[existingIndex] = host
            } else {
                current.add(host)
            }
            _discoveredHosts.value = current
        }
    }

    fun stopDiscovery() {
        beaconListenJob?.cancel()
        beaconListenJob = null
    }

    // ==================== Host Session ====================

    fun startHosting(
        port: Int = NetworkDiscoveryHelper.DEFAULT_PORT,
        hostName: String = "White Host",
        initialSnapshotJson: String = ""
    ) {
        disconnect()

        val localIp = NetworkDiscoveryHelper.getLocalIpAddress(context)
        val roomCode = NetworkDiscoveryHelper.generateRoomCode(localIp, port)
        val sessionId = UUID.randomUUID().toString()

        _sessionState.value = MultiplayerSessionState(
            status = MultiplayerStatus.HOSTING_LISTENING,
            role = MultiplayerRole.HOST,
            sessionId = sessionId,
            roomCode = roomCode,
            localIp = localIp,
            hostIp = localIp,
            port = port
        )

        server = MultiplayerServer(
            port = port,
            onClientConnected = { clientIp ->
                scope.launch {
                    _sessionState.value = _sessionState.value.copy(
                        status = MultiplayerStatus.CONNECTED,
                        opponentName = "Guest ($clientIp)"
                    )
                }
            },
            onMessageReceived = { msg ->
                handleIncomingMessage(msg, isServer = true, initialSnapshotJson = initialSnapshotJson)
            },
            onClientDisconnected = { reason ->
                scope.launch {
                    _sessionState.value = _sessionState.value.copy(
                        status = MultiplayerStatus.DISCONNECTED,
                        errorMessage = "Guest disconnected: $reason"
                    )
                    onOpponentDisconnectedReceived?.invoke(reason)
                }
            },
            onError = { error ->
                scope.launch {
                    _sessionState.value = _sessionState.value.copy(
                        status = MultiplayerStatus.ERROR,
                        errorMessage = error.localizedMessage ?: "Server socket error"
                    )
                }
            }
        ).also { it.start(scope) }

        // Start broadcasting beacon
        beaconBroadcastJob = NetworkDiscoveryHelper.startBroadcastingBeacon(
            scope = scope,
            roomCode = roomCode,
            port = port,
            hostName = hostName,
            localIp = localIp
        )

        startHeartbeat()
    }

    // ==================== Client Session ====================

    fun joinGame(
        targetAddressInput: String,
        clientName: String = "Black Guest"
    ) {
        disconnect()

        val localIp = NetworkDiscoveryHelper.getLocalIpAddress(context)
        val target = NetworkDiscoveryHelper.resolveTargetAddress(targetAddressInput, localIp)
        if (target == null) {
            _sessionState.value = _sessionState.value.copy(
                status = MultiplayerStatus.ERROR,
                errorMessage = "Invalid IP address or room code: '$targetAddressInput'"
            )
            return
        }

        val (hostIp, port) = target

        _sessionState.value = MultiplayerSessionState(
            status = MultiplayerStatus.CLIENT_CONNECTING,
            role = MultiplayerRole.CLIENT,
            localIp = localIp,
            hostIp = hostIp,
            port = port,
            roomCode = targetAddressInput
        )

        client = MultiplayerClient(
            hostIp = hostIp,
            port = port,
            onConnected = {
                scope.launch {
                    _sessionState.value = _sessionState.value.copy(
                        status = MultiplayerStatus.CONNECTED
                    )
                    // Send JOIN_REQUEST
                    val joinPayload = JSONObject().apply {
                        put("playerName", clientName)
                        put("clientVersion", MultiplayerMessage.PROTOCOL_VERSION)
                    }.toString()

                    client?.sendMessage(
                        MultiplayerMessage(
                            sessionId = "",
                            type = MultiplayerMessageType.JOIN_REQUEST,
                            payload = joinPayload
                        )
                    )
                }
            },
            onMessageReceived = { msg ->
                handleIncomingMessage(msg, isServer = false)
            },
            onDisconnected = { reason ->
                scope.launch {
                    _sessionState.value = _sessionState.value.copy(
                        status = MultiplayerStatus.DISCONNECTED,
                        errorMessage = reason
                    )
                    onOpponentDisconnectedReceived?.invoke(reason)
                }
            },
            onError = { error ->
                scope.launch {
                    _sessionState.value = _sessionState.value.copy(
                        status = MultiplayerStatus.ERROR,
                        errorMessage = error.localizedMessage ?: "Failed to connect to $hostIp:$port"
                    )
                }
            }
        ).also { it.start(scope) }

        startHeartbeat()
    }

    // ==================== Message Dispatching ====================

    private fun handleIncomingMessage(
        message: MultiplayerMessage,
        isServer: Boolean,
        initialSnapshotJson: String = ""
    ) {
        scope.launch {
            when (message.type) {
                MultiplayerMessageType.JOIN_REQUEST -> {
                    if (isServer) {
                        val clientJson = try { JSONObject(message.payload) } catch (_: Exception) { JSONObject() }
                        val clientName = clientJson.optString("playerName", "Black Player")
                        val currentSession = _sessionState.value

                        _sessionState.value = currentSession.copy(
                            status = MultiplayerStatus.GAME_IN_PROGRESS,
                            opponentName = clientName
                        )

                        // Respond with JOIN_ACCEPTED containing session token & initial state
                        val acceptPayload = JSONObject().apply {
                            put("sessionId", currentSession.sessionId)
                            put("assignedColor", "BLACK")
                            put("initialState", initialSnapshotJson)
                        }.toString()

                        server?.sendMessage(
                            MultiplayerMessage(
                                sessionId = currentSession.sessionId,
                                type = MultiplayerMessageType.JOIN_ACCEPTED,
                                payload = acceptPayload
                            )
                        )
                    }
                }

                MultiplayerMessageType.JOIN_ACCEPTED -> {
                    if (!isServer) {
                        val payloadJson = try { JSONObject(message.payload) } catch (_: Exception) { JSONObject() }
                        val sid = payloadJson.optString("sessionId", message.sessionId)
                        val initialState = payloadJson.optString("initialState", "")

                        _sessionState.value = _sessionState.value.copy(
                            status = MultiplayerStatus.GAME_IN_PROGRESS,
                            sessionId = sid,
                            opponentName = "Host (White)"
                        )

                        if (initialState.isNotEmpty()) {
                            onGameStateSyncReceived?.invoke(initialState)
                        }
                    }
                }

                MultiplayerMessageType.JOIN_REJECTED -> {
                    if (!isServer) {
                        _sessionState.value = _sessionState.value.copy(
                            status = MultiplayerStatus.ERROR,
                            errorMessage = message.payload.ifEmpty { "Host rejected join request" }
                        )
                    }
                }

                MultiplayerMessageType.MOVE -> {
                    try {
                        val moveJson = JSONObject(message.payload)
                        val ff = moveJson.getInt("ff")
                        val fr = moveJson.getInt("fr")
                        val tf = moveJson.getInt("tf")
                        val tr = moveJson.getInt("tr")
                        val promoStr = moveJson.optString("promo", "")
                        val promo = if (promoStr.isNotEmpty()) PieceType.valueOf(promoStr) else null

                        onOpponentMoveReceived?.invoke(ff, fr, tf, tr, promo)
                    } catch (_: Exception) {
                        // Ignore malformed move payload
                    }
                }

                MultiplayerMessageType.GAME_STATE -> {
                    if (!isServer && message.payload.isNotEmpty()) {
                        onGameStateSyncReceived?.invoke(message.payload)
                    }
                }

                MultiplayerMessageType.OFFER_DRAW -> {
                    _sessionState.value = _sessionState.value.copy(drawOffer = DrawOfferState.RECEIVED)
                    onDrawOfferedReceived?.invoke()
                }

                MultiplayerMessageType.ACCEPT_DRAW -> {
                    _sessionState.value = _sessionState.value.copy(drawOffer = DrawOfferState.NONE)
                    onDrawAcceptedReceived?.invoke()
                }

                MultiplayerMessageType.REJECT_DRAW -> {
                    _sessionState.value = _sessionState.value.copy(drawOffer = DrawOfferState.NONE)
                    onDrawRejectedReceived?.invoke()
                }

                MultiplayerMessageType.RESIGN -> {
                    onOpponentResignedReceived?.invoke()
                }

                MultiplayerMessageType.DISCONNECT -> {
                    _sessionState.value = _sessionState.value.copy(
                        status = MultiplayerStatus.DISCONNECTED,
                        errorMessage = "Opponent left the match"
                    )
                    onOpponentDisconnectedReceived?.invoke("Opponent left the match")
                }

                MultiplayerMessageType.PING -> {
                    val echo = MultiplayerMessage(
                        sessionId = message.sessionId,
                        type = MultiplayerMessageType.PONG,
                        payload = message.payload
                    )
                    if (isServer) server?.sendMessage(echo) else client?.sendMessage(echo)
                }

                MultiplayerMessageType.PONG -> {
                    val sentTime = message.payload.toLongOrNull() ?: 0L
                    if (sentTime > 0) {
                        val lat = (System.currentTimeMillis() - sentTime).coerceAtLeast(0L)
                        _sessionState.value = _sessionState.value.copy(latencyMs = lat)
                    }
                }

                else -> {}
            }
        }
    }

    // ==================== Action Transmissions ====================

    fun sendMove(
        fromFile: Int,
        fromRank: Int,
        toFile: Int,
        toRank: Int,
        promotionPiece: PieceType? = null
    ) {
        val payload = JSONObject().apply {
            put("ff", fromFile)
            put("fr", fromRank)
            put("tf", toFile)
            put("tr", toRank)
            if (promotionPiece != null) {
                put("promo", promotionPiece.name)
            }
        }.toString()

        val msg = MultiplayerMessage(
            sessionId = _sessionState.value.sessionId,
            type = MultiplayerMessageType.MOVE,
            sequenceNumber = ++currentSequence,
            payload = payload
        )

        scope.launch {
            if (_sessionState.value.role == MultiplayerRole.HOST) {
                server?.sendMessage(msg)
            } else {
                client?.sendMessage(msg)
            }
        }
    }

    fun broadcastAuthoritativeState(snapshotJson: String) {
        if (_sessionState.value.role != MultiplayerRole.HOST) return
        val msg = MultiplayerMessage(
            sessionId = _sessionState.value.sessionId,
            type = MultiplayerMessageType.GAME_STATE,
            sequenceNumber = ++currentSequence,
            payload = snapshotJson
        )
        scope.launch {
            server?.sendMessage(msg)
        }
    }

    fun offerDraw() {
        _sessionState.value = _sessionState.value.copy(drawOffer = DrawOfferState.SENT)
        val msg = MultiplayerMessage(
            sessionId = _sessionState.value.sessionId,
            type = MultiplayerMessageType.OFFER_DRAW,
            sequenceNumber = ++currentSequence
        )
        scope.launch {
            if (_sessionState.value.role == MultiplayerRole.HOST) {
                server?.sendMessage(msg)
            } else {
                client?.sendMessage(msg)
            }
        }
    }

    fun acceptDraw() {
        _sessionState.value = _sessionState.value.copy(drawOffer = DrawOfferState.NONE)
        val msg = MultiplayerMessage(
            sessionId = _sessionState.value.sessionId,
            type = MultiplayerMessageType.ACCEPT_DRAW,
            sequenceNumber = ++currentSequence
        )
        scope.launch {
            if (_sessionState.value.role == MultiplayerRole.HOST) {
                server?.sendMessage(msg)
            } else {
                client?.sendMessage(msg)
            }
        }
    }

    fun rejectDraw() {
        _sessionState.value = _sessionState.value.copy(drawOffer = DrawOfferState.NONE)
        val msg = MultiplayerMessage(
            sessionId = _sessionState.value.sessionId,
            type = MultiplayerMessageType.REJECT_DRAW,
            sequenceNumber = ++currentSequence
        )
        scope.launch {
            if (_sessionState.value.role == MultiplayerRole.HOST) {
                server?.sendMessage(msg)
            } else {
                client?.sendMessage(msg)
            }
        }
    }

    fun resign() {
        val msg = MultiplayerMessage(
            sessionId = _sessionState.value.sessionId,
            type = MultiplayerMessageType.RESIGN,
            sequenceNumber = ++currentSequence
        )
        scope.launch {
            if (_sessionState.value.role == MultiplayerRole.HOST) {
                server?.sendMessage(msg)
            } else {
                client?.sendMessage(msg)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(3000L)
                val pingMsg = MultiplayerMessage(
                    sessionId = _sessionState.value.sessionId,
                    type = MultiplayerMessageType.PING,
                    payload = System.currentTimeMillis().toString()
                )
                if (_sessionState.value.role == MultiplayerRole.HOST) {
                    server?.sendMessage(pingMsg)
                } else {
                    client?.sendMessage(pingMsg)
                }
            }
        }
    }

    fun disconnect() {
        stopDiscovery()
        heartbeatJob?.cancel()
        heartbeatJob = null
        beaconBroadcastJob?.cancel()
        beaconBroadcastJob = null

        val disconnectMsg = MultiplayerMessage(
            sessionId = _sessionState.value.sessionId,
            type = MultiplayerMessageType.DISCONNECT
        )

        runCatching {
            runBlocking {
                withTimeoutOrNull(300L) {
                    if (_sessionState.value.role == MultiplayerRole.HOST) {
                        server?.sendMessage(disconnectMsg)
                    } else {
                        client?.sendMessage(disconnectMsg)
                    }
                }
            }
        }

        server?.stop()
        server = null
        client?.stop()
        client = null

        _sessionState.value = MultiplayerSessionState(
            status = MultiplayerStatus.IDLE,
            localIp = NetworkDiscoveryHelper.getLocalIpAddress(context)
        )
    }
}
