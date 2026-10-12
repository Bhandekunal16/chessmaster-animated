package com.example.chess.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import kotlin.math.*

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
    // Interpolated values calculated by piece-specific interpolator
    val currentCoord: Offset = Offset(from.file.toFloat(), from.rank.toFloat()),
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val shadowOffset: Float = 0f,
    val auraColor: Color? = null,
    val trailOffsets: List<Offset> = emptyList()
)

data class DefeatedPieceState(
    val piece: Piece,
    val square: Square,
    val progress: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val rotation: Float = 0f,
    val particles: List<Particle> = emptyList()
)

object PieceAnimationCalculator {

    /**
     * Calculates the distinct frame-by-frame transformation for each piece type.
     */
    fun calculate(
        piece: Piece,
        from: Square,
        to: Square,
        progress: Float,
        isCapture: Boolean,
        isCastlingRook: Boolean = false
    ): AnimatedPieceState {
        val p = progress.coerceIn(0f, 1f)

        return when (piece.type) {
            PieceType.PAWN -> calculatePawn(piece, from, to, p, isCapture)
            PieceType.KNIGHT -> calculateKnight(piece, from, to, p, isCapture)
            PieceType.BISHOP -> calculateBishop(piece, from, to, p, isCapture)
            PieceType.ROOK -> calculateRook(piece, from, to, p, isCapture, isCastlingRook)
            PieceType.QUEEN -> calculateQueen(piece, from, to, p, isCapture)
            PieceType.KING -> calculateKing(piece, from, to, p, isCapture)
        }
    }

    // PAWN: Hop or forward dash, diagonal lunge on attack
    private fun calculatePawn(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val isDoubleMove = abs(to.rank - from.rank) == 2
        val eased = if (isDoubleMove) easeInOutCubic(p) else easeOutQuad(p)

        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        // Vertical hop curve (parabola peaking at middle)
        val hopFactor = sin(p * Math.PI.toFloat())
        val scale = 1f + (if (isDoubleMove) 0.22f else 0.12f) * hopFactor
        val shadow = 6f * hopFactor

        // Diagonal lunge tilt on capture
        val rotation = if (isCapture) {
            val dir = if (to.file > from.file) 1f else -1f
            dir * 14f * sin(p * Math.PI.toFloat())
        } else 0f

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

    // KNIGHT: Distinctive curved L-shaped trajectory, midair elevation jump, flip/tilt!
    private fun calculateKnight(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        // High parabolic jump curve in Z-dimension (simulated height)
        val jumpHeight = sin(p * Math.PI.toFloat())
        val scale = 1f + (if (isCapture) 0.55f else 0.40f) * jumpHeight
        val shadow = 16f * jumpHeight

        // Non-linear L-curve trajectory:
        // We calculate an arched bezier waypoint to make it visibly jump around intervening pieces!
        val dx = (to.file - from.file).toFloat()
        val dy = (to.rank - from.rank).toFloat()

        // Waypoint biased toward the 2-step direction first for natural L-hop
        val midX: Float
        val midY: Float
        if (abs(dx) > abs(dy)) {
            // Moved 2 horizontally, 1 vertically: curve outward in Y
            midX = from.file + dx * 0.45f
            midY = from.rank + dy * 0.15f + (if (piece.color == PieceColor.WHITE) 0.35f else -0.35f)
        } else {
            // Moved 1 horizontally, 2 vertically: curve outward in X
            midX = from.file + dx * 0.15f + (if (dx >= 0) 0.35f else -0.35f)
            midY = from.rank + dy * 0.45f
        }

        // Quadratic bezier (from -> mid -> to)
        val u = 1f - p
        val curX = u * u * from.file + 2 * u * p * midX + p * p * to.file
        val curY = u * u * from.rank + 2 * u * p * midY + p * p * to.rank

        // Midair flip / tilt
        val tiltDir = if (dx >= 0) 1f else -1f
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

    // BISHOP: Smooth diagonal glide with flowing diagonal blade tilt
    private fun calculateBishop(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val eased = if (isCapture) easeInQuad(p) else easeInOutCubic(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        // Slight graceful diagonal lean matching motion direction
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
            scale = 1f + 0.08f * sin(p * Math.PI.toFloat()),
            rotation = rotation,
            auraColor = aura
        )
    }

    // ROOK: Heavy grounded slide along ranks/files with solid rumble
    private fun calculateRook(
        piece: Piece,
        from: Square,
        to: Square,
        p: Float,
        isCapture: Boolean,
        isCastlingRook: Boolean
    ): AnimatedPieceState {
        // Fast start, sudden heavy stop (decelerating ram)
        val eased = if (isCapture) easeInExpo(p) else easeOutCubic(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        // Grounded weight: scale compresses slightly on stop
        val scale = if (p > 0.85f && isCapture) {
            1f + 0.15f * sin((p - 0.85f) / 0.15f * Math.PI.toFloat())
        } else 1f

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            isCastlingRook = isCastlingRook,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = 0f,
            shadowOffset = 2f
        )
    }

    // QUEEN: Elegant royal glide, shimmer aura, effortless majestic dash
    private fun calculateQueen(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val eased = if (isCapture) easeInOutQuart(p) else easeInOutCubic(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        // Floating lift and royal shimmer aura
        val lift = sin(p * Math.PI.toFloat())
        val scale = 1f + 0.18f * lift
        val shadow = 8f * lift

        val aura = if (piece.color == PieceColor.WHITE) {
            Color(0x88FFD700) // Golden aura
        } else {
            Color(0x88A855F7) // Royal amethyst aura
        }

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = 0f,
            shadowOffset = shadow,
            auraColor = aura
        )
    }

    // KING: Deliberate, dignified, controlled step with heavy presence
    private fun calculateKing(piece: Piece, from: Square, to: Square, p: Float, isCapture: Boolean): AnimatedPieceState {
        val eased = easeInOutQuad(p)
        val curX = from.file + (to.file - from.file) * eased
        val curY = from.rank + (to.rank - from.rank) * eased

        val pulse = sin(p * Math.PI.toFloat())
        val scale = 1f + 0.10f * pulse

        return AnimatedPieceState(
            piece = piece,
            from = from,
            to = to,
            isCapture = isCapture,
            progress = p,
            currentCoord = Offset(curX, curY),
            scale = scale,
            rotation = 0f,
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
            val angle = (i.toFloat() / count) * 2f * Math.PI.toFloat() + (Math.random().toFloat() * 0.4f)
            val speed = 0.8f + Math.random().toFloat() * 1.6f
            Particle(
                x = 0f,
                y = 0f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                color = baseColor,
                alpha = 1f,
                radius = 3f + Math.random().toFloat() * 4f
            )
        }
    }

    // Easing helpers
    private fun easeOutQuad(x: Float): Float = 1f - (1f - x) * (1f - x)
    private fun easeInQuad(x: Float): Float = x * x
    private fun easeInOutQuad(x: Float): Float = if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f).pow(2) / 2f
    private fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3)
    private fun easeInOutCubic(x: Float): Float = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f
    private fun easeInOutQuart(x: Float): Float = if (x < 0.5f) 8f * x * x * x * x else 1f - (-2f * x + 2f).pow(4) / 2f
    private fun easeInExpo(x: Float): Float = if (x == 0f) 0f else 2f.pow(10f * x - 10f)
}
