package com.example.chess.domain.history

import com.example.chess.domain.model.BoardPosition
import com.example.chess.domain.rules.MoveGenerator
import com.example.chess.model.*

/**
 * Tracks position repetition across match history in compliance with FIDE rules.
 * Correctly accounts for legal en passant availability.
 */
object RepetitionTracker {

    /**
     * Computes a normalized position signature string.
     * Two positions are identical under FIDE Rule 9.2 if:
     * 1. The same player is on move.
     * 2. Pieces of the same kind and color occupy the same squares.
     * 3. The possible moves of all the pieces of both players are the same, including
     *    castling rights and en passant availability.
     *
     * Note: An en passant target square is only legally relevant if an en passant capture
     * can actually be legally played without leaving the moving side's king in check.
     */
    fun computeNormalizedSignature(position: BoardPosition): String {
        val sb = StringBuilder()

        // 1. Piece placement
        for (r in 7 downTo 0) {
            for (f in 0..7) {
                val p = position.board[r][f]
                if (p == null) {
                    sb.append('.')
                } else {
                    val code = when (p.type) {
                        PieceType.PAWN -> 'p'
                        PieceType.KNIGHT -> 'n'
                        PieceType.BISHOP -> 'b'
                        PieceType.ROOK -> 'r'
                        PieceType.QUEEN -> 'q'
                        PieceType.KING -> 'k'
                    }
                    sb.append(if (p.color == PieceColor.WHITE) code.uppercaseChar() else code)
                }
            }
        }

        // 2. Side to move
        sb.append(if (position.activeColor == PieceColor.WHITE) " w " else " b ")

        // 3. Castling rights
        val rights = position.castlingRights
        val castlingSb = StringBuilder()
        if (rights.whiteKingside) castlingSb.append('K')
        if (rights.whiteQueenside) castlingSb.append('Q')
        if (rights.blackKingside) castlingSb.append('k')
        if (rights.blackQueenside) castlingSb.append('q')
        sb.append(if (castlingSb.isEmpty()) "-" else castlingSb.toString())

        // 4. En passant availability: only relevant if an en passant capture can legally be made
        val epTarget = position.enPassantTarget
        val isEpLegal = if (epTarget != null) {
            val legalMoves = MoveGenerator.generateLegalMoves(position)
            legalMoves.any { it.isEnPassant && it.to == epTarget }
        } else {
            false
        }

        sb.append(' ')
        sb.append(if (isEpLegal) epTarget!!.algebraic else "-")

        return sb.toString()
    }

    /**
     * Counts how many times the normalized signature of [current] has occurred in [historySignatures] + current.
     */
    fun countOccurrences(historySignatures: List<String>, currentSignature: String): Int {
        var count = 0
        for (sig in historySignatures) {
            if (sig == currentSignature) count++
        }
        return count
    }

    /**
     * Evaluates threefold repetition (3 occurrences -> claimable) and fivefold repetition (5 occurrences -> automatic).
     *
     * @return Pair(isClaimable, isAutomatic)
     */
    fun evaluateRepetition(historySignatures: List<String>, currentPosition: BoardPosition): Pair<Boolean, Boolean> {
        val currentSig = computeNormalizedSignature(currentPosition)
        val occurrences = countOccurrences(historySignatures, currentSig) + 1 // +1 for the current position itself

        val isClaimable = occurrences >= 3
        val isAutomatic = occurrences >= 5
        return Pair(isClaimable, isAutomatic)
    }
}
