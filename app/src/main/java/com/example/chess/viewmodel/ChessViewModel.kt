package com.example.chess.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.animation.*
import com.example.chess.audio.ChessSoundPlayer
import com.example.chess.engine.ChessAI
import com.example.chess.engine.ChessEngine
import com.example.chess.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.chess.types.ImpactType

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("grandmaster_chess_prefs", Context.MODE_PRIVATE)
    private val soundPlayer = ChessSoundPlayer(application)

    private val _gameState = MutableStateFlow(ChessEngine.createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _settings = MutableStateFlow(GameSettings())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Square?>(null)
    val selectedSquare: StateFlow<Square?> = _selectedSquare.asStateFlow()

    private val _legalMovesForSelected = MutableStateFlow<List<Move>>(emptyList())
    val legalMovesForSelected: StateFlow<List<Move>> = _legalMovesForSelected.asStateFlow()

    private val _animatedPiece = MutableStateFlow<AnimatedPieceState?>(null)
    val animatedPiece: StateFlow<AnimatedPieceState?> = _animatedPiece.asStateFlow()

    private val _castlingRookPiece = MutableStateFlow<AnimatedPieceState?>(null)
    val castlingRookPiece: StateFlow<AnimatedPieceState?> = _castlingRookPiece.asStateFlow()

    private val _defeatedPiece = MutableStateFlow<DefeatedPieceState?>(null)
    val defeatedPiece: StateFlow<DefeatedPieceState?> = _defeatedPiece.asStateFlow()

    private val _impactEffect = MutableStateFlow<BoardImpactEffect?>(null)
    val impactEffect: StateFlow<BoardImpactEffect?> = _impactEffect.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _pendingPromotion = MutableStateFlow<Move?>(null)
    val pendingPromotion: StateFlow<Move?> = _pendingPromotion.asStateFlow()

    private val _showGameOverDialog = MutableStateFlow(false)
    val showGameOverDialog: StateFlow<Boolean> = _showGameOverDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog: StateFlow<Boolean> = _showThemeDialog.asStateFlow()

    private val _showPgnDialog = MutableStateFlow(false)
    val showPgnDialog: StateFlow<Boolean> = _showPgnDialog.asStateFlow()

    private var timerJob: Job? = null
    private var aiJob: Job? = null
    private var animationJob: Job? = null

    init {
        loadSettings()
        startTimerIfNeeded()
    }

    private fun loadSettings() {
        val themeOrdinal = prefs.getInt("theme", BoardTheme.CLASSIC_WOOD.ordinal)
        val pieceSetOrdinal = prefs.getInt("piece_set", PieceSet.CLASSIC_STAUNTON.ordinal)
        val modeOrdinal = prefs.getInt("mode", GameMode.PVC.ordinal)
        val diffOrdinal = prefs.getInt("difficulty", AIDifficulty.MEDIUM.ordinal)
        val speedOrdinal = prefs.getInt("speed", AnimationSpeed.NORMAL.ordinal)
        val sound = prefs.getBoolean("sound", true)
        val haptics = prefs.getBoolean("haptics", true)
        val is3D = prefs.getBoolean("is3D", false)
        val syncEffects = prefs.getBoolean("sync_theme_effects", true)

        val loaded = GameSettings(
            mode = GameMode.values().getOrElse(modeOrdinal) { GameMode.PVC },
            aiDifficulty = AIDifficulty.values().getOrElse(diffOrdinal) { AIDifficulty.MEDIUM },
            boardTheme = BoardTheme.values().getOrElse(themeOrdinal) { BoardTheme.CLASSIC_WOOD },
            pieceSet = PieceSet.values().getOrElse(pieceSetOrdinal) { PieceSet.CLASSIC_STAUNTON },
            animationSpeed = AnimationSpeed.values().getOrElse(speedOrdinal) { AnimationSpeed.NORMAL },
            soundEnabled = sound,
            hapticsEnabled = haptics,
            is3DView = is3D,
            syncThemeEffects = syncEffects
        )
        _settings.value = loaded
    }

    fun updateSettings(newSettings: GameSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putInt("theme", newSettings.boardTheme.ordinal)
            .putInt("piece_set", newSettings.pieceSet.ordinal)
            .putInt("mode", newSettings.mode.ordinal)
            .putInt("difficulty", newSettings.aiDifficulty.ordinal)
            .putInt("speed", newSettings.animationSpeed.ordinal)
            .putBoolean("sound", newSettings.soundEnabled)
            .putBoolean("haptics", newSettings.hapticsEnabled)
            .putBoolean("is3D", newSettings.is3DView)
            .putBoolean("sync_theme_effects", newSettings.syncThemeEffects)
            .apply()

        _showSettingsDialog.value = false
    }

    fun openThemeDialog() {
        _showThemeDialog.value = true
    }

    fun closeThemeDialog() {
        _showThemeDialog.value = false
    }

    fun resetToDefaultTheme() {
        updateSettings(
            _settings.value.copy(
                boardTheme = BoardTheme.CLASSIC_WOOD,
                pieceSet = PieceSet.CLASSIC_STAUNTON,
                syncThemeEffects = true
            )
        )
    }

    fun toggleFlip() {
        _isFlipped.update { !it }
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun openPgn() {
        _showPgnDialog.value = true
    }

    fun closePgn() {
        _showPgnDialog.value = false
    }

    fun dismissGameOver() {
        _showGameOverDialog.value = false
    }

    fun onSquareClicked(square: Square) {
        if (_isThinking.value || animationJob?.isActive == true || _gameState.value.isGameOver) return

        val state = _gameState.value
        val currentSelected = _selectedSquare.value

        // Check if user clicked a destination square for a legal move
        if (currentSelected != null) {
            val matchingMove = _legalMovesForSelected.value.find { it.to == square }
            if (matchingMove != null) {
                // If promotion required, show promotion dialog
                if (matchingMove.piece.type == PieceType.PAWN &&
                    ((matchingMove.piece.color == PieceColor.WHITE && square.rank == 7) ||
                     (matchingMove.piece.color == PieceColor.BLACK && square.rank == 0))
                ) {
                    _pendingPromotion.value = matchingMove
                    return
                }

                executePlayerMove(matchingMove)
                return
            }
        }

        // Otherwise select/deselect piece
        val clickedPiece = state.getPiece(square)
        if (clickedPiece != null && clickedPiece.color == state.activeColor) {
            _selectedSquare.value = square
            _legalMovesForSelected.value = ChessEngine.getLegalMoves(state, square)
        } else {
            _selectedSquare.value = null
            _legalMovesForSelected.value = emptyList()
        }
    }

    fun completePromotion(selectedType: PieceType) {
        val pending = _pendingPromotion.value ?: return
        _pendingPromotion.value = null
        val promoMove = pending.copy(promotionPiece = selectedType)
        executePlayerMove(promoMove)
    }

    private fun executePlayerMove(move: Move) {
        _selectedSquare.value = null
        _legalMovesForSelected.value = emptyList()

        performMoveWithAnimation(move) {
            checkPostMoveConditions()
        }
    }

    private fun performMoveWithAnimation(move: Move, onComplete: () -> Unit) {
        val speed = _settings.value.animationSpeed
        val isSound = _settings.value.soundEnabled
        val isHaptics = _settings.value.hapticsEnabled

        if (speed == AnimationSpeed.OFF) {
            // Instant move without animation
            applyAndPlaySound(move, isSound, isHaptics)
            onComplete()
            return
        }

        val baseDurationMs = when (speed) {
            AnimationSpeed.SLOW -> 520L
            AnimationSpeed.NORMAL -> 300L
            AnimationSpeed.FAST -> 170L
            AnimationSpeed.OFF -> 0L
        }

        animationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val totalDuration = (baseDurationMs * speed.durationMultiplier).toLong()

            // Sound on move start
            if (move.isCapture) {
                soundPlayer.playCapture(move.piece.type, isSound, isHaptics)
            } else {
                soundPlayer.playMove(move.piece.type, isSound, isHaptics)
            }

            // Setup defeated piece animation if capture
            val capturedPiece = move.capturedPiece
            val defeatedInitial = if (capturedPiece != null) {
                DefeatedPieceState(
                    piece = capturedPiece,
                    square = if (move.isEnPassant) Square(move.to.file, move.from.rank) else move.to,
                    progress = 0f,
                    scale = 1f,
                    alpha = 1f,
                    particles = PieceAnimationCalculator.generateImpactParticles(
                        type = when (move.piece.type) {
                            PieceType.KNIGHT -> ImpactType.KNIGHT_AERIAL_STRIKE
                            PieceType.ROOK -> ImpactType.ROOK_SHOCKWAVE
                            PieceType.QUEEN -> ImpactType.QUEEN_ROYAL_BURST
                            PieceType.BISHOP -> ImpactType.BISHOP_SLASH
                            PieceType.KING -> ImpactType.KING_HEAVY_IMPACT
                            else -> ImpactType.PAWN_SPARK
                        },
                        isWhite = move.piece.color == PieceColor.WHITE,
                        themeAccentColor = if (_settings.value.syncThemeEffects) androidx.compose.ui.graphics.Color(_settings.value.boardTheme.accentColor) else null
                    )
                )
            } else null

            _defeatedPiece.value = defeatedInitial

            // Check if castling to animate Rook synchronously
            val castlingRookFrom: Square?
            val castlingRookTo: Square?
            if (move.isCastling) {
                val rank = move.from.rank
                if (move.isKingsideCastle) {
                    castlingRookFrom = Square(7, rank)
                    castlingRookTo = Square(5, rank)
                } else {
                    castlingRookFrom = Square(0, rank)
                    castlingRookTo = Square(3, rank)
                }
            } else {
                castlingRookFrom = null
                castlingRookTo = null
            }

            // Animation interpolation loop (~60 FPS)
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)

                // Update moving piece
                _animatedPiece.value = PieceAnimationCalculator.calculate(
                    piece = move.piece,
                    from = move.from,
                    to = move.to,
                    progress = rawProgress,
                    isCapture = move.isCapture
                )

                // Update castling rook if any
                if (castlingRookFrom != null && castlingRookTo != null) {
                    val rook = _gameState.value.getPiece(castlingRookFrom)
                    if (rook != null) {
                        _castlingRookPiece.value = PieceAnimationCalculator.calculate(
                            piece = rook,
                            from = castlingRookFrom,
                            to = castlingRookTo,
                            progress = rawProgress,
                            isCapture = false,
                            isCastlingRook = true
                        )
                    }
                }

                // Update defeated piece dissolving
                if (defeatedInitial != null) {
                    val defP = (rawProgress * 1.3f).coerceIn(0f, 1f)
                    _defeatedPiece.value = defeatedInitial.copy(
                        progress = defP,
                        scale = 1f - 0.75f * defP,
                        alpha = 1f - defP,
                        rotation = defP * 30f
                    )
                }

                // Impact effect (sparks & shockwave on capture landing)
                if (move.isCapture && rawProgress >= 0.5f) {
                    val impactProg = ((rawProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
                    _impactEffect.value = BoardImpactEffect(
                        square = move.to,
                        type = when (move.piece.type) {
                            PieceType.ROOK -> ImpactType.ROOK_SHOCKWAVE
                            PieceType.KNIGHT -> ImpactType.KNIGHT_AERIAL_STRIKE
                            PieceType.QUEEN -> ImpactType.QUEEN_ROYAL_BURST
                            PieceType.BISHOP -> ImpactType.BISHOP_SLASH
                            PieceType.KING -> ImpactType.KING_HEAVY_IMPACT
                            else -> ImpactType.PAWN_SPARK
                        },
                        progress = impactProg,
                        isWhitePiece = move.piece.color == PieceColor.WHITE,
                        particles = defeatedInitial?.particles ?: emptyList()
                    )
                }

                if (rawProgress >= 1f) break
                delay(16)
            }

            // Clear animated layers
            _animatedPiece.value = null
            _castlingRookPiece.value = null
            _defeatedPiece.value = null
            _impactEffect.value = null

            applyStateUpdate(move)
            onComplete()
        }
    }

    private fun applyAndPlaySound(move: Move, isSound: Boolean, isHaptics: Boolean) {
        if (move.isCapture) {
            soundPlayer.playCapture(move.piece.type, isSound, isHaptics)
        } else {
            soundPlayer.playMove(move.piece.type, isSound, isHaptics)
        }
        applyStateUpdate(move)
    }

    private fun applyStateUpdate(move: Move) {
        val next = ChessEngine.applyMove(_gameState.value, move)
        _gameState.value = next

        val isSound = _settings.value.soundEnabled
        val isHaptics = _settings.value.hapticsEnabled

        if (next.isCheckmate) {
            soundPlayer.playCheckmate(isSound, isHaptics)
            _showGameOverDialog.value = true
        } else if (next.isDraw) {
            _showGameOverDialog.value = true
        } else if (next.isCheck) {
            soundPlayer.playCheck(isSound, isHaptics)
        } else if (move.promotionPiece != null) {
            soundPlayer.playPromotion(isSound, isHaptics)
        }

        // Auto flip board in PvP if enabled
        if (_settings.value.mode == GameMode.PVP && _settings.value.autoFlipBoard) {
            _isFlipped.value = next.activeColor == PieceColor.BLACK
        }
    }

    private fun checkPostMoveConditions() {
        val state = _gameState.value
        if (state.isGameOver) return

        // Trigger AI move if in PVC mode and it's Black's turn (or White's turn if player is Black)
        if (_settings.value.mode == GameMode.PVC && state.activeColor == PieceColor.BLACK) {
            triggerAIMove()
        }
    }

    private fun triggerAIMove() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _isThinking.value = true
            // Small pause for realistic human pacing and animation clearance
            delay(280)

            val currentState = _gameState.value
            val bestMove = withContext(Dispatchers.Default) {
                ChessAI.findBestMove(currentState, _settings.value.aiDifficulty)
            }

            _isThinking.value = false

            if (bestMove != null && isActive && !currentState.isGameOver) {
                performMoveWithAnimation(bestMove) {
                    checkPostMoveConditions()
                }
            }
        }
    }

    fun undoMove() {
        if (_isThinking.value || animationJob?.isActive == true) return
        val history = _gameState.value.moveHistory
        if (history.isEmpty()) return

        // In PVC, undo two moves (player and AI) to return to player's turn
        val undoCount = if (_settings.value.mode == GameMode.PVC && history.size >= 2) 2 else 1
        val targetSize = history.size - undoCount

        // Replay from initial state to target size
        var reconstructed = ChessEngine.createInitialState()
        for (i in 0 until targetSize) {
            reconstructed = ChessEngine.applyMove(reconstructed, history[i])
        }

        _gameState.value = reconstructed.copy(
            redoHistory = history.takeLast(undoCount)
        )
        _selectedSquare.value = null
        _legalMovesForSelected.value = emptyList()
    }

    fun redoMove() {
        if (_isThinking.value || animationJob?.isActive == true) return
        val redos = _gameState.value.redoHistory
        if (redos.isEmpty()) return

        val move = redos.first()
        val next = ChessEngine.applyMove(_gameState.value, move)
        _gameState.value = next.copy(redoHistory = redos.drop(1))
    }

    fun getHint() {
        if (_isThinking.value || _gameState.value.isGameOver) return
        viewModelScope.launch {
            _isThinking.value = true
            val currentState = _gameState.value
            val bestMove = withContext(Dispatchers.Default) {
                ChessAI.findBestMove(currentState, AIDifficulty.HARD)
            }
            _isThinking.value = false

            if (bestMove != null) {
                _selectedSquare.value = bestMove.from
                _legalMovesForSelected.value = listOf(bestMove)
            }
        }
    }

    fun restartGame() {
        aiJob?.cancel()
        animationJob?.cancel()
        _isThinking.value = false
        _selectedSquare.value = null
        _legalMovesForSelected.value = emptyList()
        _animatedPiece.value = null
        _castlingRookPiece.value = null
        _defeatedPiece.value = null
        _impactEffect.value = null
        _showGameOverDialog.value = false

        val initial = ChessEngine.createInitialState()
        val timeTotal = _settings.value.timeControl.totalSeconds
        _gameState.value = initial.copy(
            whiteTimeSeconds = timeTotal,
            blackTimeSeconds = timeTotal,
            isTimerRunning = timeTotal > 0
        )
    }

    private fun startTimerIfNeeded() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val state = _gameState.value
                if (state.isTimerRunning && !state.isGameOver && _settings.value.timeControl != TimeControl.UNLIMITED) {
                    if (state.activeColor == PieceColor.WHITE) {
                        val newTime = (state.whiteTimeSeconds - 1).coerceAtLeast(0)
                        if (newTime == 0) {
                            _gameState.value = state.copy(
                                whiteTimeSeconds = 0,
                                isCheckmate = true,
                                winner = PieceColor.BLACK
                            )
                            _showGameOverDialog.value = true
                        } else {
                            _gameState.value = state.copy(whiteTimeSeconds = newTime)
                        }
                    } else {
                        val newTime = (state.blackTimeSeconds - 1).coerceAtLeast(0)
                        if (newTime == 0) {
                            _gameState.value = state.copy(
                                blackTimeSeconds = 0,
                                isCheckmate = true,
                                winner = PieceColor.WHITE
                            )
                            _showGameOverDialog.value = true
                        } else {
                            _gameState.value = state.copy(blackTimeSeconds = newTime)
                        }
                    }
                }
            }
        }
    }

    fun calculateMaterialAdvantage(forColor: PieceColor): Int {
        val whiteScore = _gameState.value.capturedBlack.sumOf { it.type.value }
        val blackScore = _gameState.value.capturedWhite.sumOf { it.type.value }
        val diff = if (forColor == PieceColor.WHITE) whiteScore - blackScore else blackScore - whiteScore
        return if (diff > 0) diff / 100 else 0
    }

    fun getPgnText(): String {
        return ChessEngine.exportPgn(
            state = _gameState.value,
            whitePlayer = if (_settings.value.mode == GameMode.PVC && _settings.value.playerColor == PieceColor.BLACK) "Computer (${_settings.value.aiDifficulty.label})" else "Player 1",
            blackPlayer = if (_settings.value.mode == GameMode.PVC) "Computer (${_settings.value.aiDifficulty.label})" else "Player 2"
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        aiJob?.cancel()
        animationJob?.cancel()
    }
}
