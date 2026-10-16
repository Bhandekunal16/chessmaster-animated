package com.example.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D)),
        containerColor = Color(0xFF0A0F1D),
        topBar = {
            GrandmasterTopAppBar(
                settings = settings,
                onDifficultySelected = { diff -> viewModel.setDifficulty(diff) },
                onModeSelected = { mode -> viewModel.setGameMode(mode) },
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
            // Top Player Card (Black by default, or White if flipped)
            val topColor = if (isFlipped) PieceColor.WHITE else PieceColor.BLACK
            val topIsAI = settings.mode == GameMode.PVC && topColor == PieceColor.BLACK
            val topAdvantage = viewModel.calculateMaterialAdvantage(topColor)

            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                PlayerCard(
                    playerColor = topColor,
                    playerName = if (topIsAI) "AI Engine" else if (topColor == PieceColor.BLACK) "Player 2 (Black)" else "Player 1 (White)",
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

            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                PlayerCard(
                    playerColor = bottomColor,
                    playerName = if (bottomIsAI) "AI Engine" else if (bottomColor == PieceColor.WHITE) "Player 1 (White)" else "Player 2 (Black)",
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
}
