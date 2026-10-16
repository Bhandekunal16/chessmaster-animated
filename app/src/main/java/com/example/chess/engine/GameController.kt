package com.example.chess.engine

import com.example.chess.model.*
import com.example.chess.repository.ActiveGameSnapshot
import com.example.chess.repository.SerializedMove
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Orchestrates game-level operations, turn transitions, AI computations,
 * moves execution, undo/redo history, PGN export, and integration with the [GameClock].
 */
class GameController(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    clock: GameClock? = null
) {

    private val _gameState = MutableStateFlow(ChessEngine.createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    val gameClock: GameClock = clock ?: GameClock(
        scope = scope,
        onTick = { whiteSec, blackSec ->
            _gameState.update { it.copy(whiteTimeSeconds = whiteSec, blackTimeSeconds = blackSec) }
        },
        onTimeout = { timedOutColor ->
            handleTimeExpired(timedOutColor)
        }
    )

    private var aiJob: Job? = null
    // Incremented whenever the game state is restarted, moved, or undone to reject stale AI results
    private var gameGeneration: Long = 0L

    /**
     * Attempts to apply the specified [move] to the current board.
     * Validates that the move is legal and that AI is not currently thinking.
     *
     * @return true if the move was legal and applied, false otherwise.
     */
    fun applyMove(move: Move): Boolean {
        // Reject user moves while AI is thinking to avoid race conditions and conflicting moves
        if (_isThinking.value) return false

        val currentState = _gameState.value
        if (currentState.isGameOver) return false

        val legalMoves = ChessEngine.getLegalMoves(currentState, move.from)
        val isLegal = legalMoves.any { it.to == move.to && (move.promotionPiece == null || it.promotionPiece == move.promotionPiece) }
        if (!isLegal) {
            return false
        }

        // Advance game generation to invalidate any in-flight AI calculations
        gameGeneration++
        cancelAiJob()

        val nextState = ChessEngine.applyMove(currentState, move)

        // Sync clock turn
        val incSeconds = gameClock.currentTimeControl.incrementSeconds
        gameClock.switchTurn(nextState.activeColor, incSeconds)

        if (nextState.isGameOver) {
            gameClock.stop()
        }

        _gameState.value = nextState.copy(
            whiteTimeSeconds = gameClock.clockState.value.whiteTimeSeconds,
            blackTimeSeconds = gameClock.clockState.value.blackTimeSeconds,
            isTimerRunning = gameClock.clockState.value.isRunning
        )

        return true
    }

    /**
     * Requests an AI move computation for [difficulty].
     *
     * @param delayMs Delay before AI search starts (for human pacing).
     * @param onMoveReady Optional callback when the move is found. If null, the move is applied immediately.
     */
    fun triggerAIMove(
        difficulty: AIDifficulty,
        delayMs: Long = 0L,
        onMoveReady: ((Move) -> Unit)? = null
    ): Job {
        cancelAiJob()
        val currentGen = gameGeneration
        val snapshot = _gameState.value

        if (snapshot.isGameOver) {
            _isThinking.value = false
            return Job().apply { complete() }
        }

        _isThinking.value = true
        val job = scope.launch {
            try {
                if (delayMs > 0) {
                    delay(delayMs)
                }

                if (!isActive || currentGen != gameGeneration || _gameState.value != snapshot) {
                    return@launch
                }

                val bestMove = withContext(computeDispatcher) {
                    ChessAI.findBestMove(snapshot, difficulty)
                }

                // Check again for cancellation or state change (stale results)
                if (!isActive || currentGen != gameGeneration || _gameState.value != snapshot) {
                    return@launch
                }

                _isThinking.value = false

                if (bestMove != null) {
                    // Validate move legality against current board before applying
                    val legalMoves = ChessEngine.getLegalMoves(_gameState.value, bestMove.from)
                    val isStillLegal = legalMoves.any { 
                        it.to == bestMove.to && (bestMove.promotionPiece == null || it.promotionPiece == bestMove.promotionPiece) 
                    }
                    if (isStillLegal) {
                        if (onMoveReady != null) {
                            onMoveReady(bestMove)
                        } else {
                            applyMove(bestMove)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Cancellation is normal when game state changes or search is invalidated; rethrow
                throw e
            } catch (e: Throwable) {
                // Unexpected search failure: safely log or handle
            } finally {
                if (currentGen == gameGeneration) {
                    _isThinking.value = false
                }
            }
        }
        aiJob = job
        return job
    }

    /**
     * Undoes the last move(s). In PVC mode, undos 2 moves (player + AI) to return to player turn.
     */
    fun undoMove(mode: GameMode = GameMode.PVC): Boolean {
        cancelAiJob()
        gameGeneration++

        val history = _gameState.value.moveHistory
        if (history.isEmpty()) return false

        val undoCount = if (mode == GameMode.PVC && history.size >= 2) 2 else 1
        val targetSize = history.size - undoCount

        var reconstructed = ChessEngine.createInitialState()
        for (i in 0 until targetSize) {
            reconstructed = ChessEngine.applyMove(reconstructed, history[i])
        }

        gameClock.switchTurn(reconstructed.activeColor)

        _gameState.value = reconstructed.copy(
            redoHistory = history.takeLast(undoCount),
            whiteTimeSeconds = gameClock.clockState.value.whiteTimeSeconds,
            blackTimeSeconds = gameClock.clockState.value.blackTimeSeconds,
            isTimerRunning = gameClock.clockState.value.isRunning
        )
        return true
    }

    /**
     * Redoes the last undone move if available.
     */
    fun redoMove(): Boolean {
        cancelAiJob()
        gameGeneration++

        val redos = _gameState.value.redoHistory
        if (redos.isEmpty()) return false

        val move = redos.first()
        val next = ChessEngine.applyMove(_gameState.value, move)
        gameClock.switchTurn(next.activeColor)

        _gameState.value = next.copy(
            redoHistory = redos.drop(1),
            whiteTimeSeconds = gameClock.clockState.value.whiteTimeSeconds,
            blackTimeSeconds = gameClock.clockState.value.blackTimeSeconds,
            isTimerRunning = gameClock.clockState.value.isRunning
        )
        return true
    }

    /**
     * Restarts the game to initial board layout and resets timers.
     */
    fun restartGame(timeControl: TimeControl = TimeControl.UNLIMITED) {
        cancelAiJob()
        gameGeneration++
        _isThinking.value = false

        gameClock.reset(timeControl, PieceColor.WHITE)

        val initial = ChessEngine.createInitialState()
        val totalSec = timeControl.totalSeconds
        _gameState.value = initial.copy(
            whiteTimeSeconds = totalSec,
            blackTimeSeconds = totalSec,
            isTimerRunning = totalSec > 0
        )
    }

    /**
     * Restores authoritative [GameState] from persistent storage after process recreation.
     * Cancels existing jobs, resets timers safely, and pauses the clock.
     */
    fun restoreGame(state: GameState, timeControl: TimeControl = TimeControl.UNLIMITED) {
        cancelAiJob()
        gameGeneration++
        _isThinking.value = false

        if (timeControl != TimeControl.UNLIMITED && timeControl.totalSeconds > 0) {
            gameClock.start(
                timeControl = timeControl,
                initialColor = state.activeColor,
                whiteSeconds = state.whiteTimeSeconds,
                blackSeconds = state.blackTimeSeconds
            )
            gameClock.pause()
        } else {
            gameClock.stop()
        }

        _gameState.value = state.copy(
            isTimerRunning = false
        )
    }

    /**
     * Creates an [ActiveGameSnapshot] of the current authoritative game state.
     */
    fun createSnapshot(
        sequence: Long,
        mode: GameMode,
        aiDifficulty: AIDifficulty,
        playerColor: PieceColor,
        timeControl: TimeControl
    ): ActiveGameSnapshot {
        val state = _gameState.value
        return ActiveGameSnapshot(
            schemaVersion = 1,
            saveSequence = sequence,
            mode = mode,
            aiDifficulty = aiDifficulty,
            playerColor = playerColor,
            timeControl = timeControl,
            whiteTimeSeconds = state.whiteTimeSeconds,
            blackTimeSeconds = state.blackTimeSeconds,
            isTimerRunning = state.isTimerRunning,
            moveHistory = state.moveHistory.map { m ->
                SerializedMove(
                    fromFile = m.from.file,
                    fromRank = m.from.rank,
                    toFile = m.to.file,
                    toRank = m.to.rank,
                    promotionPiece = m.promotionPiece
                )
            },
            redoHistory = state.redoHistory.map { m ->
                SerializedMove(
                    fromFile = m.from.file,
                    fromRank = m.from.rank,
                    toFile = m.to.file,
                    toRank = m.to.rank,
                    promotionPiece = m.promotionPiece
                )
            },
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Computes a hint for the current board state asynchronously.
     */
    fun getHint(
        difficulty: AIDifficulty = AIDifficulty.HARD,
        onHintFound: (Move?) -> Unit
    ): Job {
        if (_isThinking.value || _gameState.value.isGameOver) {
            return Job().apply { complete() }
        }

        val snapshot = _gameState.value
        val currentGen = gameGeneration

        return scope.launch {
            _isThinking.value = true
            try {
                val bestMove = withContext(computeDispatcher) {
                    ChessAI.findBestMove(snapshot, difficulty)
                }
                if (isActive && currentGen == gameGeneration && _gameState.value == snapshot) {
                    onHintFound(bestMove)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Ignore or handle
            } finally {
                if (currentGen == gameGeneration) {
                    _isThinking.value = false
                }
            }
        }
    }

    /**
     * Calculates the material advantage for the given [forColor].
     */
    fun calculateMaterialAdvantage(forColor: PieceColor): Int {
        val whiteScore = _gameState.value.capturedBlack.sumOf { it.type.value }
        val blackScore = _gameState.value.capturedWhite.sumOf { it.type.value }
        val diff = if (forColor == PieceColor.WHITE) whiteScore - blackScore else blackScore - whiteScore
        return if (diff > 0) diff / 100 else 0
    }

    /**
     * Exports the game in standard PGN format.
     */
    fun exportPgn(whitePlayer: String, blackPlayer: String): String {
        return ChessEngine.exportPgn(
            state = _gameState.value,
            whitePlayer = whitePlayer,
            blackPlayer = blackPlayer
        )
    }

    /**
     * Returns legal moves for the given [square], or all legal moves if null.
     */
    fun getLegalMoves(square: Square? = null): List<Move> {
        val state = _gameState.value
        return if (square != null) {
            ChessEngine.getLegalMoves(state, square)
        } else {
            ChessEngine.getLegalMoves(state)
        }
    }

    private fun handleTimeExpired(timedOutColor: PieceColor) {
        cancelAiJob()
        gameGeneration++
        _isThinking.value = false

        _gameState.update { current ->
            current.copy(
                whiteTimeSeconds = if (timedOutColor == PieceColor.WHITE) 0 else current.whiteTimeSeconds,
                blackTimeSeconds = if (timedOutColor == PieceColor.BLACK) 0 else current.blackTimeSeconds,
                isCheckmate = true,
                winner = timedOutColor.opposite(),
                isTimerRunning = false
            )
        }
    }

    fun cancelAiJob() {
        aiJob?.cancel()
        aiJob = null
        _isThinking.value = false
    }

    fun cancelAIJob() = cancelAiJob()

    fun cleanup() {
        cancelAiJob()
        gameClock.cancel()
    }
}
