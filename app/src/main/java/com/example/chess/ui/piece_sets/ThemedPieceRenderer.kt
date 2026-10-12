package com.example.chess.ui.piece_sets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

object ThemedPieceRenderer {

    // 1. NEON CYBERPUNK PIECES
    fun drawNeon(drawScope: DrawScope, type: PieceType, color: PieceColor) {
        val isWhite = color == PieceColor.WHITE
        val neonColor = if (isWhite) Color(0xFF00F0FF) else Color(0xFFFF007F) // Cyan vs Neon Magenta
        val coreGlow = if (isWhite) Color(0x3300F0FF) else Color(0x33FF007F)
        val centerColor = if (isWhite) Color(0xFFE0FFFF) else Color(0xFFFFE4E1)

        val w = drawScope.size.width
        val h = drawScope.size.height
        fun x(normX: Float): Float = normX * w
        fun y(normY: Float): Float = normY * h

        with(drawScope) {
            // Draw outer neon glow bloom
            drawCircle(color = coreGlow, radius = x(0.38f), center = Offset(x(0.5f), y(0.5f)))

            val strokeGlow = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val strokeSharp = Stroke(width = w * 0.04f, cap = StrokeCap.Round, join = StrokeJoin.Round)

            when (type) {
                PieceType.PAWN -> {
                    // Geometric neon circuit pawn
                    val p = Path().apply {
                        moveTo(x(0.30f), y(0.85f))
                        lineTo(x(0.70f), y(0.85f))
                        lineTo(x(0.60f), y(0.55f))
                        lineTo(x(0.40f), y(0.55f))
                        close()
                    }
                    drawPath(p, coreGlow, style = Fill)
                    drawPath(p, neonColor, style = strokeSharp)
                    drawCircle(coreGlow, radius = x(0.18f), center = Offset(x(0.50f), y(0.32f)))
                    drawCircle(neonColor, radius = x(0.18f), center = Offset(x(0.50f), y(0.32f)), style = strokeSharp)
                    drawCircle(centerColor, radius = x(0.05f), center = Offset(x(0.50f), y(0.32f)))
                }
                PieceType.KNIGHT -> {
                    val p = Path().apply {
                        moveTo(x(0.24f), y(0.88f))
                        lineTo(x(0.76f), y(0.88f))
                        lineTo(x(0.68f), y(0.60f))
                        lineTo(x(0.60f), y(0.20f))
                        lineTo(x(0.48f), y(0.28f))
                        lineTo(x(0.26f), y(0.48f))
                        lineTo(x(0.42f), y(0.56f))
                        lineTo(x(0.32f), y(0.88f))
                        close()
                    }
                    drawPath(p, coreGlow, style = Fill)
                    drawPath(p, neonColor, style = strokeSharp)
                    drawCircle(centerColor, radius = x(0.04f), center = Offset(x(0.42f), y(0.40f)))
                }
                PieceType.BISHOP -> {
                    val p = Path().apply {
                        moveTo(x(0.25f), y(0.88f))
                        lineTo(x(0.75f), y(0.88f))
                        lineTo(x(0.62f), y(0.55f))
                        lineTo(x(0.50f), y(0.22f))
                        lineTo(x(0.38f), y(0.55f))
                        close()
                    }
                    drawPath(p, coreGlow, style = Fill)
                    drawPath(p, neonColor, style = strokeSharp)
                    drawLine(neonColor, Offset(x(0.58f), y(0.32f)), Offset(x(0.42f), y(0.48f)), strokeWidth = w * 0.045f)
                    drawCircle(centerColor, radius = x(0.05f), center = Offset(x(0.50f), y(0.18f)))
                }
                PieceType.ROOK -> {
                    val p = Path().apply {
                        moveTo(x(0.24f), y(0.88f))
                        lineTo(x(0.76f), y(0.88f))
                        lineTo(x(0.72f), y(0.32f))
                        lineTo(x(0.60f), y(0.32f))
                        lineTo(x(0.60f), y(0.42f))
                        lineTo(x(0.52f), y(0.42f))
                        lineTo(x(0.52f), y(0.32f))
                        lineTo(x(0.48f), y(0.32f))
                        lineTo(x(0.48f), y(0.42f))
                        lineTo(x(0.40f), y(0.42f))
                        lineTo(x(0.40f), y(0.32f))
                        lineTo(x(0.28f), y(0.32f))
                        close()
                    }
                    drawPath(p, coreGlow, style = Fill)
                    drawPath(p, neonColor, style = strokeSharp)
                    drawLine(centerColor, Offset(x(0.50f), y(0.50f)), Offset(x(0.50f), y(0.78f)), strokeWidth = w * 0.04f)
                }
                PieceType.QUEEN -> {
                    val p = Path().apply {
                        moveTo(x(0.24f), y(0.88f))
                        lineTo(x(0.76f), y(0.88f))
                        lineTo(x(0.66f), y(0.54f))
                        lineTo(x(0.78f), y(0.26f))
                        lineTo(x(0.62f), y(0.38f))
                        lineTo(x(0.50f), y(0.22f))
                        lineTo(x(0.38f), y(0.38f))
                        lineTo(x(0.22f), y(0.26f))
                        lineTo(x(0.34f), y(0.54f))
                        close()
                    }
                    drawPath(p, coreGlow, style = Fill)
                    drawPath(p, neonColor, style = strokeSharp)
                    drawCircle(centerColor, radius = x(0.045f), center = Offset(x(0.50f), y(0.18f)))
                }
                PieceType.KING -> {
                    val p = Path().apply {
                        moveTo(x(0.22f), y(0.88f))
                        lineTo(x(0.78f), y(0.88f))
                        lineTo(x(0.68f), y(0.44f))
                        lineTo(x(0.32f), y(0.44f))
                        close()
                    }
                    drawPath(p, coreGlow, style = Fill)
                    drawPath(p, neonColor, style = strokeSharp)
                    drawLine(neonColor, Offset(x(0.50f), y(0.16f)), Offset(x(0.50f), y(0.38f)), strokeWidth = w * 0.05f)
                    drawLine(neonColor, Offset(x(0.40f), y(0.24f)), Offset(x(0.60f), y(0.24f)), strokeWidth = w * 0.05f)
                    drawCircle(centerColor, radius = x(0.04f), center = Offset(x(0.50f), y(0.24f)))
                }
            }
        }
    }

