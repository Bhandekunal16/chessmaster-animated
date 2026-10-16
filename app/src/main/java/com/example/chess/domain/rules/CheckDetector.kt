package com.example.chess.domain.rules

import com.example.chess.model.*

/**
 * Pure Kotlin check detector and square attack evaluation engine.
 * Independently testable without Android lifecycle or UI dependencies.
 */
object CheckDetector {

    val BISHOP_DIRECTIONS = arrayOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
    val ROOK_DIRECTIONS = arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
    val QUEEN_DIRECTIONS = BISHOP_DIRECTIONS + ROOK_DIRECTIONS
    val KNIGHT_OFFSETS = arrayOf(
        Pair(1, 2), Pair(2, 1), Pair(2, -1), Pair(1, -2),
        Pair(-1, -2), Pair(-2, -1), Pair(-2, 1), Pair(-1, 2)
    )

    /**
     * Determines whether [square] is under attack by any piece of [byColor].
     */
    fun isSquareAttacked(square: Square, byColor: PieceColor, board: Array<Array<Piece?>>): Boolean {
        // Pawns
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        val pawnRank = square.rank - pawnDir
        if (pawnRank in 0..7) {
            for (df in listOf(-1, 1)) {
                val pf = square.file + df
                if (pf in 0..7) {
                    val p = board[pawnRank][pf]
                    if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
                }
            }
        }

        // Knights
        for ((df, dr) in KNIGHT_OFFSETS) {
            val nf = square.file + df
            val nr = square.rank + dr
            if (nf in 0..7 && nr in 0..7) {
                val p = board[nr][nf]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // Bishops and Queens (diagonal rays)
        for ((df, dr) in BISHOP_DIRECTIONS) {
            var nf = square.file + df
            var nr = square.rank + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = board[nr][nf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                nf += df
                nr += dr
            }
        }

        // Rooks and Queens (orthogonal rays)
        for ((df, dr) in ROOK_DIRECTIONS) {
            var nf = square.file + df
            var nr = square.rank + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = board[nr][nf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                nf += df
                nr += dr
            }
        }

        // King (adjacent squares)
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val nf = square.file + df
                val nr = square.rank + dr
                if (nf in 0..7 && nr in 0..7) {
                    val p = board[nr][nf]
                    if (p != null && p.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        return false
    }

    /**
     * Finds the [Square] occupied by the King of [color].
     */
    fun findKingSquare(color: PieceColor, board: Array<Array<Piece?>>): Square? {
        for (r in 0..7) {
            for (f in 0..7) {
                val p = board[r][f]
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return Square(f, r)
                }
            }
        }
        return null
    }

    /**
     * Determines whether the King of [color] is currently in check.
     */
    fun isKingInCheck(color: PieceColor, board: Array<Array<Piece?>>): Boolean {
        val kingSq = findKingSquare(color, board) ?: return false
        return isSquareAttacked(kingSq, color.opposite(), board)
    }

    /**
     * Returns all enemy squares giving check to the king of [color].
     */
    fun getCheckers(color: PieceColor, board: Array<Array<Piece?>>): List<Square> {
        val kingSq = findKingSquare(color, board) ?: return emptyList()
        val opponentColor = color.opposite()
        val checkers = mutableListOf<Square>()

        // Check pawns
        val pawnDir = if (opponentColor == PieceColor.WHITE) 1 else -1
        val pawnRank = kingSq.rank - pawnDir
        if (pawnRank in 0..7) {
            for (df in listOf(-1, 1)) {
                val pf = kingSq.file + df
                if (pf in 0..7) {
                    val p = board[pawnRank][pf]
                    if (p != null && p.color == opponentColor && p.type == PieceType.PAWN) {
                        checkers.add(Square(pf, pawnRank))
                    }
                }
            }
        }

        // Check knights
        for ((df, dr) in KNIGHT_OFFSETS) {
            val nf = kingSq.file + df
            val nr = kingSq.rank + dr
            if (nf in 0..7 && nr in 0..7) {
                val p = board[nr][nf]
                if (p != null && p.color == opponentColor && p.type == PieceType.KNIGHT) {
                    checkers.add(Square(nf, nr))
                }
            }
        }

        // Check diagonal rays (Bishop / Queen)
        for ((df, dr) in BISHOP_DIRECTIONS) {
            var nf = kingSq.file + df
            var nr = kingSq.rank + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = board[nr][nf]
                if (p != null) {
                    if (p.color == opponentColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        checkers.add(Square(nf, nr))
                    }
                    break
                }
                nf += df
                nr += dr
            }
        }

        // Check orthogonal rays (Rook / Queen)
        for ((df, dr) in ROOK_DIRECTIONS) {
            var nf = kingSq.file + df
            var nr = kingSq.rank + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = board[nr][nf]
                if (p != null) {
                    if (p.color == opponentColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        checkers.add(Square(nf, nr))
                    }
                    break
                }
                nf += df
                nr += dr
            }
        }

        return checkers
    }

    /**
     * Determines whether the King of [color] is in double check (attacked by two separate pieces).
     * In double check, the only legal response is a King move.
     */
    fun isDoubleCheck(color: PieceColor, board: Array<Array<Piece?>>): Boolean {
        return getCheckers(color, board).size >= 2
    }
}
