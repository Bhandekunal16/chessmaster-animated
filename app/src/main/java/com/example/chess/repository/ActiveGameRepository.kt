package com.example.chess.repository

import android.content.SharedPreferences
import com.example.chess.engine.ChessEngine
import com.example.chess.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Compact serializable representation of a move for process death survival.
 */
data class SerializedMove(
    val fromFile: Int,
    val fromRank: Int,
    val toFile: Int,
    val toRank: Int,
    val promotionPiece: PieceType? = null
)

/**
 * Versioned snapshot representing an active chess game across process recreations.
 * Presentation-only artifacts (animations, sounds, highlights, AI job scopes) are strictly excluded.
 */
data class ActiveGameSnapshot(
    val schemaVersion: Int = 1,
    val saveSequence: Long = 0L,
    val mode: GameMode = GameMode.PVC,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val playerColor: PieceColor = PieceColor.WHITE,
    val timeControl: TimeControl = TimeControl.UNLIMITED,
    val whiteTimeSeconds: Int = 0,
    val blackTimeSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val moveHistory: List<SerializedMove> = emptyList(),
    val redoHistory: List<SerializedMove> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Robust JSON serializer and game state reconstructor for active game snapshots.
 */
object GameSnapshotSerializer {

    const val CURRENT_SCHEMA_VERSION = 1

    fun serialize(snapshot: ActiveGameSnapshot): String {
        val root = JSONObject()
        root.put("schemaVersion", snapshot.schemaVersion)
        root.put("saveSequence", snapshot.saveSequence)
        root.put("mode", snapshot.mode.name)
        root.put("aiDifficulty", snapshot.aiDifficulty.name)
        root.put("playerColor", snapshot.playerColor.name)
        root.put("timeControl", snapshot.timeControl.name)
        root.put("whiteTimeSeconds", snapshot.whiteTimeSeconds)
        root.put("blackTimeSeconds", snapshot.blackTimeSeconds)
        root.put("isTimerRunning", snapshot.isTimerRunning)
        root.put("timestamp", snapshot.timestamp)

        val movesArr = JSONArray()
        for (m in snapshot.moveHistory) {
            val obj = JSONObject()
            obj.put("ff", m.fromFile)
            obj.put("fr", m.fromRank)
            obj.put("tf", m.toFile)
            obj.put("tr", m.toRank)
            if (m.promotionPiece != null) {
                obj.put("promo", m.promotionPiece.name)
            }
            movesArr.put(obj)
        }
        root.put("moves", movesArr)

        val redosArr = JSONArray()
        for (m in snapshot.redoHistory) {
            val obj = JSONObject()
            obj.put("ff", m.fromFile)
            obj.put("fr", m.fromRank)
            obj.put("tf", m.toFile)
            obj.put("tr", m.toRank)
            if (m.promotionPiece != null) {
                obj.put("promo", m.promotionPiece.name)
            }
            redosArr.put(obj)
        }
        root.put("redos", redosArr)

        return root.toString()
    }

    fun deserialize(rawJson: String): ActiveGameSnapshot? {
        return runCatching {
            val root = JSONObject(rawJson)
            val schemaVer = root.optInt("schemaVersion", 0)
            if (schemaVer != CURRENT_SCHEMA_VERSION) {
                // Outdated or unsupported schema version: reject safely
                return null
            }

            val seq = root.optLong("saveSequence", 0L)
            val modeStr = root.optString("mode", GameMode.PVC.name)
            val diffStr = root.optString("aiDifficulty", AIDifficulty.MEDIUM.name)
            val colorStr = root.optString("playerColor", PieceColor.WHITE.name)
            val tcStr = root.optString("timeControl", TimeControl.UNLIMITED.name)
            val whiteSec = root.optInt("whiteTimeSeconds", 0)
            val blackSec = root.optInt("blackTimeSeconds", 0)
            val timerRunning = root.optBoolean("isTimerRunning", false)
            val ts = root.optLong("timestamp", System.currentTimeMillis())

            val mode = GameMode.entries.find { it.name == modeStr } ?: GameMode.PVC
            val diff = AIDifficulty.entries.find { it.name == diffStr } ?: AIDifficulty.MEDIUM
            val color = PieceColor.entries.find { it.name == colorStr } ?: PieceColor.WHITE
            val tc = TimeControl.entries.find { it.name == tcStr } ?: TimeControl.UNLIMITED

            val moves = mutableListOf<SerializedMove>()
            val movesArr = root.optJSONArray("moves")
            if (movesArr != null) {
                for (i in 0 until movesArr.length()) {
                    val mObj = movesArr.getJSONObject(i)
                    val promo = if (mObj.has("promo")) {
                        PieceType.entries.find { it.name == mObj.getString("promo") }
                    } else null
                    moves.add(
                        SerializedMove(
                            fromFile = mObj.getInt("ff"),
                            fromRank = mObj.getInt("fr"),
                            toFile = mObj.getInt("tf"),
                            toRank = mObj.getInt("tr"),
                            promotionPiece = promo
                        )
                    )
                }
            }

            val redos = mutableListOf<SerializedMove>()
            val redosArr = root.optJSONArray("redos")
            if (redosArr != null) {
                for (i in 0 until redosArr.length()) {
                    val mObj = redosArr.getJSONObject(i)
                    val promo = if (mObj.has("promo")) {
                        PieceType.entries.find { it.name == mObj.getString("promo") }
                    } else null
                    redos.add(
                        SerializedMove(
                            fromFile = mObj.getInt("ff"),
                            fromRank = mObj.getInt("fr"),
                            toFile = mObj.getInt("tf"),
                            toRank = mObj.getInt("tr"),
                            promotionPiece = promo
                        )
                    )
                }
            }

            ActiveGameSnapshot(
                schemaVersion = schemaVer,
                saveSequence = seq,
                mode = mode,
                aiDifficulty = diff,
                playerColor = color,
                timeControl = tc,
                whiteTimeSeconds = whiteSec,
                blackTimeSeconds = blackSec,
                isTimerRunning = timerRunning,
                moveHistory = moves,
                redoHistory = redos,
                timestamp = ts
            )
        }.getOrNull()
    }

    /**
     * Reconstructs authoritative [GameState] by replaying moves from the initial board state.
     * Validates that every move is legal. Returns null if any move is corrupted or illegal.
     */
    fun reconstructGameState(snapshot: ActiveGameSnapshot): GameState? {
        return runCatching {
            var state = ChessEngine.createInitialState()

            for (m in snapshot.moveHistory) {
                val from = Square(m.fromFile, m.fromRank)
                val to = Square(m.toFile, m.toRank)
                val legals = ChessEngine.getLegalMoves(state, from)
                val matchingMove = legals.find {
                    it.to == to && (m.promotionPiece == null || it.promotionPiece == m.promotionPiece)
                } ?: return null // Corrupted move: fail safe

                state = ChessEngine.applyMove(state, matchingMove)
            }

            // Reconstruct redo moves if legal from current state
            val redos = mutableListOf<Move>()
            var redoSim = state
            for (m in snapshot.redoHistory) {
                val from = Square(m.fromFile, m.fromRank)
                val to = Square(m.toFile, m.toRank)
                val legals = ChessEngine.getLegalMoves(redoSim, from)
                val matchingMove = legals.find {
                    it.to == to && (m.promotionPiece == null || it.promotionPiece == m.promotionPiece)
                } ?: break
                redos.add(matchingMove)
                redoSim = ChessEngine.applyMove(redoSim, matchingMove)
            }

            // Clocks are restored with elapsed time paused to prevent unfair timeouts during process death
            state.copy(
                redoHistory = redos,
                whiteTimeSeconds = snapshot.whiteTimeSeconds,
                blackTimeSeconds = snapshot.blackTimeSeconds,
                isTimerRunning = false
            )
        }.getOrNull()
    }
}

/**
 * Dedicated persistence contract for active game sessions.
 */
interface ActiveGameRepository {
    suspend fun saveActiveGame(snapshot: ActiveGameSnapshot)
    suspend fun loadActiveGame(): ActiveGameSnapshot?
    suspend fun clearActiveGame()
}

/**
 * Concrete SharedPreferences-backed active game persistence.
 * Prevents race conditions and out-of-order writes using monotonic sequence checks.
 */
class SharedPreferencesActiveGameRepository(
    private val prefs: SharedPreferences
) : ActiveGameRepository {

    companion object {
        const val PREFS_NAME = "grandmaster_chess_active_game_prefs"
        const val KEY_SNAPSHOT = "active_game_snapshot"
        const val KEY_SEQUENCE = "active_game_sequence"
    }

    @Volatile
    private var lastSavedSequence: Long = prefs.getLong(KEY_SEQUENCE, 0L)

    override suspend fun saveActiveGame(snapshot: ActiveGameSnapshot) {
        synchronized(this) {
            // Guard against stale async writes overwriting newer snapshots
            if (snapshot.saveSequence < lastSavedSequence) {
                return
            }
            lastSavedSequence = snapshot.saveSequence
            val json = GameSnapshotSerializer.serialize(snapshot)
            prefs.edit()
                .putString(KEY_SNAPSHOT, json)
                .putLong(KEY_SEQUENCE, snapshot.saveSequence)
                .apply()
        }
    }

    override suspend fun loadActiveGame(): ActiveGameSnapshot? {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return GameSnapshotSerializer.deserialize(raw)
    }

    override suspend fun clearActiveGame() {
        synchronized(this) {
            lastSavedSequence = 0L
            prefs.edit()
                .remove(KEY_SNAPSHOT)
                .remove(KEY_SEQUENCE)
                .apply()
        }
    }
}

/**
 * In-memory test fake implementation of [ActiveGameRepository].
 */
class FakeActiveGameRepository : ActiveGameRepository {
    private var snapshot: ActiveGameSnapshot? = null
    private var lastSequence: Long = 0L

    override suspend fun saveActiveGame(snapshot: ActiveGameSnapshot) {
        if (snapshot.saveSequence >= lastSequence) {
            lastSequence = snapshot.saveSequence
            this.snapshot = snapshot
        }
    }

    override suspend fun loadActiveGame(): ActiveGameSnapshot? = snapshot

    override suspend fun clearActiveGame() {
        snapshot = null
        lastSequence = 0L
    }
}