    // 2. ROYAL LUXURY PIECES (Gold Filigree + Jewel Accents)
    fun drawRoyal(drawScope: DrawScope, type: PieceType, color: PieceColor) {
        val isWhite = color == PieceColor.WHITE
        val primary = if (isWhite) Color(0xFFFFFBEB) else Color(0xFF1E1B2E)
        val goldTrim = Color(0xFFF59E0B)
        val jewel = if (isWhite) Color(0xFFEF4444) else Color(0xFF06B6D4) // Ruby vs Sapphire jewels

        val w = drawScope.size.width
        val h = drawScope.size.height
        fun x(normX: Float): Float = normX * w
        fun y(normY: Float): Float = normY * h

        with(drawScope) {
            // Draw Staunton base
            StauntonPieceRenderer.draw(this, type, color, isFlatVector = false)

            // Add Royal Gold Filigree trims and jewels
            val goldStroke = Stroke(width = w * 0.04f, cap = StrokeCap.Round)

            // Gold Pediment Band
            drawLine(goldTrim, Offset(x(0.25f), y(0.86f)), Offset(x(0.75f), y(0.86f)), strokeWidth = w * 0.04f)

            when (type) {
                PieceType.KING -> {
                    // Jewel on King's headband
                    drawCircle(jewel, radius = x(0.04f), center = Offset(x(0.50f), y(0.44f)))
                    // Gold cross
                    drawLine(goldTrim, Offset(x(0.42f), y(0.14f)), Offset(x(0.58f), y(0.14f)), strokeWidth = w * 0.04f)
                    drawLine(goldTrim, Offset(x(0.50f), y(0.08f)), Offset(x(0.50f), y(0.22f)), strokeWidth = w * 0.04f)
                }
                PieceType.QUEEN -> {
                    // 3 Royal Jewels on Queen's coronet
                    drawCircle(jewel, radius = x(0.035f), center = Offset(x(0.50f), y(0.16f)))
                    drawCircle(goldTrim, radius = x(0.03f), center = Offset(x(0.38f), y(0.20f)))
                    drawCircle(goldTrim, radius = x(0.03f), center = Offset(x(0.62f), y(0.20f)))
                }
                PieceType.BISHOP -> {
                    drawCircle(jewel, radius = x(0.04f), center = Offset(x(0.50f), y(0.52f)))
                }
                PieceType.ROOK -> {
                    drawLine(goldTrim, Offset(x(0.30f), y(0.40f)), Offset(x(0.70f), y(0.40f)), strokeWidth = w * 0.035f)
                }
                PieceType.KNIGHT -> {
                    drawCircle(jewel, radius = x(0.03f), center = Offset(x(0.40f), y(0.36f)))
                }
                PieceType.PAWN -> {
                    drawCircle(goldTrim, radius = x(0.035f), center = Offset(x(0.50f), y(0.30f)))
                }
            }
        }
    }

