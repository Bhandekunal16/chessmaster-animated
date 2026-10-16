package com.example.chess.domain.model

import com.example.chess.model.*

/**
 * Pure Kotlin immutable representation of a chessboard position.
 * Contains no Android framework, UI, or ViewModel dependencies.
 */
data class BoardPosition(
    val board: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    val activeColor: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
) {
    fun getPiece(square: Square): Piece? = board[square.rank][square.file]
    fun getPiece(file: Int, rank: Int): Piece? = board[rank][file]

    fun cloneBoard(): Array<Array<Piece?>> {
        return Array(8) { r ->
            Array(8) { f ->
                board[r][f]
            }
        }
    }

    /**
     * Converts this position to standard FEN (Forsyth–Edwards Notation).
     */
    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 7 downTo 0) {
            var empty = 0
            for (f in 0..7) {
                val p = board[r][f]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    val letter = when (p.type) {
                        PieceType.PAWN -> "p"
                        PieceType.KNIGHT -> "n"
                        PieceType.BISHOP -> "b"
                        PieceType.ROOK -> "r"
                        PieceType.QUEEN -> "q"
                        PieceType.KING -> "k"
                    }
                    sb.append(if (p.color == PieceColor.WHITE) letter.uppercase() else letter)
                }
            }
            if (empty > 0) sb.append(empty)
            if (r > 0) sb.append('/')
        }

        sb.append(if (activeColor == PieceColor.WHITE) " w " else " b ")

        val rights = StringBuilder()
        if (castlingRights.whiteKingside) rights.append('K')
        if (castlingRights.whiteQueenside) rights.append('Q')
        if (castlingRights.blackKingside) rights.append('k')
        if (castlingRights.blackQueenside) rights.append('q')
        sb.append(if (rights.isEmpty()) "-" else rights.toString())

        sb.append(' ')
        sb.append(enPassantTarget?.algebraic ?: "-")
        sb.append(' ')
        sb.append(halfMoveClock)
        sb.append(' ')
        sb.append(fullMoveNumber)

        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardPosition) return false
        if (activeColor != other.activeColor) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantTarget != other.enPassantTarget) return false
        if (halfMoveClock != other.halfMoveClock) return false
        if (fullMoveNumber != other.fullMoveNumber) return false
        for (r in 0..7) {
            for (f in 0..7) {
                if (board[r][f] != other.board[r][f]) return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = activeColor.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantTarget?.hashCode() ?: 0)
        result = 31 * result + halfMoveClock
        result = 31 * result + fullMoveNumber
        for (r in 0..7) {
            for (f in 0..7) {
                result = 31 * result + (board[r][f]?.hashCode() ?: 0)
            }
        }
        return result
    }

    companion object {
        /**
         * Creates standard initial chess position.
         */
        fun createInitial(): BoardPosition {
            val board = Array(8) { Array<Piece?>(8) { null } }

            // Place White pieces
            board[0][0] = Piece.create(PieceType.ROOK, PieceColor.WHITE)
            board[0][1] = Piece.create(PieceType.KNIGHT, PieceColor.WHITE)
            board[0][2] = Piece.create(PieceType.BISHOP, PieceColor.WHITE)
            board[0][3] = Piece.create(PieceType.QUEEN, PieceColor.WHITE)
            board[0][4] = Piece.create(PieceType.KING, PieceColor.WHITE)
            board[0][5] = Piece.create(PieceType.BISHOP, PieceColor.WHITE)
            board[0][6] = Piece.create(PieceType.KNIGHT, PieceColor.WHITE)
            board[0][7] = Piece.create(PieceType.ROOK, PieceColor.WHITE)
            for (f in 0..7) {
                board[1][f] = Piece.create(PieceType.PAWN, PieceColor.WHITE)
            }

            // Place Black pieces
            board[7][0] = Piece.create(PieceType.ROOK, PieceColor.BLACK)
            board[7][1] = Piece.create(PieceType.KNIGHT, PieceColor.BLACK)
            board[7][2] = Piece.create(PieceType.BISHOP, PieceColor.BLACK)
            board[7][3] = Piece.create(PieceType.QUEEN, PieceColor.BLACK)
            board[7][4] = Piece.create(PieceType.KING, PieceColor.BLACK)
            board[7][5] = Piece.create(PieceType.BISHOP, PieceColor.BLACK)
            board[7][6] = Piece.create(PieceType.KNIGHT, PieceColor.BLACK)
            board[7][7] = Piece.create(PieceType.ROOK, PieceColor.BLACK)
            for (f in 0..7) {
                board[6][f] = Piece.create(PieceType.PAWN, PieceColor.BLACK)
            }

            return BoardPosition(
                board = board,
                activeColor = PieceColor.WHITE,
                castlingRights = CastlingRights(),
                enPassantTarget = null,
                halfMoveClock = 0,
                fullMoveNumber = 1
            )
        }

        /**
         * Parses a standard FEN string into a [BoardPosition].
         */
        fun fromFen(fen: String): BoardPosition {
            val parts = fen.trim().split(Regex("\\s+"))
            require(parts.isNotEmpty()) { "Invalid FEN string: empty" }

            val board = Array(8) { Array<Piece?>(8) { null } }
            val rankStrings = parts[0].split('/')
            require(rankStrings.size == 8) { "Invalid FEN: must have 8 ranks, got ${rankStrings.size}" }

            for (r in 7 downTo 0) {
                val rankStr = rankStrings[7 - r]
                var f = 0
                for (ch in rankStr) {
                    if (ch.isDigit()) {
                        f += ch.digitToInt()
                    } else {
                        val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                        val type = when (ch.lowercaseChar()) {
                            'p' -> PieceType.PAWN
                            'n' -> PieceType.KNIGHT
                            'b' -> PieceType.BISHOP
                            'r' -> PieceType.ROOK
                            'q' -> PieceType.QUEEN
                            'k' -> PieceType.KING
                            else -> throw IllegalArgumentException("Unknown piece char '$ch'")
                        }
                        board[r][f] = Piece.create(type, color, hasMoved = false)
                        f++
                    }
                }
                require(f == 8) { "Rank $r in FEN does not sum to 8 squares (got $f)" }
            }

            val activeColor = if (parts.size > 1 && parts[1].lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE

            val castlingStr = if (parts.size > 2) parts[2] else "-"
            val castlingRights = CastlingRights(
                whiteKingside = castlingStr.contains('K'),
                whiteQueenside = castlingStr.contains('Q'),
                blackKingside = castlingStr.contains('k'),
                blackQueenside = castlingStr.contains('q')
            )

            val epTarget = if (parts.size > 3 && parts[3] != "-") {
                Square.fromAlgebraic(parts[3])
            } else {
                null
            }

            val halfMove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            val fullMove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

            return BoardPosition(
                board = board,
                activeColor = activeColor,
                castlingRights = castlingRights,
                enPassantTarget = epTarget,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove
            )
        }
    }
}
