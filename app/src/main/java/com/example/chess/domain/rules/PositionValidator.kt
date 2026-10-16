package com.example.chess.domain.rules

import com.example.chess.domain.model.BoardPosition
import com.example.chess.model.*

/**
 * Validates chess position structural integrity and move legality.
 */
object PositionValidator {

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    /**
     * Verifies that [position] adheres to structural chess rules.
     */
    fun validatePosition(position: BoardPosition): ValidationResult {
        var whiteKings = 0
        var blackKings = 0

        for (r in 0..7) {
            for (f in 0..7) {
                val p = position.board[r][f] ?: continue
                if (p.type == PieceType.KING) {
                    if (p.color == PieceColor.WHITE) whiteKings++ else blackKings++
                }
                // Pawns can never occupy the 1st (rank 0) or 8th (rank 7) rank
                if (p.type == PieceType.PAWN && (r == 0 || r == 7)) {
                    return ValidationResult.Invalid("Pawn found on illegal rank $r at ${Square(f, r).algebraic}")
                }
            }
        }

        if (whiteKings != 1) {
            return ValidationResult.Invalid("Expected exactly 1 White King, found $whiteKings")
        }
        if (blackKings != 1) {
            return ValidationResult.Invalid("Expected exactly 1 Black King, found $blackKings")
        }

        // The player who is NOT on move cannot be in check
        val inactiveColor = position.activeColor.opposite()
        if (CheckDetector.isKingInCheck(inactiveColor, position.board)) {
            return ValidationResult.Invalid("Inactive king ($inactiveColor) is in check")
        }

        return ValidationResult.Valid
    }

    /**
     * Returns true if [move] is fully legal in [position].
     */
    fun isLegalMove(position: BoardPosition, move: Move): Boolean {
        val legals = MoveGenerator.generateLegalMoves(position, move.from)
        return legals.any {
            it.from == move.from &&
            it.to == move.to &&
            (move.promotionPiece == null || it.promotionPiece == move.promotionPiece)
        }
    }
}
