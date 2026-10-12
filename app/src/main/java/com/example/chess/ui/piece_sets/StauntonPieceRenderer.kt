package com.example.chess.ui.piece_sets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

object StauntonPieceRenderer {

    fun draw(drawScope: DrawScope, type: PieceType, color: PieceColor, isFlatVector: Boolean = false) {
        val isWhite = color == PieceColor.WHITE
        val body = if (isWhite) Color(0xFFF8FAFC) else Color(0xFF1E293B)
        val outline = if (isWhite) Color(0xFF0F172A) else Color(0xFFE2E8F0)
        val highlight = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF475569)

        val w = drawScope.size.width
        val h = drawScope.size.height
        fun x(normX: Float): Float = normX * w
        fun y(normY: Float): Float = normY * h

        val stroke = Stroke(
            width = w * if (isFlatVector) 0.055f else 0.045f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        with(drawScope) {
            when (type) {
                PieceType.PAWN -> drawStauntonPawn(::x, ::y, body, outline, highlight, stroke, isFlatVector)
                PieceType.KNIGHT -> drawStauntonKnight(::x, ::y, body, outline, highlight, stroke, isFlatVector)
                PieceType.BISHOP -> drawStauntonBishop(::x, ::y, body, outline, highlight, stroke, isFlatVector)
                PieceType.ROOK -> drawStauntonRook(::x, ::y, body, outline, highlight, stroke, isFlatVector)
                PieceType.QUEEN -> drawStauntonQueen(::x, ::y, body, outline, highlight, stroke, isFlatVector)
                PieceType.KING -> drawStauntonKing(::x, ::y, body, outline, highlight, stroke, isFlatVector)
            }
        }
    }

    private fun DrawScope.drawStauntonPawn(
        x: (Float) -> Float, y: (Float) -> Float,
        body: Color, outline: Color, highlight: Color, stroke: Stroke, isFlat: Boolean
    ) {
        val basePath = Path().apply {
            moveTo(x(0.22f), y(0.85f))
            lineTo(x(0.78f), y(0.85f))
            lineTo(x(0.82f), y(0.92f))
            lineTo(x(0.18f), y(0.92f))
            close()
        }
        drawPath(basePath, body, style = Fill)
        drawPath(basePath, outline, style = stroke)

        val bodyPath = Path().apply {
            moveTo(x(0.28f), y(0.85f))
            cubicTo(x(0.32f), y(0.68f), x(0.38f), y(0.55f), x(0.40f), y(0.48f))
            lineTo(x(0.60f), y(0.48f))
            cubicTo(x(0.62f), y(0.55f), x(0.68f), y(0.68f), x(0.72f), y(0.85f))
            close()
        }
        drawPath(bodyPath, body, style = Fill)
        drawPath(bodyPath, outline, style = stroke)

        drawRoundRect(
            color = body,
            topLeft = Offset(x(0.35f), y(0.45f)),
            size = androidx.compose.ui.geometry.Size(x(0.30f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f))
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(x(0.35f), y(0.45f)),
            size = androidx.compose.ui.geometry.Size(x(0.30f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f)),
            style = stroke
        )

        drawCircle(color = body, radius = x(0.18f), center = Offset(x(0.50f), y(0.30f)))
        drawCircle(color = outline, radius = x(0.18f), center = Offset(x(0.50f), y(0.30f)), style = stroke)

        if (!isFlat) {
            drawCircle(color = highlight, radius = x(0.045f), center = Offset(x(0.44f), y(0.24f)))
        }
    }

    private fun DrawScope.drawStauntonKnight(
        x: (Float) -> Float, y: (Float) -> Float,
        body: Color, outline: Color, highlight: Color, stroke: Stroke, isFlat: Boolean
    ) {
        val basePath = Path().apply {
            moveTo(x(0.20f), y(0.85f))
            lineTo(x(0.80f), y(0.85f))
            lineTo(x(0.84f), y(0.92f))
            lineTo(x(0.16f), y(0.92f))
            close()
        }
        drawPath(basePath, body, style = Fill)
        drawPath(basePath, outline, style = stroke)

        val horsePath = Path().apply {
            moveTo(x(0.26f), y(0.85f))
            cubicTo(x(0.26f), y(0.70f), x(0.30f), y(0.62f), x(0.35f), y(0.56f))
            lineTo(x(0.22f), y(0.42f))
            cubicTo(x(0.20f), y(0.38f), x(0.22f), y(0.32f), x(0.28f), y(0.31f))
            lineTo(x(0.40f), y(0.33f))
            lineTo(x(0.46f), y(0.16f))
            lineTo(x(0.54f), y(0.24f))
            cubicTo(x(0.64f), y(0.26f), x(0.74f), y(0.36f), x(0.72f), y(0.50f))
            cubicTo(x(0.71f), y(0.62f), x(0.73f), y(0.74f), x(0.74f), y(0.85f))
            close()
        }
        drawPath(horsePath, body, style = Fill)
        drawPath(horsePath, outline, style = stroke)

        drawCircle(color = outline, radius = x(0.035f), center = Offset(x(0.38f), y(0.37f)))
        drawCircle(color = highlight, radius = x(0.012f), center = Offset(x(0.37f), y(0.36f)))
        drawCircle(color = outline, radius = x(0.018f), center = Offset(x(0.25f), y(0.37f)))

        val manePath = Path().apply {
            moveTo(x(0.52f), y(0.35f))
            cubicTo(x(0.62f), y(0.42f), x(0.64f), y(0.52f), x(0.62f), y(0.65f))
        }
        drawPath(manePath, outline, style = stroke)
    }

