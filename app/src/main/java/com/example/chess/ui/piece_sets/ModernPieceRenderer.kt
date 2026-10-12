package com.example.chess.ui.piece_sets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

object ModernPieceRenderer {

    fun drawMinimal(drawScope: DrawScope, type: PieceType, color: PieceColor) {
        val isWhite = color == PieceColor.WHITE
        val primary = if (isWhite) Color(0xFFF1F5F9) else Color(0xFF0F172A)
        val border = if (isWhite) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        val accent = if (isWhite) Color(0xFF0284C7) else Color(0xFF38BDF8)

        val w = drawScope.size.width
        val h = drawScope.size.height
        fun x(normX: Float): Float = normX * w
        fun y(normY: Float): Float = normY * h

        val stroke = Stroke(width = w * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        with(drawScope) {
            when (type) {
                PieceType.PAWN -> {
                    // Minimalist clean pill base + slender stem + pure circle
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(x(0.25f), y(0.80f)),
                        size = Size(x(0.50f), y(0.12f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.06f)),
                        style = Fill
                    )
                    drawRoundRect(
                        color = border,
                        topLeft = Offset(x(0.25f), y(0.80f)),
                        size = Size(x(0.50f), y(0.12f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.06f)),
                        style = stroke
                    )
                    // Stem
                    drawRect(primary, topLeft = Offset(x(0.42f), y(0.48f)), size = Size(x(0.16f), y(0.32f)))
                    drawRect(border, topLeft = Offset(x(0.42f), y(0.48f)), size = Size(x(0.16f), y(0.32f)), style = stroke)
                    // Head
                    drawCircle(primary, radius = x(0.16f), center = Offset(x(0.50f), y(0.32f)))
                    drawCircle(border, radius = x(0.16f), center = Offset(x(0.50f), y(0.32f)), style = stroke)
                    drawCircle(accent, radius = x(0.04f), center = Offset(x(0.50f), y(0.32f)))
                }
                PieceType.KNIGHT -> {
                    // Angular geometric horse profile
                    val p = Path().apply {
                        moveTo(x(0.22f), y(0.90f))
                        lineTo(x(0.78f), y(0.90f))
                        lineTo(x(0.72f), y(0.60f))
                        lineTo(x(0.56f), y(0.20f)) // ear tip
                        lineTo(x(0.46f), y(0.24f))
                        lineTo(x(0.24f), y(0.46f)) // snout
                        lineTo(x(0.38f), y(0.58f))
                        lineTo(x(0.32f), y(0.90f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, border, style = stroke)
                    // Sleek geometric eye slit
                    drawLine(border, start = Offset(x(0.40f), y(0.42f)), end = Offset(x(0.46f), y(0.38f)), strokeWidth = w * 0.04f)
                }
                PieceType.BISHOP -> {
                    // Tapered column with clean 45-degree bevel cut
                    val p = Path().apply {
                        moveTo(x(0.24f), y(0.90f))
                        lineTo(x(0.76f), y(0.90f))
                        lineTo(x(0.65f), y(0.52f))
                        lineTo(x(0.50f), y(0.20f))
                        lineTo(x(0.35f), y(0.52f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, border, style = stroke)
                    // Clean angled slit cut
                    drawLine(border, start = Offset(x(0.58f), y(0.30f)), end = Offset(x(0.42f), y(0.46f)), strokeWidth = w * 0.05f)
                    drawCircle(accent, radius = x(0.045f), center = Offset(x(0.50f), y(0.16f)))
                }
                PieceType.ROOK -> {
                    // Pure monolith with clean rectangular crenellation cuts
                    val p = Path().apply {
                        moveTo(x(0.22f), y(0.90f))
                        lineTo(x(0.78f), y(0.90f))
                        lineTo(x(0.74f), y(0.26f))
                        lineTo(x(0.62f), y(0.26f))
                        lineTo(x(0.62f), y(0.38f))
                        lineTo(x(0.54f), y(0.38f))
                        lineTo(x(0.54f), y(0.26f))
                        lineTo(x(0.46f), y(0.26f))
                        lineTo(x(0.46f), y(0.38f))
                        lineTo(x(0.38f), y(0.38f))
                        lineTo(x(0.38f), y(0.26f))
                        lineTo(x(0.26f), y(0.26f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, border, style = stroke)
                }
                PieceType.QUEEN -> {
                    // Flared sleek trapezoid with 3 sharp crown chevrons
                    val p = Path().apply {
                        moveTo(x(0.22f), y(0.90f))
                        lineTo(x(0.78f), y(0.90f))
                        lineTo(x(0.68f), y(0.52f))
                        lineTo(x(0.80f), y(0.26f)) // right tip
                        lineTo(x(0.62f), y(0.38f))
                        lineTo(x(0.50f), y(0.20f)) // center tip
                        lineTo(x(0.38f), y(0.38f))
                        lineTo(x(0.20f), y(0.26f)) // left tip
                        lineTo(x(0.32f), y(0.52f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, border, style = stroke)
                    drawCircle(accent, radius = x(0.04f), center = Offset(x(0.50f), y(0.17f)))
                }
                PieceType.KING -> {
                    // Stately column with clean floating cross
                    val p = Path().apply {
                        moveTo(x(0.20f), y(0.90f))
                        lineTo(x(0.80f), y(0.90f))
                        lineTo(x(0.70f), y(0.42f))
                        lineTo(x(0.30f), y(0.42f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, border, style = stroke)
                    // Floating diamond cross
                    drawRect(primary, topLeft = Offset(x(0.46f), y(0.18f)), size = Size(x(0.08f), y(0.20f)))
                    drawRect(border, topLeft = Offset(x(0.46f), y(0.18f)), size = Size(x(0.08f), y(0.20f)), style = stroke)
                    drawRect(primary, topLeft = Offset(x(0.38f), y(0.23f)), size = Size(x(0.24f), y(0.08f)))
                    drawRect(border, topLeft = Offset(x(0.38f), y(0.23f)), size = Size(x(0.24f), y(0.08f)), style = stroke)
                }
            }
        }
    }

    fun draw3DRealistic(drawScope: DrawScope, type: PieceType, color: PieceColor) {
        val isWhite = color == PieceColor.WHITE
        val gradientTop = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF334155)
        val gradientBottom = if (isWhite) Color(0xFFCBD5E1) else Color(0xFF0F172A)
        val specular = if (isWhite) Color(0x99FFFFFF) else Color(0x6694A3B8)
        val shadow = Color(0x44000000)

        val w = drawScope.size.width
        val h = drawScope.size.height
        fun x(normX: Float): Float = normX * w
        fun y(normY: Float): Float = normY * h

        with(drawScope) {
            // Draw realistic soft contact shadow ellipse on square floor
            drawOval(
                color = shadow,
                topLeft = Offset(x(0.12f), y(0.86f)),
                size = Size(x(0.76f), y(0.10f))
            )

            // Draw base shape with vertical lighting gradient
            val brush = Brush.linearGradient(
                colors = listOf(gradientTop, gradientBottom),
                start = Offset(x(0.5f), y(0.1f)),
                end = Offset(x(0.5f), y(0.9f))
            )

            // Draw Staunton with 3D gradient & specular lighting arc
            StauntonPieceRenderer.draw(this, type, color, isFlatVector = false)

            // Specular sheen highlight along left flank
            val highlightPath = Path().apply {
                moveTo(x(0.35f), y(0.30f))
                cubicTo(x(0.32f), y(0.45f), x(0.32f), y(0.65f), x(0.30f), y(0.80f))
            }
            drawPath(
                highlightPath,
                specular,
                style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
            )
        }
    }
}
