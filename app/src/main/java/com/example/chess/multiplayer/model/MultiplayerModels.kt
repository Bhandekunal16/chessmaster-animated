package com.example.chess.multiplayer.model

import org.json.JSONObject

enum class MultiplayerRole {
    HOST,   // Playing White, authoritative game owner
    CLIENT  // Playing Black, sends move requests to Host
}

enum class MultiplayerStatus {
    IDLE,
    HOSTING_LISTENING,
    CLIENT_CONNECTING,
    CONNECTED,
    GAME_IN_PROGRESS,
    RECONNECTING,
    DISCONNECTED,
    ERROR
}

enum class MultiplayerMessageType {
    JOIN_REQUEST,
    JOIN_ACCEPTED,
    JOIN_REJECTED,
    GAME_START,
    MOVE,
    MOVE_REJECTED,
    GAME_STATE,
    OFFER_DRAW,
    ACCEPT_DRAW,
    REJECT_DRAW,
    RESIGN,
    DISCONNECT,
    PING,
    PONG
}

data class MultiplayerMessage(
    val version: Int = PROTOCOL_VERSION,
    val sessionId: String,
    val type: MultiplayerMessageType,
    val sequenceNumber: Long = 0L,
    val payload: String = ""
) {
    companion object {
        const val PROTOCOL_VERSION = 1
        const val MAX_MESSAGE_SIZE = 65536 // 64 KB safety limit
    }
}

object MultiplayerMessageSerializer {

    fun serialize(msg: MultiplayerMessage): String {
        val json = JSONObject()
        json.put("v", msg.version)
        json.put("sid", msg.sessionId)
        json.put("t", msg.type.name)
        json.put("seq", msg.sequenceNumber)
        json.put("p", msg.payload)
        return json.toString()
    }

    fun deserialize(raw: String): MultiplayerMessage? {
        if (raw.length > MultiplayerMessage.MAX_MESSAGE_SIZE) {
            return null
        }
        return try {
            val json = JSONObject(raw)
            val version = json.optInt("v", 1)
            val sessionId = json.optString("sid", "")
            val typeStr = json.optString("t", "")
            val type = MultiplayerMessageType.valueOf(typeStr)
            val seq = json.optLong("seq", 0L)
            val payload = json.optString("p", "")
            MultiplayerMessage(
                version = version,
                sessionId = sessionId,
                type = type,
                sequenceNumber = seq,
                payload = payload
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class DiscoveredHost(
    val ip: String,
    val port: Int,
    val roomCode: String,
    val hostName: String,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class MultiplayerSessionState(
    val status: MultiplayerStatus = MultiplayerStatus.IDLE,
    val role: MultiplayerRole = MultiplayerRole.HOST,
    val sessionId: String = "",
    val roomCode: String = "",
    val localIp: String = "",
    val hostIp: String = "",
    val port: Int = 8888,
    val opponentName: String = "",
    val latencyMs: Long = 0L,
    val drawOffer: DrawOfferState = DrawOfferState.NONE,
    val errorMessage: String? = null
)

enum class DrawOfferState {
    NONE,
    SENT,
    RECEIVED
}
