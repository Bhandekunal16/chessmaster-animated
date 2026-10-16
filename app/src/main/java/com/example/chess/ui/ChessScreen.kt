package com.example.chess.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.multiplayer.model.MultiplayerRole
import com.example.chess.multiplayer.model.MultiplayerStatus
import com.example.chess.multiplayer.ui.DrawOfferDialog
import com.example.chess.multiplayer.ui.LocalMultiplayerDialog
import com.example.chess.multiplayer.ui.MultiplayerDisconnectionDialog
import com.example.chess.viewmodel.ChessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selectedSquare by viewModel.selectedSquare.collectAsStateWithLifecycle()
    val legalMoves by viewModel.legalMovesForSelected.collectAsStateWithLifecycle()
    val animatedPiece by viewModel.animatedPiece.collectAsStateWithLifecycle()
    val castlingRook by viewModel.castlingRookPiece.collectAsStateWithLifecycle()
    val defeatedPiece by viewModel.defeatedPiece.collectAsStateWithLifecycle()
    val impactEffect by viewModel.impactEffect.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val isFlipped by viewModel.isFlipped.collectAsStateWithLifecycle()
    val pendingPromotion by viewModel.pendingPromotion.collectAsStateWithLifecycle()
    val showGameOver by viewModel.showGameOverDialog.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val showThemes by viewModel.showThemeDialog.collectAsStateWithLifecycle()
    val showPgn by viewModel.showPgnDialog.collectAsStateWithLifecycle()

    // Multiplayer states
    val multiplayerSessionState by viewModel.multiplayerSessionState.collectAsStateWithLifecycle()
    val discoveredHosts by viewModel.discoveredHosts.collectAsStateWithLifecycle()
    val multiplayerNotice by viewModel.multiplayerNotice.collectAsStateWithLifecycle()
    val showMultiplayerDialog by viewModel.showMultiplayerDialog.collectAsStateWithLifecycle()
    val showDrawOfferDialog by viewModel.showDrawOfferDialog.collectAsStateWithLifecycle()
    val showDisconnectionDialog by viewModel.showDisconnectionDialog.collectAsStateWithLifecycle()

    val isMultiplayer = settings.mode == GameMode.LOCAL_MULTIPLAYER
    val isHost = multiplayerSessionState.role == MultiplayerRole.HOST
    val myRoleColor = if (isHost) PieceColor.WHITE else PieceColor.BLACK

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D)),
        containerColor = Color(0xFF0A0F1D),
        topBar = {
            GrandmasterTopAppBar(
                settings = settings,
                onDifficultySelected = { diff -> viewModel.setDifficulty(diff) },
                onModeSelected = { mode ->
                    if (mode == GameMode.LOCAL_MULTIPLAYER) {
                        viewModel.openMultiplayerDialog()
                    } else {
                        viewModel.setGameMode(mode)
                    }
                },
                onOpenMultiplayer = { viewModel.openMultiplayerDialog() },
                onThemeClick = { viewModel.openThemeDialog() },
                onPgnClick = { viewModel.openPgn() },
                onShareClick = { ShareAppHelper.shareApp(context) },
                onSettingsClick = { viewModel.openSettings() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Multiplayer Status Ribbon
            if (isMultiplayer) {
                Surface(
                    color = when (multiplayerSessionState.status) {
                        MultiplayerStatus.CONNECTED, MultiplayerStatus.GAME_IN_PROGRESS -> Color(0xFF065F46)
                        MultiplayerStatus.HOSTING_LISTENING -> Color(0xFF1E3A8A)
                        MultiplayerStatus.CLIENT_CONNECTING -> Color(0xFF854D0E)
                        MultiplayerStatus.DISCONNECTED, MultiplayerStatus.ERROR -> Color(0xFF991B1B)
                        else -> Color(0xFF1E293B)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("multiplayer_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (multiplayerSessionState.status) {
                                    MultiplayerStatus.HOSTING_LISTENING -> "Hosting • Room ${multiplayerSessionState.roomCode} • Waiting for guest..."
                                    MultiplayerStatus.CLIENT_CONNECTING -> "Connecting to ${multiplayerSessionState.hostIp}..."
                                    MultiplayerStatus.CONNECTED, MultiplayerStatus.GAME_IN_PROGRESS -> "Wi-Fi Match • Connected (${if (isHost) "White Host" else "Black Guest"})"
                                    MultiplayerStatus.DISCONNECTED -> "Connection Lost • Tap Reconnect"
                                    MultiplayerStatus.ERROR -> "Network Issue: ${multiplayerSessionState.errorMessage ?: "Unknown error"}"
                                    else -> "Local Wi-Fi Match"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        if (multiplayerSessionState.latencyMs > 0) {
                            Text(
                                text = "${multiplayerSessionState.latencyMs} ms",
                                fontSize = 11.sp,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }
            }

            // Multiplayer In-Game Notice Card
            if (multiplayerNotice != null) {
                Surface(
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("multiplayer_notice_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = multiplayerNotice!!,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearMultiplayerNotice() },
                            modifier = Modifier.size(24.dp).testTag("clear_notice_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Top Player Card (Black by default, or White if flipped)
            val topColor = if (isFlipped) PieceColor.WHITE else PieceColor.BLACK
            val topIsAI = settings.mode == GameMode.PVC && topColor == PieceColor.BLACK
            val topAdvantage = viewModel.calculateMaterialAdvantage(topColor)

            val topPlayerName = when {
                isMultiplayer -> {
                    if (topColor == myRoleColor) {
                        "You (${if (topColor == PieceColor.WHITE) "White • Host" else "Black • Guest"})"
                    } else {
                        "Opponent (${if (topColor == PieceColor.WHITE) "White • Host" else "Black • Guest"})"
                    }
                }
                topIsAI -> "AI Engine"
                topColor == PieceColor.BLACK -> "Player 2 (Black)"
                else -> "Player 1 (White)"
            }

            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                PlayerCard(
                    playerColor = topColor,
                    playerName = topPlayerName,
                    isAI = topIsAI,
                    aiDifficulty = if (topIsAI) settings.aiDifficulty else null,
                    isThinking = isThinking && topColor == gameState.activeColor,
                    isCurrentTurn = gameState.activeColor == topColor && !gameState.isGameOver,
                    timeRemainingSeconds = if (topColor == PieceColor.WHITE) gameState.whiteTimeSeconds else gameState.blackTimeSeconds,
                    capturedPieces = if (topColor == PieceColor.WHITE) gameState.capturedBlack else gameState.capturedWhite,
                    materialAdvantage = topAdvantage,
                    pieceSet = settings.pieceSet,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Interactive Animated Chessboard
            ChessBoard(
                gameState = gameState,
                settings = settings,
                selectedSquare = selectedSquare,
                legalMovesForSelected = legalMoves,
                animatedPiece = animatedPiece,
                castlingRookPiece = castlingRook,
                defeatedPiece = defeatedPiece,
                impactEffect = impactEffect,
                isFlipped = isFlipped,
                isThinking = isThinking,
                onSquareClick = { viewModel.onSquareClicked(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            // Bottom Player Card (White by default, or Black if flipped)
            val bottomColor = if (isFlipped) PieceColor.BLACK else PieceColor.WHITE
            val bottomIsAI = settings.mode == GameMode.PVC && bottomColor == PieceColor.BLACK
            val bottomAdvantage = viewModel.calculateMaterialAdvantage(bottomColor)

            val bottomPlayerName = when {
                isMultiplayer -> {
                    if (bottomColor == myRoleColor) {
                        "You (${if (bottomColor == PieceColor.WHITE) "White • Host" else "Black • Guest"})"
                    } else {
                        "Opponent (${if (bottomColor == PieceColor.WHITE) "White • Host" else "Black • Guest"})"
                    }
                }
                bottomIsAI -> "AI Engine"
                bottomColor == PieceColor.WHITE -> "Player 1 (White)"
                else -> "Player 2 (Black)"
            }

            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                PlayerCard(
                    playerColor = bottomColor,
                    playerName = bottomPlayerName,
                    isAI = bottomIsAI,
                    aiDifficulty = if (bottomIsAI) settings.aiDifficulty else null,
                    isThinking = isThinking && bottomColor == gameState.activeColor,
                    isCurrentTurn = gameState.activeColor == bottomColor && !gameState.isGameOver,
                    timeRemainingSeconds = if (bottomColor == PieceColor.WHITE) gameState.whiteTimeSeconds else gameState.blackTimeSeconds,
                    capturedPieces = if (bottomColor == PieceColor.WHITE) gameState.capturedBlack else gameState.capturedWhite,
                    materialAdvantage = bottomAdvantage,
                    pieceSet = settings.pieceSet,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Move History Ribbon
            MoveHistoryRow(
                moves = gameState.moveHistory,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            // Action Controls
            ChessActionBar(
                canUndo = gameState.moveHistory.isNotEmpty(),
                canRedo = gameState.redoHistory.isNotEmpty(),
                isThinking = isThinking,
                onUndo = { viewModel.undoMove() },
                onRedo = { viewModel.redoMove() },
                onHint = { viewModel.getHint() },
                onThemes = { viewModel.openThemeDialog() },
                onFlip = { viewModel.toggleFlip() },
                onNewGame = { viewModel.restartGame() },
                onSettings = { viewModel.openSettings() },
                isMultiplayer = isMultiplayer,
                onOfferDraw = { viewModel.offerMultiplayerDraw() },
                onResign = { viewModel.resignMultiplayer() },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    // Pawn Promotion Picker Dialog
    if (pendingPromotion != null) {
        val color = pendingPromotion!!.piece.color
        PawnPromotionDialog(
            color = color,
            pieceSet = settings.pieceSet,
            onSelect = { selectedType ->
                viewModel.completePromotion(selectedType)
            }
        )
    }

    // Game Over Announcement Dialog
    if (showGameOver && gameState.isGameOver) {
        GameOverDialog(
            gameState = gameState,
            onRematch = { viewModel.restartGame() },
            onDismiss = { viewModel.dismissGameOver() },
            onExportPgn = {
                viewModel.dismissGameOver()
                viewModel.openPgn()
            }
        )
    }

    // Settings Dialog
    if (showSettings) {
        SettingsDialog(
            currentSettings = settings,
            onSave = { viewModel.updateSettings(it) },
            onOpenThemeStudio = { viewModel.openThemeDialog() },
            onOpenMultiplayer = { viewModel.openMultiplayerDialog() },
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // Dedicated Theme Studio Dialog
    if (showThemes) {
        ThemeSelectionDialog(
            currentSettings = settings,
            onApplySettings = { viewModel.updateSettings(it) },
            onDismiss = { viewModel.closeThemeDialog() }
        )
    }

    // PGN Dialog
    if (showPgn) {
        PgnDialog(
            pgnText = viewModel.getPgnText(),
            onDismiss = { viewModel.closePgn() }
        )
    }

    // Local Wi-Fi Multiplayer Dialog
    if (showMultiplayerDialog) {
        LocalMultiplayerDialog(
            sessionState = multiplayerSessionState,
            discoveredHosts = discoveredHosts,
            onStartHosting = { viewModel.startHostingMultiplayer() },
            onCancelHosting = { viewModel.cancelHostingMultiplayer() },
            onJoinGame = { address -> viewModel.joinMultiplayerMatch(address) },
            onStartDiscovery = { viewModel.startMultiplayerDiscovery() },
            onStopDiscovery = { viewModel.stopMultiplayerDiscovery() },
            onDismiss = { viewModel.closeMultiplayerDialog() }
        )
    }

    // Multiplayer Draw Offer Dialog
    if (showDrawOfferDialog) {
        DrawOfferDialog(
            onAccept = { viewModel.acceptMultiplayerDraw() },
            onDecline = { viewModel.declineMultiplayerDraw() }
        )
    }

    // Multiplayer Disconnection Alert
    if (showDisconnectionDialog) {
        MultiplayerDisconnectionDialog(
            reason = multiplayerSessionState.errorMessage ?: "Wi-Fi connection interrupted",
            onRetry = { viewModel.retryMultiplayerReconnect() },
            onLeaveMatch = { viewModel.leaveMultiplayerMatch() }
        )
    }
}
