package com.example.chess.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.animation.*
import com.example.chess.engine.ChessEngine
import com.example.chess.model.*
import com.example.chess.types.*
import kotlin.math.sin

@Composable
fun ChessBoard(
    gameState: GameState,
    settings: GameSettings,
    selectedSquare: Square?,
    legalMovesForSelected: List<Move>,
    animatedPiece: AnimatedPieceState?,
    castlingRookPiece: AnimatedPieceState?,
    defeatedPiece: DefeatedPieceState?,
    impactEffect: BoardImpactEffect?,
    isFlipped: Boolean,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = settings.boardTheme
    val lightColor = Color(theme.lightSquare)
    val darkColor = Color(theme.darkSquare)
    val highlightColor = Color(theme.highlightColor)
    val checkColor = Color(theme.checkColor)
    val lastMoveColor = Color(theme.lastMoveColor)
    val legalMoveColor = Color(theme.legalMoveColor)
    val captureTargetColor = Color(theme.captureTargetColor)
    val borderColor = Color(theme.boardBorder)

    val lastMove = gameState.moveHistory.lastOrNull()
    val kingInCheckSquare = if (gameState.isCheck) {
        ChessEngine.findKingSquare(gameState.activeColor, gameState.board)
    } else null

    // Pulsing animation for check alert
    val infiniteTransition = rememberInfiniteTransition(label = "check_pulse")
    val checkPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
            .graphicsLayer {
                if (settings.is3DView) {
                    rotationX = 20f
                    cameraDistance = 14f * density
                    shadowElevation = 24.dp.toPx()
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(3.5.dp, borderColor, RoundedCornerShape(12.dp))
            .testTag("chess_board")
    ) {
        val boardSizePx = constraints.maxWidth.toFloat()
        val squareSizePx = boardSizePx / 8f
        val squareSizeDp = maxWidth / 8

        // Draw 8x8 squares grid
        Column(modifier = Modifier.fillMaxSize()) {
            for (displayRow in 0..7) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (displayCol in 0..7) {
                        // Calculate logical rank and file based on board flip
                        val file = if (isFlipped) 7 - displayCol else displayCol
                        val rank = if (isFlipped) displayRow else 7 - displayRow
                        val sq = Square(file, rank)

                        val isLight = (file + rank) % 2 != 0
                        val isSelected = selectedSquare == sq
                        val isLastMoveFrom = lastMove?.from == sq
                        val isLastMoveTo = lastMove?.to == sq
                        val isKingCheck = kingInCheckSquare == sq
                        val legalMoveTo = legalMovesForSelected.find { it.to == sq }

                        val sqBg = when {
                            isKingCheck -> checkColor.copy(alpha = checkPulse)
                            isSelected -> highlightColor
                            isLastMoveTo || isLastMoveFrom -> lastMoveColor
                            isLight -> lightColor
                            else -> darkColor
                        }

                        val piece = gameState.getPiece(sq)
                        // If piece is currently moving in animation, don't draw it static on board
                        val isCurrentlyMoving = (animatedPiece != null && animatedPiece.from == sq) ||
                                (castlingRookPiece != null && castlingRookPiece.from == sq)
                        // If piece was captured and is animating defeat, don't draw it static
                        val isCurrentlyDefeated = defeatedPiece != null && defeatedPiece.square == sq

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(sqBg)
                                .clickable { onSquareClick(sq) }
                                .testTag("square_${sq.algebraic}"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Coordinate Labels (ranks on left edge, files on bottom edge)
                            if (displayCol == 0) {
                                Text(
                                    text = (rank + 1).toString(),
                                    color = if (isLight) darkColor.copy(alpha = 0.7f) else lightColor.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 2.dp, top = 2.dp)
                                )
                            }
                            if (displayRow == 7) {
                                Text(
                                    text = ('a' + file).toString(),
                                    color = if (isLight) darkColor.copy(alpha = 0.7f) else lightColor.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 2.dp, bottom = 2.dp)
                                )
                            }

                            // Static Chess Piece
                            if (piece != null && !isCurrentlyMoving && !isCurrentlyDefeated) {
                                ChessPieceView(
                                    type = piece.type,
                                    color = piece.color,
                                    pieceSet = settings.pieceSet,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                )
                            }

                            // Legal Move Indicator
                            if (legalMoveTo != null) {
                                if (legalMoveTo.isCapture) {
                                    // Capture target marker: glowing target ring
                                    Box(
                                        modifier = Modifier
                                            .size(squareSizeDp * 0.85f)
                                            .border(3.dp, captureTargetColor, CircleShape)
                                            .background(captureTargetColor.copy(alpha = 0.2f), CircleShape)
                                    )
                                } else {
                                    // Empty legal square: soft centered dot
                                    Box(
                                        modifier = Modifier
                                            .size(squareSizeDp * 0.28f)
                                            .clip(CircleShape)
                                            .background(legalMoveColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Animated Defeated / Captured Piece Overlay
        if (defeatedPiece != null) {
            val displayCol = if (isFlipped) 7 - defeatedPiece.square.file else defeatedPiece.square.file
            val displayRow = if (isFlipped) defeatedPiece.square.rank else 7 - defeatedPiece.square.rank
            val leftOffset = squareSizeDp * displayCol
            val topOffset = squareSizeDp * displayRow

            Box(
                modifier = Modifier
                    .offset(x = leftOffset, y = topOffset)
                    .size(squareSizeDp)
                    .graphicsLayer {
                        scaleX = defeatedPiece.scale
                        scaleY = defeatedPiece.scale
                        alpha = defeatedPiece.alpha
                        rotationZ = defeatedPiece.rotation
                    },
                contentAlignment = Alignment.Center
            ) {
                ChessPieceView(
                    type = defeatedPiece.piece.type,
                    color = defeatedPiece.piece.color,
                    pieceSet = settings.pieceSet,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                )
            }
        }

        // Active Animated Piece (King, Queen, Rook, Bishop, Knight, Pawn)
        if (animatedPiece != null) {
            RenderAnimatedPiece(
                animated = animatedPiece,
                pieceSet = settings.pieceSet,
                isFlipped = isFlipped,
                squareSizeDp = squareSizeDp
            )
        }

        // Castling Rook Coordinated Animation
        if (castlingRookPiece != null) {
            RenderAnimatedPiece(
                animated = castlingRookPiece,
                pieceSet = settings.pieceSet,
                isFlipped = isFlipped,
                squareSizeDp = squareSizeDp
            )
        }

        // Board Ripple & Impact Particles Canvas Overlay
        if (impactEffect != null || (defeatedPiece != null && defeatedPiece.particles.isNotEmpty())) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw ripple rings
                if (impactEffect != null) {
                    val dispCol = if (isFlipped) 7 - impactEffect.square.file else impactEffect.square.file
                    val dispRow = if (isFlipped) impactEffect.square.rank else 7 - impactEffect.square.rank
                    val cx = (dispCol + 0.5f) * squareSizePx
                    val cy = (dispRow + 0.5f) * squareSizePx

                    val ringRadius = squareSizePx * 0.8f * impactEffect.progress
                    val ringAlpha = (1f - impactEffect.progress).coerceIn(0f, 1f)

                    val rippleColor = when (impactEffect.type) {
                        ImpactType.ROOK_SHOCKWAVE -> Color(0xFFF97316)
                        ImpactType.QUEEN_ROYAL_BURST -> Color(0xFFFACC15)
                        ImpactType.KNIGHT_AERIAL_STRIKE -> Color(0xFF38BDF8)
                        ImpactType.BISHOP_SLASH -> Color(0xFF34D399)
                        ImpactType.KING_HEAVY_IMPACT -> Color(0xFFE11D48)
                        ImpactType.PROMOTION_BURST -> Color(0xFFFFD700)
                        else -> Color(0xFFE2E8F0)
                    }

                    // Expanding shockwave rings
                    drawCircle(
                        color = rippleColor.copy(alpha = ringAlpha * 0.8f),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 4.dp.toPx() * (1f - impactEffect.progress))
                    )
                    drawCircle(
                        color = rippleColor.copy(alpha = ringAlpha * 0.4f),
                        radius = ringRadius * 0.6f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Impact particles
                    impactEffect.particles.forEach { p ->
                        val px = cx + p.vx * squareSizePx * 0.6f * impactEffect.progress
                        val py = cy + p.vy * squareSizePx * 0.6f * impactEffect.progress
                        drawCircle(
                            color = p.color.copy(alpha = ringAlpha),
                            radius = p.radius,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderAnimatedPiece(
    animated: AnimatedPieceState,
    pieceSet: PieceSet,
    isFlipped: Boolean,
    squareSizeDp: Dp
) {
    val displayX = if (isFlipped) 7f - animated.currentCoord.x else animated.currentCoord.x
    val displayY = if (isFlipped) animated.currentCoord.y else 7f - animated.currentCoord.y

    val leftOffset = squareSizeDp * displayX
    val topOffset = squareSizeDp * displayY

    Box(
        modifier = Modifier
            .offset(x = leftOffset, y = topOffset)
            .size(squareSizeDp)
            .graphicsLayer {
                scaleX = animated.scale
                scaleY = animated.scale
                rotationZ = animated.rotation
                alpha = animated.alpha
                if (animated.shadowOffset > 0f) {
                    shadowElevation = animated.shadowOffset.dp.toPx()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Royal / Magic Aura Glow behind Queen / Promotion
        if (animated.auraColor != null) {
            Box(
                modifier = Modifier
                    .size(squareSizeDp * 0.9f)
                    .background(animated.auraColor, CircleShape)
            )
        }

        ChessPieceView(
            type = animated.piece.type,
            color = animated.piece.color,
            pieceSet = pieceSet,
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        )
    }
}
