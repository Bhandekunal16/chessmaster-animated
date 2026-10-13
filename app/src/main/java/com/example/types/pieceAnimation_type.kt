package com.example.chess.types

import com.example.chess.model.Square
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