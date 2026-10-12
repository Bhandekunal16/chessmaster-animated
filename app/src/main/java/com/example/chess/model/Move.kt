package com.example.chess.model

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun copyWithout(color: PieceColor, kingside: Boolean = true, queenside: Boolean = true): CastlingRights {
        return when (color) {
            PieceColor.WHITE -> copy(
                whiteKingside = if (kingside) false else whiteKingside,
                whiteQueenside = if (queenside) false else whiteQueenside
            )
            PieceColor.BLACK -> copy(
                blackKingside = if (kingside) false else blackKingside,
                blackQueenside = if (queenside) false else blackQueenside
            )
        }
    }
}

data class Move(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val isKingsideCastle: Boolean = false,
    val promotionPiece: PieceType? = null,
    val san: String = "",
    val fenBefore: String = "",
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false
) {
    val isCapture: Boolean
        get() = capturedPiece != null || isEnPassant
}
