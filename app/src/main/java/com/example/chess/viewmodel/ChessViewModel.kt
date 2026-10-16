package com.example.chess.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chess.animation.*
import com.example.chess.audio.ChessSoundPlayer
import com.example.chess.engine.GameClock
import com.example.chess.engine.GameController
import com.example.chess.model.*
import com.example.chess.repository.*
import com.example.chess.types.*
import com.example.chess.ui.ShareAppHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Presentation ViewModel for Grandmaster Chess.
 *
 * Responsibilities:
 * - Presentation state management (selections, animations, dialogs, visual effects).
 * - UI interaction processing and event dispatching.
 * - Delegating core chess operations to [GameController].
 * - Delegating user preferences to [SettingsRepository].
 * - Delegating active game session persistence to [ActiveGameRepository].
 * - Coordinating visual animation interpolations and sound effects.
 */
class ChessViewModel @JvmOverloads constructor(
    application: Application,
    private val settingsRepository: SettingsRepository = SharedPreferencesSettingsRepository(
        application.getSharedPreferences(SharedPreferencesSettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    ),
    private val gameController: GameController = GameController(),
    private val soundPlayer: ChessSoundPlayer = ChessSoundPlayer(application),
    private val activeGameRepository: ActiveGameRepository? = SharedPreferencesActiveGameRepository(
        application.getSharedPreferences(SharedPreferencesActiveGameRepository.PREFS_NAME, Context.MODE_PRIVATE)
    ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    // Domain state delegated to GameController and SettingsRepository
    val gameState: StateFlow<GameState> = gameController.gameState
    val settings: StateFlow<GameSettings> = settingsRepository.settings
    val isThinking: StateFlow<Boolean> = gameController.isThinking

    // Monotonic sequence counter to guarantee older async persistence writes cannot overwrite newer state
    private var saveSequence: Long = 0L

    // Active restoration job handle (for lifecycle tracking and deterministic testing)
    var restorationJob: Job? = null
        private set

    // Presentation-only state
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

    private var animationJob: Job? = null

    init {
        // Attempt active game state restoration across process recreation
        if (activeGameRepository != null) {
            restorationJob = viewModelScope.launch {
                val savedSnapshot = withContext(ioDispatcher) {
                    activeGameRepository.loadActiveGame()
                }
                if (savedSnapshot != null && savedSnapshot.moveHistory.isNotEmpty()) {
                    val restoredState = withContext(ioDispatcher) {
                        GameSnapshotSerializer.reconstructGameState(savedSnapshot)
                    }
                    if (restoredState != null) {
                        saveSequence = savedSnapshot.saveSequence
                        gameController.restoreGame(restoredState, savedSnapshot.timeControl)
                        return@launch
                    }
                }

                // If no active saved game or snapshot corrupted, synchronize initial time control
                val initialTimeControl = settingsRepository.settings.value.timeControl
                if (initialTimeControl != TimeControl.UNLIMITED) {
                    gameController.restartGame(initialTimeControl)
                }
            }
        } else {
            // Synchronize initial time control
            val initialTimeControl = settingsRepository.settings.value.timeControl
            if (initialTimeControl != TimeControl.UNLIMITED) {
                gameController.restartGame(initialTimeControl)
            }
        }

        // Listen for game-over state transitions safely (one-time event on transition)
        viewModelScope.launch {
            var previousGameOver = false
            gameController.gameState.collect { state ->
                if (state.isGameOver && !previousGameOver && !_showGameOverDialog.value) {
                    _showGameOverDialog.value = true
                }
                previousGameOver = state.isGameOver
            }
        }
    }

    // ==================== User Actions ====================

    fun onSquareClicked(square: Square) {
        if (isThinking.value || animationJob?.isActive == true || gameState.value.isGameOver) return

        val state = gameState.value
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
            _legalMovesForSelected.value = gameController.getLegalMoves(square)
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
        val currentSettings = settings.value
        val speed = currentSettings.animationSpeed
        val isSound = currentSettings.soundEnabled
        val isHaptics = currentSettings.hapticsEnabled

        if (speed == AnimationSpeed.OFF) {
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

            // Play move initiation audio
            if (move.isCapture) {
                soundPlayer.playCapture(move.piece.type, isSound, isHaptics)
            } else {
                soundPlayer.playMove(move.piece.type, isSound, isHaptics)
            }

            // Defeated piece dissolve effect
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
                        themeAccentColor = if (currentSettings.syncThemeEffects) Color(currentSettings.boardTheme.accentColor) else null
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

                _animatedPiece.value = PieceAnimationCalculator.calculate(
                    piece = move.piece,
                    from = move.from,
                    to = move.to,
                    progress = rawProgress,
                    isCapture = move.isCapture
                )

                if (castlingRookFrom != null && castlingRookTo != null) {
                    val rook = gameState.value.getPiece(castlingRookFrom)
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

                if (defeatedInitial != null) {
                    val defP = (rawProgress * 1.3f).coerceIn(0f, 1f)
                    _defeatedPiece.value = defeatedInitial.copy(
                        progress = defP,
                        scale = 1f - 0.75f * defP,
                        alpha = 1f - defP,
                        rotation = defP * 30f
                    )
                }

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

            // Clear visual animated overlays
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
        val applied = gameController.applyMove(move)
        if (!applied) return

        persistActiveGame()

        val next = gameState.value
        val isSound = settings.value.soundEnabled
        val isHaptics = settings.value.hapticsEnabled

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

        if (settings.value.mode == GameMode.PVP && settings.value.autoFlipBoard) {
            _isFlipped.value = next.activeColor == PieceColor.BLACK
        }
    }

    private fun checkPostMoveConditions() {
        val state = gameState.value
        if (state.isGameOver) return

        if (settings.value.mode == GameMode.PVC && state.activeColor == PieceColor.BLACK) {
            triggerAIMove()
        }
    }

    private fun triggerAIMove() {
        gameController.triggerAIMove(
            difficulty = settings.value.aiDifficulty,
            delayMs = 280L,
            onMoveReady = { aiMove ->
                performMoveWithAnimation(aiMove) {
                    checkPostMoveConditions()
                }
            }
        )
    }

    fun undoMove() {
        if (isThinking.value || animationJob?.isActive == true) return
        val success = gameController.undoMove(settings.value.mode)
        if (success) {
            _selectedSquare.value = null
            _legalMovesForSelected.value = emptyList()
            persistActiveGame()
        }
    }

    fun redoMove() {
        if (isThinking.value || animationJob?.isActive == true) return
        val success = gameController.redoMove()
        if (success) {
            persistActiveGame()
        }
    }

    fun getHint() {
        if (isThinking.value || gameState.value.isGameOver) return
        gameController.getHint(AIDifficulty.HARD) { bestMove ->
            if (bestMove != null) {
                _selectedSquare.value = bestMove.from
                _legalMovesForSelected.value = listOf(bestMove)
            }
        }
    }

    fun restartGame() {
        animationJob?.cancel()
        _selectedSquare.value = null
        _legalMovesForSelected.value = emptyList()
        _animatedPiece.value = null
        _castlingRookPiece.value = null
        _defeatedPiece.value = null
        _impactEffect.value = null
        _showGameOverDialog.value = false

        viewModelScope.launch(Dispatchers.IO) {
            activeGameRepository?.clearActiveGame()
        }

        gameController.restartGame(settings.value.timeControl)
    }

    private fun persistActiveGame() {
        val repo = activeGameRepository ?: return
        val currentSeq = ++saveSequence
        val currentSettings = settings.value
        val snapshot = gameController.createSnapshot(
            sequence = currentSeq,
            mode = currentSettings.mode,
            aiDifficulty = currentSettings.aiDifficulty,
            playerColor = currentSettings.playerColor,
            timeControl = currentSettings.timeControl
        )
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveActiveGame(snapshot)
        }
    }

    // ==================== Settings & Dialogs ====================

    fun updateSettings(newSettings: GameSettings) {
        val oldTimeControl = settings.value.timeControl
        settingsRepository.updateSettings(newSettings)
        _showSettingsDialog.value = false

        // Restart timer if time control changed
        if (oldTimeControl != newSettings.timeControl) {
            gameController.restartGame(newSettings.timeControl)
        }
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
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
            settings.value.copy(
                boardTheme = BoardTheme.CLASSIC_WOOD,
                pieceSet = PieceSet.CLASSIC_STAUNTON,
                syncThemeEffects = true
            )
        )
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

    fun toggleFlip() {
        _isFlipped.update { !it }
    }

    fun calculateMaterialAdvantage(forColor: PieceColor): Int {
        return gameController.calculateMaterialAdvantage(forColor)
    }

    fun getPgnText(): String {
        val currentSettings = settings.value
        val whitePlayer = if (currentSettings.mode == GameMode.PVC && currentSettings.playerColor == PieceColor.BLACK) {
            "Computer (${currentSettings.aiDifficulty.label})"
        } else {
            "Player 1"
        }
        val blackPlayer = if (currentSettings.mode == GameMode.PVC) {
            "Computer (${currentSettings.aiDifficulty.label})"
        } else {
            "Player 2"
        }
        return gameController.exportPgn(whitePlayer, blackPlayer)
    }

    /**
     * Triggers the Android native Sharesheet to share Grandmaster Chess.
     */
    fun shareApp(context: Context, downloadUrl: String = ShareAppHelper.DEFAULT_DOWNLOAD_URL) {
        ShareAppHelper.shareApp(context, downloadUrl)
    }

    override fun onCleared() {
        super.onCleared()
        animationJob?.cancel()
        gameController.cleanup()
    }

    companion object {
        fun provideFactory(
            application: Application,
            settingsRepository: SettingsRepository? = null,
            gameController: GameController? = null,
            soundPlayer: ChessSoundPlayer? = null,
            activeGameRepository: ActiveGameRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChessViewModel(
                    application = application,
                    settingsRepository = settingsRepository ?: SharedPreferencesSettingsRepository(
                        application.getSharedPreferences(SharedPreferencesSettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
                    ),
                    gameController = gameController ?: GameController(),
                    soundPlayer = soundPlayer ?: ChessSoundPlayer(application),
                    activeGameRepository = activeGameRepository ?: SharedPreferencesActiveGameRepository(
                        application.getSharedPreferences(SharedPreferencesActiveGameRepository.PREFS_NAME, Context.MODE_PRIVATE)
                    )
                ) as T
            }
        }
    }
}