    // 3. FANTASY PIECES (Mythic Spiked Crests & Armor)
    fun drawFantasy(drawScope: DrawScope, type: PieceType, color: PieceColor) {
        val isWhite = color == PieceColor.WHITE
        val primary = if (isWhite) Color(0xFFF8FAFC) else Color(0xFF1E293B)
        val outline = if (isWhite) Color(0xFF0F172A) else Color(0xFFE2E8F0)
        val magic = if (isWhite) Color(0xFFF59E0B) else Color(0xFFA855F7)

        val w = drawScope.size.width
        val h = drawScope.size.height
        fun x(normX: Float): Float = normX * w
        fun y(normY: Float): Float = normY * h
        val stroke = Stroke(width = w * 0.048f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        with(drawScope) {
            when (type) {
                PieceType.PAWN -> {
                    // Vanguard soldier pointed helm
                    val p = Path().apply {
                        moveTo(x(0.24f), y(0.88f))
                        lineTo(x(0.76f), y(0.88f))
                        lineTo(x(0.70f), y(0.54f))
                        lineTo(x(0.50f), y(0.18f)) // Spiked helm crest
                        lineTo(x(0.30f), y(0.54f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, outline, style = stroke)
                    // Visor slit
                    drawLine(magic, Offset(x(0.38f), y(0.50f)), Offset(x(0.62f), y(0.50f)), strokeWidth = w * 0.04f)
                }
                PieceType.KNIGHT -> {
                    // Dragon-maned horned warsteed
                    val p = Path().apply {
                        moveTo(x(0.24f), y(0.88f))
                        lineTo(x(0.76f), y(0.88f))
                        lineTo(x(0.70f), y(0.60f))
                        lineTo(x(0.78f), y(0.40f)) // Jagged mane spine
                        lineTo(x(0.68f), y(0.38f))
                        lineTo(x(0.74f), y(0.20f)) // Horn
                        lineTo(x(0.50f), y(0.24f))
                        lineTo(x(0.22f), y(0.44f)) // Spiked snout
                        lineTo(x(0.40f), y(0.56f))
                        lineTo(x(0.32f), y(0.88f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, outline, style = stroke)
                    drawCircle(magic, radius = x(0.035f), center = Offset(x(0.40f), y(0.38f)))
                }
                PieceType.BISHOP -> {
                    // Mystic archmage hood
                    val p = Path().apply {
                        moveTo(x(0.24f), y(0.88f))
                        lineTo(x(0.76f), y(0.88f))
                        lineTo(x(0.64f), y(0.50f))
                        lineTo(x(0.56f), y(0.16f)) // Curved hood tip
                        lineTo(x(0.48f), y(0.16f))
                        lineTo(x(0.36f), y(0.50f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, outline, style = stroke)
                    drawCircle(magic, radius = x(0.045f), center = Offset(x(0.50f), y(0.42f)))
                }
                PieceType.ROOK -> {
                    // Fortress citadel with battlements
                    val p = Path().apply {
                        moveTo(x(0.20f), y(0.88f))
                        lineTo(x(0.80f), y(0.88f))
                        lineTo(x(0.74f), y(0.28f))
                        lineTo(x(0.62f), y(0.28f))
                        lineTo(x(0.62f), y(0.38f))
                        lineTo(x(0.54f), y(0.38f))
                        lineTo(x(0.54f), y(0.28f))
                        lineTo(x(0.46f), y(0.28f))
                        lineTo(x(0.46f), y(0.38f))
                        lineTo(x(0.38f), y(0.38f))
                        lineTo(x(0.38f), y(0.28f))
                        lineTo(x(0.26f), y(0.28f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, outline, style = stroke)
                    // Arrow slit
                    drawLine(magic, Offset(x(0.50f), y(0.48f)), Offset(x(0.50f), y(0.68f)), strokeWidth = w * 0.045f)
                }
                PieceType.QUEEN -> {
                    // High-collared sorceress crown with wings
                    val p = Path().apply {
                        moveTo(x(0.22f), y(0.88f))
                        lineTo(x(0.78f), y(0.88f))
                        lineTo(x(0.68f), y(0.52f))
                        lineTo(x(0.82f), y(0.22f)) // Winged tip
                        lineTo(x(0.62f), y(0.36f))
                        lineTo(x(0.50f), y(0.18f)) // Center spire
                        lineTo(x(0.38f), y(0.36f))
                        lineTo(x(0.18f), y(0.22f)) // Winged tip
                        lineTo(x(0.32f), y(0.52f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, outline, style = stroke)
                    drawCircle(magic, radius = x(0.045f), center = Offset(x(0.50f), y(0.46f)))
                }
                PieceType.KING -> {
                    // Spiked warlord crown
                    val p = Path().apply {
                        moveTo(x(0.20f), y(0.88f))
                        lineTo(x(0.80f), y(0.88f))
                        lineTo(x(0.70f), y(0.45f))
                        lineTo(x(0.80f), y(0.24f)) // Crown point
                        lineTo(x(0.62f), y(0.32f))
                        lineTo(x(0.50f), y(0.14f)) // Imperial crest
                        lineTo(x(0.38f), y(0.32f))
                        lineTo(x(0.20f), y(0.24f)) // Crown point
                        lineTo(x(0.30f), y(0.45f))
                        close()
                    }
                    drawPath(p, primary, style = Fill)
                    drawPath(p, outline, style = stroke)
                    drawCircle(magic, radius = x(0.05f), center = Offset(x(0.50f), y(0.14f)))
                }
            }
        }
    }

    // 4. RETRO PIXEL ART PIECES
    fun drawPixelArt(drawScope: DrawScope, type: PieceType, color: PieceColor) {
        val isWhite = color == PieceColor.WHITE
        val primary = if (isWhite) Color(0xFFF8FAFC) else Color(0xFF1E293B)
        val outline = if (isWhite) Color(0xFF0F172A) else Color(0xFFE2E8F0)
        val accent = if (isWhite) Color(0xFFFBBF24) else Color(0xFF38BDF8)

        val w = drawScope.size.width
        val h = drawScope.size.height

        // 12x12 retro grid
        val cols = 12
        val rows = 12
        val cellW = w / cols
        val cellH = h / rows

        fun pixel(c: Int, r: Int, pColor: Color) {
            drawScope.drawRect(
                color = pColor,
                topLeft = Offset(c * cellW, r * cellH),
                size = Size(cellW, cellH)
            )
        }

        // Stepped pixel bitmaps for each piece
        val pixels = when (type) {
            PieceType.PAWN -> listOf(
                Pair(5, 2), Pair(6, 2),
                Pair(4, 3), Pair(5, 3), Pair(6, 3), Pair(7, 3),
                Pair(4, 4), Pair(5, 4), Pair(6, 4), Pair(7, 4),
                Pair(5, 5), Pair(6, 5),
                Pair(5, 6), Pair(6, 6),
                Pair(4, 7), Pair(5, 7), Pair(6, 7), Pair(7, 7),
                Pair(4, 8), Pair(5, 8), Pair(6, 8), Pair(7, 8),
                Pair(3, 9), Pair(4, 9), Pair(5, 9), Pair(6, 9), Pair(7, 9), Pair(8, 9),
                Pair(2, 10), Pair(3, 10), Pair(4, 10), Pair(5, 10), Pair(6, 10), Pair(7, 10), Pair(8, 10), Pair(9, 10)
            )
            PieceType.KNIGHT -> listOf(
                Pair(5, 2), Pair(6, 2),
                Pair(4, 3), Pair(5, 3), Pair(6, 3),
                Pair(3, 4), Pair(4, 4), Pair(5, 4), Pair(6, 4), Pair(7, 4),
                Pair(2, 5), Pair(3, 5), Pair(4, 5), Pair(5, 5), Pair(6, 5), Pair(7, 5),
                Pair(2, 6), Pair(3, 6), Pair(5, 6), Pair(6, 6), Pair(7, 6),
                Pair(3, 7), Pair(4, 7), Pair(5, 7), Pair(6, 7), Pair(7, 7),
                Pair(3, 8), Pair(4, 8), Pair(5, 8), Pair(6, 8), Pair(7, 8),
                Pair(3, 9), Pair(4, 9), Pair(5, 9), Pair(6, 9), Pair(7, 9), Pair(8, 9),
                Pair(2, 10), Pair(3, 10), Pair(4, 10), Pair(5, 10), Pair(6, 10), Pair(7, 10), Pair(8, 10), Pair(9, 10)
            )
            PieceType.BISHOP -> listOf(
                Pair(5, 1), Pair(6, 1),
                Pair(5, 2), Pair(6, 2),
                Pair(4, 3), Pair(5, 3), Pair(7, 3),
                Pair(4, 4), Pair(5, 4), Pair(6, 4), Pair(7, 4),
                Pair(4, 5), Pair(5, 5), Pair(6, 5), Pair(7, 5),
                Pair(5, 6), Pair(6, 6),
                Pair(4, 7), Pair(5, 7), Pair(6, 7), Pair(7, 7),
                Pair(4, 8), Pair(5, 8), Pair(6, 8), Pair(7, 8),
                Pair(3, 9), Pair(4, 9), Pair(5, 9), Pair(6, 9), Pair(7, 9), Pair(8, 9),
                Pair(2, 10), Pair(3, 10), Pair(4, 10), Pair(5, 10), Pair(6, 10), Pair(7, 10), Pair(8, 10), Pair(9, 10)
            )
            PieceType.ROOK -> listOf(
                Pair(2, 2), Pair(3, 2), Pair(5, 2), Pair(6, 2), Pair(8, 2), Pair(9, 2),
                Pair(2, 3), Pair(3, 3), Pair(4, 3), Pair(5, 3), Pair(6, 3), Pair(7, 3), Pair(8, 3), Pair(9, 3),
                Pair(3, 4), Pair(4, 4), Pair(5, 4), Pair(6, 4), Pair(7, 4), Pair(8, 4),
                Pair(4, 5), Pair(5, 5), Pair(6, 5), Pair(7, 5),
                Pair(4, 6), Pair(5, 6), Pair(6, 6), Pair(7, 6),
                Pair(4, 7), Pair(5, 7), Pair(6, 7), Pair(7, 7),
                Pair(3, 8), Pair(4, 8), Pair(5, 8), Pair(6, 8), Pair(7, 8), Pair(8, 8),
                Pair(3, 9), Pair(4, 9), Pair(5, 9), Pair(6, 9), Pair(7, 9), Pair(8, 9),
                Pair(2, 10), Pair(3, 10), Pair(4, 10), Pair(5, 10), Pair(6, 10), Pair(7, 10), Pair(8, 10), Pair(9, 10)
            )
            PieceType.QUEEN -> listOf(
                Pair(2, 2), Pair(5, 1), Pair(6, 1), Pair(9, 2),
                Pair(2, 3), Pair(4, 3), Pair(5, 3), Pair(6, 3), Pair(7, 3), Pair(9, 3),
                Pair(3, 4), Pair(4, 4), Pair(5, 4), Pair(6, 4), Pair(7, 4), Pair(8, 4),
                Pair(3, 5), Pair(4, 5), Pair(5, 5), Pair(6, 5), Pair(7, 5), Pair(8, 5),
                Pair(4, 6), Pair(5, 6), Pair(6, 6), Pair(7, 6),
                Pair(4, 7), Pair(5, 7), Pair(6, 7), Pair(7, 7),
                Pair(3, 8), Pair(4, 8), Pair(5, 8), Pair(6, 8), Pair(7, 8), Pair(8, 8),
                Pair(3, 9), Pair(4, 9), Pair(5, 9), Pair(6, 9), Pair(7, 9), Pair(8, 9),
                Pair(2, 10), Pair(3, 10), Pair(4, 10), Pair(5, 10), Pair(6, 10), Pair(7, 10), Pair(8, 10), Pair(9, 10)
            )
            PieceType.KING -> listOf(
                Pair(5, 1), Pair(6, 1),
                Pair(4, 2), Pair(5, 2), Pair(6, 2), Pair(7, 2),
                Pair(5, 3), Pair(6, 3),
                Pair(3, 4), Pair(4, 4), Pair(5, 4), Pair(6, 4), Pair(7, 4), Pair(8, 4),
                Pair(3, 5), Pair(4, 5), Pair(5, 5), Pair(6, 5), Pair(7, 5), Pair(8, 5),
                Pair(4, 6), Pair(5, 6), Pair(6, 6), Pair(7, 6),
                Pair(4, 7), Pair(5, 7), Pair(6, 7), Pair(7, 7),
                Pair(3, 8), Pair(4, 8), Pair(5, 8), Pair(6, 8), Pair(7, 8), Pair(8, 8),
                Pair(3, 9), Pair(4, 9), Pair(5, 9), Pair(6, 9), Pair(7, 9), Pair(8, 9),
                Pair(2, 10), Pair(3, 10), Pair(4, 10), Pair(5, 10), Pair(6, 10), Pair(7, 10), Pair(8, 10), Pair(9, 10)
            )
        }

        // Draw shadow/outline first
        for ((c, r) in pixels) {
            pixel(c, r, outline)
        }
        // Infill slightly smaller inside pixels
        for ((c, r) in pixels) {
            val inset = cellW * 0.1f
            drawScope.drawRect(
                color = primary,
                topLeft = Offset(c * cellW + inset, r * cellH + inset),
                size = Size(cellW - inset * 2, cellH - inset * 2)
            )
        }
    }
}