    private fun DrawScope.drawStauntonBishop(
        x: (Float) -> Float, y: (Float) -> Float,
        body: Color, outline: Color, highlight: Color, stroke: Stroke, isFlat: Boolean
    ) {
        val basePath = Path().apply {
            moveTo(x(0.20f), y(0.85f))
            lineTo(x(0.80f), y(0.85f))
            lineTo(x(0.84f), y(0.92f))
            lineTo(x(0.16f), y(0.92f))
            close()
        }
        drawPath(basePath, body, style = Fill)
        drawPath(basePath, outline, style = stroke)

        val flarePath = Path().apply {
            moveTo(x(0.26f), y(0.85f))
            cubicTo(x(0.32f), y(0.72f), x(0.38f), y(0.60f), x(0.40f), y(0.54f))
            lineTo(x(0.60f), y(0.54f))
            cubicTo(x(0.62f), y(0.60f), x(0.68f), y(0.72f), x(0.74f), y(0.85f))
            close()
        }
        drawPath(flarePath, body, style = Fill)
        drawPath(flarePath, outline, style = stroke)

        drawRoundRect(
            color = body,
            topLeft = Offset(x(0.34f), y(0.52f)),
            size = androidx.compose.ui.geometry.Size(x(0.32f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f))
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(x(0.34f), y(0.52f)),
            size = androidx.compose.ui.geometry.Size(x(0.32f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f)),
            style = stroke
        )

        val mitrePath = Path().apply {
            moveTo(x(0.36f), y(0.52f))
            cubicTo(x(0.28f), y(0.38f), x(0.38f), y(0.22f), x(0.50f), y(0.18f))
            cubicTo(x(0.62f), y(0.22f), x(0.72f), y(0.38f), x(0.64f), y(0.52f))
            close()
        }
        drawPath(mitrePath, body, style = Fill)
        drawPath(mitrePath, outline, style = stroke)

        val slitPath = Path().apply {
            moveTo(x(0.56f), y(0.26f))
            lineTo(x(0.44f), y(0.38f))
        }
        drawPath(slitPath, outline, style = stroke)

        drawCircle(color = body, radius = x(0.05f), center = Offset(x(0.50f), y(0.15f)))
        drawCircle(color = outline, radius = x(0.05f), center = Offset(x(0.50f), y(0.15f)), style = stroke)
    }

    private fun DrawScope.drawStauntonRook(
        x: (Float) -> Float, y: (Float) -> Float,
        body: Color, outline: Color, highlight: Color, stroke: Stroke, isFlat: Boolean
    ) {
        val basePath = Path().apply {
            moveTo(x(0.18f), y(0.85f))
            lineTo(x(0.82f), y(0.85f))
            lineTo(x(0.86f), y(0.92f))
            lineTo(x(0.14f), y(0.92f))
            close()
        }
        drawPath(basePath, body, style = Fill)
        drawPath(basePath, outline, style = stroke)

        val bodyPath = Path().apply {
            moveTo(x(0.24f), y(0.85f))
            lineTo(x(0.28f), y(0.42f))
            lineTo(x(0.72f), y(0.42f))
            lineTo(x(0.76f), y(0.85f))
            close()
        }
        drawPath(bodyPath, body, style = Fill)
        drawPath(bodyPath, outline, style = stroke)

        drawRoundRect(
            color = body,
            topLeft = Offset(x(0.22f), y(0.36f)),
            size = androidx.compose.ui.geometry.Size(x(0.56f), y(0.08f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.01f), y(0.01f))
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(x(0.22f), y(0.36f)),
            size = androidx.compose.ui.geometry.Size(x(0.56f), y(0.08f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.01f), y(0.01f)),
            style = stroke
        )

        val battlementsPath = Path().apply {
            moveTo(x(0.24f), y(0.36f))
            lineTo(x(0.24f), y(0.22f))
            lineTo(x(0.35f), y(0.22f))
            lineTo(x(0.35f), y(0.30f))
            lineTo(x(0.44f), y(0.30f))
            lineTo(x(0.44f), y(0.22f))
            lineTo(x(0.56f), y(0.22f))
            lineTo(x(0.56f), y(0.30f))
            lineTo(x(0.65f), y(0.30f))
            lineTo(x(0.65f), y(0.22f))
            lineTo(x(0.76f), y(0.22f))
            lineTo(x(0.76f), y(0.36f))
            close()
        }
        drawPath(battlementsPath, body, style = Fill)
        drawPath(battlementsPath, outline, style = stroke)
    }

