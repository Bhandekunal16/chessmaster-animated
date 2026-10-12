package com.example.chess.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceSet
import com.example.chess.model.PieceType
import com.example.chess.ui.piece_sets.ModernPieceRenderer
import com.example.chess.ui.piece_sets.StauntonPieceRenderer
import com.example.chess.ui.piece_sets.ThemedPieceRenderer

object ChessPieceColors {
    val WhiteBody = Color(0xFFF8FAFC)
    val WhiteHighlight = Color(0xFFFFFFFF)
    val WhiteShadow = Color(0xFFCBD5E1)
    val WhiteOutline = Color(0xFF1E293B)

    val BlackBody = Color(0xFF1E293B)
    val BlackHighlight = Color(0xFF475569)
    val BlackShadow = Color(0xFF0F172A)
    val BlackOutline = Color(0xFFE2E8F0)
}

@Composable
fun ChessPieceView(
    type: PieceType,
    color: PieceColor,
    pieceSet: PieceSet = PieceSet.CLASSIC_STAUNTON,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawChessPiece(type, color, pieceSet)
        }
    }
}

fun DrawScope.drawChessPiece(
    type: PieceType,
    color: PieceColor,
    pieceSet: PieceSet = PieceSet.CLASSIC_STAUNTON
) {
    when (pieceSet) {
        PieceSet.CLASSIC_STAUNTON -> StauntonPieceRenderer.draw(this, type, color, isFlatVector = false)
        PieceSet.MODERN_MINIMAL -> ModernPieceRenderer.drawMinimal(this, type, color)
        PieceSet.REALISTIC_3D -> ModernPieceRenderer.draw3DRealistic(this, type, color)
        PieceSet.FLAT_VECTOR -> StauntonPieceRenderer.draw(this, type, color, isFlatVector = true)
        PieceSet.FANTASY -> ThemedPieceRenderer.drawFantasy(this, type, color)
        PieceSet.NEON -> ThemedPieceRenderer.drawNeon(this, type, color)
        PieceSet.ROYAL -> ThemedPieceRenderer.drawRoyal(this, type, color)
        PieceSet.PIXEL_ART -> ThemedPieceRenderer.drawPixelArt(this, type, color)
    }
}
