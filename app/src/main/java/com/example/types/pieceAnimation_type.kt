package com.example.chess.types

import com.example.chess.model.Piece
import com.example.chess.model.Square
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class ImpactType {
    PAWN_SPARK,
    KNIGHT_AERIAL_STRIKE,
    BISHOP_SLASH,
    ROOK_SHOCKWAVE,
    QUEEN_ROYAL_BURST,
    KING_HEAVY_IMPACT,
    PROMOTION_BURST
}

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val alpha: Float,
    val radius: Float
)

data class BoardImpactEffect(
    val square: Square,
    val type: ImpactType,
    val progress: Float = 0f,
    val isWhitePiece: Boolean = true,
    val particles: List<Particle> = emptyList()
)

data class AnimatedPieceState(
    val piece: Piece,
    val from: Square,
    val to: Square,
    val isCapture: Boolean,
    val isCastlingRook: Boolean = false,
    val progress: Float = 0f,
    val currentCoord: Offset = Offset(from.file.toFloat(), from.rank.toFloat()),
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val shadowOffset: Float = 0f,
    val auraColor: Color? = null,
    val trailOffsets: List<Offset> = emptyList()
)