package com.example.chess.domain.rules

import com.example.chess.domain.model.BoardPosition
import com.example.chess.model.*

/**
 * Pure Kotlin legal move generator and move application engine.
 * Fully compliant with FIDE chess rules.
 */
object MoveGenerator {

    /**
     * Generates all strictly legal moves for the active side in [position].
     * If [from] is provided, generates only legal moves originating from that square.
     */
    fun generateLegalMoves(position: BoardPosition, from: Square? = null): List<Move> {
        val pseudoMoves = generatePseudoLegalMoves(position, from)
        val color = position.activeColor

        return pseudoMoves.filter { move ->
            val simulatedBoard = simulateMoveBoard(position.board, move)
            !CheckDetector.isKingInCheck(color, simulatedBoard)
        }
    }

    /**
     * Generates all pseudo-legal moves (moves obeying piece movement mechanics
     * without verifying if the moving side's king is left in check).
     */
    fun generatePseudoLegalMoves(position: BoardPosition, from: Square? = null): List<Move> {
        val moves = mutableListOf<Move>()
        val color = position.activeColor

        if (from != null) {
            val piece = position.getPiece(from)
            if (piece != null && piece.color == color) {
                getPseudoMovesForPiece(position, from, piece, moves)
            }
            return moves
        }

        for (r in 0..7) {
            for (f in 0..7) {
                val piece = position.board[r][f]
                if (piece != null && piece.color == color) {
                    getPseudoMovesForPiece(position, Square(f, r), piece, moves)
                }
            }
        }

        return moves
    }

    private fun getPseudoMovesForPiece(
        position: BoardPosition,
        from: Square,
        piece: Piece,
        moves: MutableList<Move>
    ) {
        when (piece.type) {
            PieceType.PAWN -> getPawnMoves(position, from, piece, moves)
            PieceType.KNIGHT -> getKnightMoves(position, from, piece, moves)
            PieceType.BISHOP -> getSlidingMoves(position, from, piece, moves, CheckDetector.BISHOP_DIRECTIONS)
            PieceType.ROOK -> getSlidingMoves(position, from, piece, moves, CheckDetector.ROOK_DIRECTIONS)
            PieceType.QUEEN -> getSlidingMoves(position, from, piece, moves, CheckDetector.QUEEN_DIRECTIONS)
            PieceType.KING -> getKingMoves(position, from, piece, moves)
        }
    }