    private fun DrawScope.drawStauntonQueen(
        x: (Float) -> Float, y: (Float) -> Float,
        body: Color, outline: Color, highlight: Color, stroke: Stroke, isFlat: Boolean
    ) {
        val basePath = Path().apply {
            moveTo(x(0.18f), y(0.85f))
            lineTo(x(0.82f), y(0.85f))
            lineTo(x(0.86f), y(0.92f))
            lineTo(x(0.14f), y(0.92f))
            close()
        }
        drawPath(basePath, body, style = Fill)
        drawPath(basePath, outline, style = stroke)

        val bodyPath = Path().apply {
            moveTo(x(0.24f), y(0.85f))
            cubicTo(x(0.30f), y(0.68f), x(0.36f), y(0.52f), x(0.40f), y(0.46f))
            lineTo(x(0.60f), y(0.46f))
            cubicTo(x(0.64f), y(0.52f), x(0.70f), y(0.68f), x(0.76f), y(0.85f))
            close()
        }
        drawPath(bodyPath, body, style = Fill)
        drawPath(bodyPath, outline, style = stroke)

        drawRoundRect(
            color = body,
            topLeft = Offset(x(0.32f), y(0.44f)),
            size = androidx.compose.ui.geometry.Size(x(0.36f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f))
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(x(0.32f), y(0.44f)),
            size = androidx.compose.ui.geometry.Size(x(0.36f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f)),
            style = stroke
        )

        val coronetPath = Path().apply {
            moveTo(x(0.34f), y(0.44f))
            lineTo(x(0.22f), y(0.25f))
            lineTo(x(0.36f), y(0.32f))
            lineTo(x(0.38f), y(0.20f))
            lineTo(x(0.46f), y(0.30f))
            lineTo(x(0.50f), y(0.16f))
            lineTo(x(0.54f), y(0.30f))
            lineTo(x(0.62f), y(0.20f))
            lineTo(x(0.64f), y(0.32f))
            lineTo(x(0.78f), y(0.25f))
            lineTo(x(0.66f), y(0.44f))
            close()
        }
        drawPath(coronetPath, body, style = Fill)
        drawPath(coronetPath, outline, style = stroke)

        listOf(
            Offset(x(0.22f), y(0.25f)),
            Offset(x(0.38f), y(0.20f)),
            Offset(x(0.50f), y(0.16f)),
            Offset(x(0.62f), y(0.20f)),
            Offset(x(0.78f), y(0.25f))
        ).forEach { center ->
            drawCircle(color = highlight, radius = x(0.028f), center = center)
            drawCircle(color = outline, radius = x(0.028f), center = center, style = stroke)
        }
    }

    private fun DrawScope.drawStauntonKing(
        x: (Float) -> Float, y: (Float) -> Float,
        body: Color, outline: Color, highlight: Color, stroke: Stroke, isFlat: Boolean
    ) {
        val basePath = Path().apply {
            moveTo(x(0.16f), y(0.85f))
            lineTo(x(0.84f), y(0.85f))
            lineTo(x(0.88f), y(0.92f))
            lineTo(x(0.12f), y(0.92f))
            close()
        }
        drawPath(basePath, body, style = Fill)
        drawPath(basePath, outline, style = stroke)

        val bodyPath = Path().apply {
            moveTo(x(0.22f), y(0.85f))
            cubicTo(x(0.28f), y(0.68f), x(0.34f), y(0.54f), x(0.38f), y(0.46f))
            lineTo(x(0.62f), y(0.46f))
            cubicTo(x(0.66f), y(0.54f), x(0.72f), y(0.68f), x(0.78f), y(0.85f))
            close()
        }
        drawPath(bodyPath, body, style = Fill)
        drawPath(bodyPath, outline, style = stroke)

        drawRoundRect(
            color = body,
            topLeft = Offset(x(0.30f), y(0.44f)),
            size = androidx.compose.ui.geometry.Size(x(0.40f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f))
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(x(0.30f), y(0.44f)),
            size = androidx.compose.ui.geometry.Size(x(0.40f), y(0.06f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.02f), y(0.02f)),
            style = stroke
        )

        val crownPath = Path().apply {
            moveTo(x(0.32f), y(0.44f))
            cubicTo(x(0.20f), y(0.30f), x(0.34f), y(0.22f), x(0.50f), y(0.22f))
            cubicTo(x(0.66f), y(0.22f), x(0.80f), y(0.30f), x(0.68f), y(0.44f))
            close()
        }
        drawPath(crownPath, body, style = Fill)
        drawPath(crownPath, outline, style = stroke)

        val crossH = Path().apply {
            moveTo(x(0.42f), y(0.14f))
            lineTo(x(0.58f), y(0.14f))
        }
        val crossV = Path().apply {
            moveTo(x(0.50f), y(0.08f))
            lineTo(x(0.50f), y(0.22f))
        }
        drawPath(crossH, outline, style = stroke)
        drawPath(crossV, outline, style = stroke)
    }
}
