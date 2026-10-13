package com.example.chess.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import kotlin.math.*
import com.example.chess.types.*

private const val zero = 0f
private const val one = 1f
private const val two = 2f



object PieceAnimationCalculator {
    fun calculate(
        piece: Piece,
        from: Square,
        to: Square,
        progress: Float,
        isCapture: Boolean,
        isCastlingRook: Boolean = false
    ): AnimatedPieceState {
        val p = progress.coerceIn(zero, one)

        return when (piece.type) {
            PieceType.PAWN -> calculatePawn(piece, from, to, p, isCapture)
            PieceType.KNIGHT -> calculateKnight(piece, from, to, p, isCapture)
            PieceType.BISHOP -> calculateBishop(piece, from, to, p, isCapture)
            PieceType.ROOK -> calculateRook(piece, from, to, p, isCapture, isCastlingRook)
            PieceType.QUEEN -> calculateQueen(piece, from, to, p, isCapture)
            PieceType.KING -> calculateKing(piece, from, to, p, isCapture)
        }
    }

    private fun calculatePawn(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val isDoubleMove = abs(to.rank - from.rank) == 2
        val eased = if (isDoubleMove) easeInOutCubic(p) else easeOutQuad(p)

        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        val hopFactor = sin(p * Math.PI.toFloat())
        val scale = one + (if (isDoubleMove) 0.22f else 0.12f) * hopFactor
        val shadow = 6f * hopFactor

        val rotation = if (isCapture) {
            val dir = if (to.file > from.file) one else -one
            dir * 14f * sin(p * Math.PI.toFloat())
        } else zero

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = rotation,
            shadowOffset = shadow
        )
    }

    private fun calculateKnight(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val jumpHeight = sin(p * Math.PI.toFloat())
        val scale = one + (if (isCapture) 0.55f else 0.40f) * jumpHeight
        val shadow = 16f * jumpHeight

        val dx = (to.file - from.file).toFloat()
        val dy = (to.rank - from.rank).toFloat()

        val midX: Float
        val midY: Float
        if (abs(dx) > abs(dy)) {
            midX = from.file + dx * 0.45f
            midY = from.rank + dy * 0.15f + (if (piece.color == PieceColor.WHITE) 0.35f else -0.35f)
        } else {
            midX = from.file + dx * 0.15f + (if (dx >= 0) 0.35f else -0.35f)
            midY = from.rank + dy * 0.45f
        }

        val u = one - p
        val curX = u * u * from.file + 2 * u * p * midX + p * p * to.file
        val curY = u * u * from.rank + 2 * u * p * midY + p * p * to.rank

        val tiltDir = if (dx >= 0) one else -one
        val rotation = tiltDir * (if (isCapture) 28f else 18f) * sin(p * Math.PI.toFloat())

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = rotation,
            shadowOffset = shadow
        )
    }

    private fun calculateBishop(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val eased = if (isCapture) easeInQuad(p) else easeInOutCubic(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        val dx = to.file - from.file
        val tiltAngle = if (dx > 0) 10f else -10f
        val rotation = tiltAngle * sin(p * Math.PI.toFloat())

        val aura = if (isCapture) Color(0x6600E5FF) else null

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = one + 0.08f * sin(p * Math.PI.toFloat()),
            rotation = rotation,
            auraColor = aura
        )
    }

    private fun calculateRook(
        piece: Piece,
        from: Square,
        to: Square,
        p: Float,
        isCapture: Boolean,
        isCastlingRook: Boolean
    ): AnimatedPieceState {
        val eased = if (isCapture) easeInExpo(p) else easeOutCubic(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        val scale = if (p > 0.85f && isCapture) {
            one + 0.15f * sin((p - 0.85f) / 0.15f * Math.PI.toFloat())
        } else one

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            isCastlingRook = isCastlingRook,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = zero,
            shadowOffset = two
        )
    }

    private fun calculateQueen(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val eased = if (isCapture) easeInOutQuart(p) else easeInOutCubic(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        val lift = sin(p * Math.PI.toFloat())
        val scale = one + 0.18f * lift
        val shadow = 8f * lift

        val aura = if (piece.color == PieceColor.WHITE) {
            Color(0x88FFD700)
        } else {
            Color(0x88A855F7)
        }

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = zero,
            shadowOffset = shadow,
            auraColor = aura
        )
    }

    private fun calculateKing(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val eased = easeInOutQuad(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        val pulse = sin(p * Math.PI.toFloat())
        val scale = one + 0.10f * pulse

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = zero,
            shadowOffset = 4f * pulse
        )
    }

    fun generateImpactParticles(
        type: ImpactType,
        isWhite: Boolean,
        themeAccentColor: Color? = null,
        count: Int = 14
    ): List<Particle> {
        val baseColor = themeAccentColor ?: when (type) {
            ImpactType.PAWN_SPARK -> if (isWhite) Color(0xFFFDE047) else Color(0xFFFB923C)
            ImpactType.KNIGHT_AERIAL_STRIKE -> Color(0xFF38BDF8)
            ImpactType.BISHOP_SLASH -> Color(0xFF34D399)
            ImpactType.ROOK_SHOCKWAVE -> Color(0xFFF97316)
            ImpactType.QUEEN_ROYAL_BURST -> if (isWhite) Color(0xFFFACC15) else Color(0xFFC084FC)
            ImpactType.KING_HEAVY_IMPACT -> Color(0xFFE11D48)
            ImpactType.PROMOTION_BURST -> Color(0xFFFFD700)
        }

        return (0 until count).map { i ->
            val angle = (i.toFloat() / count) * two * Math.PI.toFloat() + (Math.random().toFloat() * 0.4f)
            val speed = 0.8f + Math.random().toFloat() * 1.6f
            Particle(
                x = zero,
                y = zero,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                color = baseColor,
                alpha = one,
                radius = 3f + Math.random().toFloat() * 4f
            )
        }
    }

    private fun easeOutQuad(x: Float): Float = one - (one - x) * (one - x)
    private fun easeInQuad(x: Float): Float = x * x
    private fun easeInOutQuad(x: Float): Float = if (x < 0.5f) two * x * x else one - (-two * x + two).pow(2) / two
    private fun easeOutCubic(x: Float): Float = one - (one - x).pow(3)
    private fun easeInOutCubic(x: Float): Float = if (x < 0.5f) 4f * x * x * x else one - (-two * x + two).pow(3) / two
    private fun easeInOutQuart(x: Float): Float = if (x < 0.5f) 8f * x * x * x * x else one - (-two * x + two).pow(4) / two
    private fun easeInExpo(x: Float): Float = if (x == zero) zero else 2f.pow(10f * x - 10f)
}