    private fun getPawnMoves(
        position: BoardPosition,
        from: Square,
        piece: Piece,
        moves: MutableList<Move>
    ) {
        val dir = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
        val promoRank = if (piece.color == PieceColor.WHITE) 7 else 0

        // Single step forward
        val nextRank = from.rank + dir
        if (nextRank in 0..7 && position.board[nextRank][from.file] == null) {
            val to = Square(from.file, nextRank)
            if (nextRank == promoRank) {
                // Promotions (Queen, Rook, Bishop, Knight)
                for (promo in PROMOTION_PIECES) {
                    moves.add(Move(from, to, piece, promotionPiece = promo))
                }
            } else {
                moves.add(Move(from, to, piece))
            }

            // Double step forward from starting rank
            val doubleRank = from.rank + 2 * dir
            if (from.rank == startRank && position.board[doubleRank][from.file] == null) {
                moves.add(Move(from, Square(from.file, doubleRank), piece))
            }
        }

        // Diagonal captures
        for (df in listOf(-1, 1)) {
            val targetFile = from.file + df
            if (targetFile in 0..7 && nextRank in 0..7) {
                val targetSquare = Square(targetFile, nextRank)
                val targetPiece = position.board[nextRank][targetFile]

                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (nextRank == promoRank) {
                        for (promo in PROMOTION_PIECES) {
                            moves.add(
                                Move(
                                    from = from,
                                    to = targetSquare,
                                    piece = piece,
                                    capturedPiece = targetPiece,
                                    promotionPiece = promo
                                )
                            )
                        }
                    } else {
                        moves.add(
                            Move(
                                from = from,
                                to = targetSquare,
                                piece = piece,
                                capturedPiece = targetPiece
                            )
                        )
                    }
                } else if (position.enPassantTarget == targetSquare) {
                    // En Passant capture
                    val epCapturedPiece = position.board[from.rank][targetFile]
                    if (epCapturedPiece != null && epCapturedPiece.color != piece.color) {
                        moves.add(
                            Move(
                                from = from,
                                to = targetSquare,
                                piece = piece,
                                capturedPiece = epCapturedPiece,
                                isEnPassant = true
                            )
                        )
                    }
                }
            }
        }
    }

    private val PROMOTION_PIECES = listOf(
        PieceType.QUEEN,
        PieceType.ROOK,
        PieceType.BISHOP,
        PieceType.KNIGHT
    )

    private fun getKnightMoves(
        position: BoardPosition,
        from: Square,
        piece: Piece,
        moves: MutableList<Move>
    ) {
        for ((df, dr) in CheckDetector.KNIGHT_OFFSETS) {
            val nf = from.file + df
            val nr = from.rank + dr
            if (nf in 0..7 && nr in 0..7) {
                val dest = Square(nf, nr)
                val target = position.board[nr][nf]
                if (target == null) {
                    moves.add(Move(from, dest, piece))
                } else if (target.color != piece.color) {
                    moves.add(Move(from, dest, piece, capturedPiece = target))
                }
            }
        }
    }

    private fun getSlidingMoves(
        position: BoardPosition,
        from: Square,
        piece: Piece,
        moves: MutableList<Move>,
        directions: Array<Pair<Int, Int>>
    ) {
        for ((df, dr) in directions) {
            var nf = from.file + df
            var nr = from.rank + dr
            while (nf in 0..7 && nr in 0..7) {
                val dest = Square(nf, nr)
                val target = position.board[nr][nf]
                if (target == null) {
                    moves.add(Move(from, dest, piece))
                } else {
                    if (target.color != piece.color) {
                        moves.add(Move(from, dest, piece, capturedPiece = target))
                    }
                    break // Ray is blocked
                }
                nf += df
                nr += dr
            }
        }
    }

    private fun getKingMoves(
        position: BoardPosition,
        from: Square,
        piece: Piece,
        moves: MutableList<Move>
    ) {
        // Standard 1-square steps
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val nf = from.file + df
                val nr = from.rank + dr
                if (nf in 0..7 && nr in 0..7) {
                    val dest = Square(nf, nr)
                    val target = position.board[nr][nf]
                    if (target == null) {
                        moves.add(Move(from, dest, piece))
                    } else if (target.color != piece.color) {
                        moves.add(Move(from, dest, piece, capturedPiece = target))
                    }
                }
            }
        }

        // Castling moves
        val rank = if (piece.color == PieceColor.WHITE) 0 else 7
        if (from.rank == rank && from.file == 4 && !piece.hasMoved) {
            val rights = position.castlingRights
            val canKingside = if (piece.color == PieceColor.WHITE) rights.whiteKingside else rights.blackKingside
            val canQueenside = if (piece.color == PieceColor.WHITE) rights.whiteQueenside else rights.blackQueenside

            // Rule: King must not be currently in check
            if (!CheckDetector.isKingInCheck(piece.color, position.board)) {
                val opponentColor = piece.color.opposite()

                // Kingside Castling (O-O): squares f and g empty; f and g not attacked
                if (canKingside && position.board[rank][5] == null && position.board[rank][6] == null) {
                    val rook = position.board[rank][7]
                    if (rook != null && rook.type == PieceType.ROOK && rook.color == piece.color) {
                        if (!CheckDetector.isSquareAttacked(Square(5, rank), opponentColor, position.board) &&
                            !CheckDetector.isSquareAttacked(Square(6, rank), opponentColor, position.board)
                        ) {
                            moves.add(
                                Move(
                                    from = from,
                                    to = Square(6, rank),
                                    piece = piece,
                                    isCastling = true,
                                    isKingsideCastle = true
                                )
                            )
                        }
                    }
                }

                // Queenside Castling (O-O-O): squares b, c, d empty; c and d not attacked
                if (canQueenside &&
                    position.board[rank][1] == null &&
                    position.board[rank][2] == null &&
                    position.board[rank][3] == null
                ) {
                    val rook = position.board[rank][0]
                    if (rook != null && rook.type == PieceType.ROOK && rook.color == piece.color) {
                        if (!CheckDetector.isSquareAttacked(Square(3, rank), opponentColor, position.board) &&
                            !CheckDetector.isSquareAttacked(Square(2, rank), opponentColor, position.board)
                        ) {
                            moves.add(
                                Move(
                                    from = from,
                                    to = Square(2, rank),
                                    piece = piece,
                                    isCastling = true,
                                    isKingsideCastle = false
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Simulates applying [move] to [board] and returns a new 8x8 array.
     */
    fun simulateMoveBoard(board: Array<Array<Piece?>>, move: Move): Array<Array<Piece?>> {
        val newBoard = Array(8) { r -> Array(8) { f -> board[r][f] } }
        val movingPiece = move.piece.copy(hasMoved = true)

        newBoard[move.from.rank][move.from.file] = null

        if (move.isCastling) {
            val rank = move.from.rank
            if (move.isKingsideCastle) {
                newBoard[rank][6] = movingPiece
                val rook = newBoard[rank][7]?.copy(hasMoved = true)
                newBoard[rank][7] = null
                newBoard[rank][5] = rook
            } else {
                newBoard[rank][2] = movingPiece
                val rook = newBoard[rank][0]?.copy(hasMoved = true)
                newBoard[rank][0] = null
                newBoard[rank][3] = rook
            }
        } else if (move.isEnPassant) {
            newBoard[move.to.rank][move.to.file] = movingPiece
            newBoard[move.from.rank][move.to.file] = null
        } else if (move.promotionPiece != null) {
            val promoPiece = Piece.create(move.promotionPiece, move.piece.color, hasMoved = true)
            newBoard[move.to.rank][move.to.file] = promoPiece
        } else {
            newBoard[move.to.rank][move.to.file] = movingPiece
        }

        return newBoard
    }

    /**
     * Applies [move] to [position] and returns the resulting immutable [BoardPosition].
     * Updates castling rights, en passant targets, half-move clock, and full-move number.
     */
    fun applyMove(position: BoardPosition, move: Move): BoardPosition {
        val newBoard = simulateMoveBoard(position.board, move)
        val opponentColor = position.activeColor.opposite()

        // Update castling rights
        var newRights = position.castlingRights

        // King moves revoke both rights for that color
        if (move.piece.type == PieceType.KING) {
            newRights = newRights.copyWithout(move.piece.color, kingside = true, queenside = true)
        } else if (move.piece.type == PieceType.ROOK) {
            // Rook moves from original corners revoke that specific right
            if (move.from == Square(0, 0)) newRights = newRights.copy(whiteQueenside = false)
            if (move.from == Square(7, 0)) newRights = newRights.copy(whiteKingside = false)
            if (move.from == Square(0, 7)) newRights = newRights.copy(blackQueenside = false)
            if (move.from == Square(7, 7)) newRights = newRights.copy(blackKingside = false)
        }

        // If a rook is captured on its original corner square, revoke opponent's castling right
        if (move.to == Square(0, 0)) newRights = newRights.copy(whiteQueenside = false)
        if (move.to == Square(7, 0)) newRights = newRights.copy(whiteKingside = false)
        if (move.to == Square(0, 7)) newRights = newRights.copy(blackQueenside = false)
        if (move.to == Square(7, 7)) newRights = newRights.copy(blackKingside = false)

        // En passant target calculation
        val newEnPassant = if (move.piece.type == PieceType.PAWN && Math.abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }

        // Half-move clock resets on pawn moves or captures
        val newHalfMove = if (move.piece.type == PieceType.PAWN || move.isCapture) 0 else position.halfMoveClock + 1

        // Full-move counter increments after Black's move
        val newFullMove = if (position.activeColor == PieceColor.BLACK) position.fullMoveNumber + 1 else position.fullMoveNumber

        return BoardPosition(
            board = newBoard,
            activeColor = opponentColor,
            castlingRights = newRights,
            enPassantTarget = newEnPassant,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove
        )
    }
}
