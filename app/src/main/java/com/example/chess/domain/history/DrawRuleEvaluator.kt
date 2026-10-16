package com.example.chess.domain.history

import com.example.chess.model.*

/**
 * Pure Kotlin evaluator for draw conditions: insufficient material, fifty-move rule, and seventy-five-move rule.
 */
object DrawRuleEvaluator {

    /**
     * Determines whether the board has insufficient material to force checkmate.
     * Recognized standard configurations:
     * 1. K vs K
     * 2. K+B vs K
     * 3. K+N vs K
     * 4. K+B vs K+B where both bishops are on the SAME square color.
     */
    fun isInsufficientMaterial(board: Array<Array<Piece?>>): Boolean {
        var whiteKnights = 0
        var whiteBishops = 0
        var blackKnights = 0
        var blackBishops = 0

        var whiteBishopSquareColor: Int? = null
        var blackBishopSquareColor: Int? = null

        for (r in 0..7) {
            for (f in 0..7) {
                val p = board[r][f] ?: continue
                when (p.type) {
                    PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                    PieceType.KNIGHT -> {
                        if (p.color == PieceColor.WHITE) whiteKnights++ else blackKnights++
                    }
                    PieceType.BISHOP -> {
                        val sqColor = (f + r) % 2
                        if (p.color == PieceColor.WHITE) {
                            whiteBishops++
                            whiteBishopSquareColor = sqColor
                        } else {
                            blackBishops++
                            blackBishopSquareColor = sqColor
                        }
                    }
                    PieceType.KING -> {}
                }
            }
        }

        val whiteMinors = whiteKnights + whiteBishops
        val blackMinors = blackKnights + blackBishops

        // King vs King
        if (whiteMinors == 0 && blackMinors == 0) return true

        // King + Minor vs King
        if ((whiteMinors == 1 && blackMinors == 0) || (whiteMinors == 0 && blackMinors == 1)) {
            return true
        }

        // King + Bishop vs King + Bishop: ONLY drawn if both bishops occupy the same square color
        if (whiteKnights == 0 && blackKnights == 0 && whiteBishops == 1 && blackBishops == 1) {
            return whiteBishopSquareColor == blackBishopSquareColor
        }

        return false
    }

    /**
     * Checks 50-move rule status based on [halfMoveClock].
     * 100 half-moves without pawn move or capture = 50-move claimable draw.
     * 150 half-moves (75 full moves) = 75-move automatic draw.
     */
    fun evaluateMoveCounterDraw(halfMoveClock: Int): Pair<Boolean, Boolean> {
        val isClaimable = halfMoveClock >= 100
        val isAutomatic = halfMoveClock >= 150
        return Pair(isClaimable, isAutomatic)
    }
}
