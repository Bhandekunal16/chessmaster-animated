package com.example.chess.engine

import com.example.chess.model.*

object ChessEngine {

    fun createInitialState(): GameState {
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

        return GameState(
            board = board,
            activeColor = PieceColor.WHITE,
            castlingRights = CastlingRights(),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
    }

    fun getLegalMoves(state: GameState, square: Square? = null): List<Move> {
        val allPseudo = mutableListOf<Move>()
        val color = state.activeColor

        for (r in 0..7) {
            for (f in 0..7) {
                val piece = state.board[r][f]
                if (piece != null && piece.color == color) {
                    if (square == null || (square.file == f && square.rank == r)) {
                        allPseudo.addAll(getPseudoLegalMoves(state, Square(f, r)))
                    }
                }
            }
        }

        // Filter out moves that leave king in check
        val legalMoves = allPseudo.filter { move ->
            val simulatedBoard = simulateMove(state.board, move)
            !isKingInCheck(color, simulatedBoard)
        }

        return legalMoves
    }

    fun getPseudoLegalMoves(state: GameState, from: Square): List<Move> {
        val piece = state.board[from.rank][from.file] ?: return emptyList()
        val moves = mutableListOf<Move>()

        when (piece.type) {
            PieceType.PAWN -> getPawnMoves(state, from, piece, moves)
            PieceType.KNIGHT -> getKnightMoves(state, from, piece, moves)
            PieceType.BISHOP -> getSlidingMoves(state, from, piece, moves, BISHOP_DIRECTIONS)
            PieceType.ROOK -> getSlidingMoves(state, from, piece, moves, ROOK_DIRECTIONS)
            PieceType.QUEEN -> getSlidingMoves(state, from, piece, moves, QUEEN_DIRECTIONS)
            PieceType.KING -> getKingMoves(state, from, piece, moves)
        }

        return moves
    }

    private val BISHOP_DIRECTIONS = arrayOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
    private val ROOK_DIRECTIONS = arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
    private val QUEEN_DIRECTIONS = BISHOP_DIRECTIONS + ROOK_DIRECTIONS
    private val KNIGHT_OFFSETS = arrayOf(
        Pair(1, 2), Pair(2, 1), Pair(2, -1), Pair(1, -2),
        Pair(-1, -2), Pair(-2, -1), Pair(-2, 1), Pair(-1, 2)
    )

    private fun getPawnMoves(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
        val dir = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
        val promoRank = if (piece.color == PieceColor.WHITE) 7 else 0

        // Single forward
        val nextRank = from.rank + dir
        if (nextRank in 0..7 && state.board[nextRank][from.file] == null) {
            val to = Square(from.file, nextRank)
            if (nextRank == promoRank) {
                // Promotion
                for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    moves.add(Move(from, to, piece, promotionPiece = promo))
                }
            } else {
                moves.add(Move(from, to, piece))
            }

            // Double forward from starting rank
            val doubleRank = from.rank + 2 * dir
            if (from.rank == startRank && state.board[doubleRank][from.file] == null) {
                moves.add(Move(from, Square(from.file, doubleRank), piece))
            }
        }

        // Diagonal captures
        for (df in listOf(-1, 1)) {
            val targetFile = from.file + df
            if (targetFile in 0..7 && nextRank in 0..7) {
                val targetSquare = Square(targetFile, nextRank)
                val targetPiece = state.board[nextRank][targetFile]

                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (nextRank == promoRank) {
                        for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(Move(from, targetSquare, piece, capturedPiece = targetPiece, promotionPiece = promo))
                        }
                    } else {
                        moves.add(Move(from, targetSquare, piece, capturedPiece = targetPiece))
                    }
                } else if (state.enPassantTarget == targetSquare) {
                    // En Passant
                    val epCapturedPiece = state.board[from.rank][targetFile]
                    if (epCapturedPiece != null && epCapturedPiece.color != piece.color) {
                        moves.add(Move(from, targetSquare, piece, capturedPiece = epCapturedPiece, isEnPassant = true))
                    }
                }
            }
        }
    }

    private fun getKnightMoves(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
        for ((df, dr) in KNIGHT_OFFSETS) {
            val nf = from.file + df
            val nr = from.rank + dr
            if (nf in 0..7 && nr in 0..7) {
                val dest = Square(nf, nr)
                val target = state.board[nr][nf]
                if (target == null) {
                    moves.add(Move(from, dest, piece))
                } else if (target.color != piece.color) {
                    moves.add(Move(from, dest, piece, capturedPiece = target))
                }
            }
        }
    }

    private fun getSlidingMoves(
        state: GameState,
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
                val target = state.board[nr][nf]
                if (target == null) {
                    moves.add(Move(from, dest, piece))
                } else {
                    if (target.color != piece.color) {
                        moves.add(Move(from, dest, piece, capturedPiece = target))
                    }
                    break // Path blocked
                }
                nf += df
                nr += dr
            }
        }
    }

    private fun getKingMoves(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
        // Standard moves
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val nf = from.file + df
                val nr = from.rank + dr
                if (nf in 0..7 && nr in 0..7) {
                    val dest = Square(nf, nr)
                    val target = state.board[nr][nf]
                    if (target == null) {
                        moves.add(Move(from, dest, piece))
                    } else if (target.color != piece.color) {
                        moves.add(Move(from, dest, piece, capturedPiece = target))
                    }
                }
            }
        }

        // Castling
        val rank = if (piece.color == PieceColor.WHITE) 0 else 7
        if (from.rank == rank && from.file == 4 && !piece.hasMoved) {
            val rights = state.castlingRights
            val canKingside = if (piece.color == PieceColor.WHITE) rights.whiteKingside else rights.blackKingside
            val canQueenside = if (piece.color == PieceColor.WHITE) rights.whiteQueenside else rights.blackQueenside

            // King must not be currently in check
            if (!isKingInCheck(piece.color, state.board)) {
                // Kingside castling: squares f and g empty, not attacked
                if (canKingside && state.board[rank][5] == null && state.board[rank][6] == null) {
                    val rook = state.board[rank][7]
                    if (rook != null && rook.type == PieceType.ROOK && !rook.hasMoved) {
                        if (!isSquareAttacked(Square(5, rank), piece.color.opposite(), state.board) &&
                            !isSquareAttacked(Square(6, rank), piece.color.opposite(), state.board)) {
                            moves.add(Move(from, Square(6, rank), piece, isCastling = true, isKingsideCastle = true))
                        }
                    }
                }

                // Queenside castling: squares b, c, d empty; c and d not attacked
                if (canQueenside && state.board[rank][1] == null && state.board[rank][2] == null && state.board[rank][3] == null) {
                    val rook = state.board[rank][0]
                    if (rook != null && rook.type == PieceType.ROOK && !rook.hasMoved) {
                        if (!isSquareAttacked(Square(3, rank), piece.color.opposite(), state.board) &&
                            !isSquareAttacked(Square(2, rank), piece.color.opposite(), state.board)) {
                            moves.add(Move(from, Square(2, rank), piece, isCastling = true, isKingsideCastle = false))
                        }
                    }
                }
            }
        }
    }

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

        // Bishops and Queens (diagonal)
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

        // Rooks and Queens (orthogonal)
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

        // King
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

    fun isKingInCheck(color: PieceColor, board: Array<Array<Piece?>>): Boolean {
        val kingSq = findKingSquare(color, board) ?: return false
        return isSquareAttacked(kingSq, color.opposite(), board)
    }

    fun simulateMove(board: Array<Array<Piece?>>, move: Move): Array<Array<Piece?>> {
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

    fun applyMove(state: GameState, move: Move): GameState {
        val newBoard = simulateMove(state.board, move)
        val opponentColor = state.activeColor.opposite()
        val fenBefore = toFen(state)

        // Calculate updated castling rights
        var newRights = state.castlingRights
        if (move.piece.type == PieceType.KING) {
            newRights = newRights.copyWithout(move.piece.color, kingside = true, queenside = true)
        } else if (move.piece.type == PieceType.ROOK) {
            if (move.from == Square(0, 0)) newRights = newRights.copy(whiteQueenside = false)
            if (move.from == Square(7, 0)) newRights = newRights.copy(whiteKingside = false)
            if (move.from == Square(0, 7)) newRights = newRights.copy(blackQueenside = false)
            if (move.from == Square(7, 7)) newRights = newRights.copy(blackKingside = false)
        }

        // If opponent rook captured at corner square, remove their rights
        if (move.to == Square(0, 0)) newRights = newRights.copy(whiteQueenside = false)
        if (move.to == Square(7, 0)) newRights = newRights.copy(whiteKingside = false)
        if (move.to == Square(0, 7)) newRights = newRights.copy(blackQueenside = false)
        if (move.to == Square(7, 7)) newRights = newRights.copy(blackKingside = false)

        // Calculate en passant target
        val newEnPassant = if (move.piece.type == PieceType.PAWN && Math.abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }

        // Halfmove clock (resets on pawn advance or capture)
        val newHalfMove = if (move.piece.type == PieceType.PAWN || move.isCapture) 0 else state.halfMoveClock + 1

        // Fullmove number increments after black moves
        val newFullMove = if (state.activeColor == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        // Update captured pieces
        val newCapturedWhite = state.capturedWhite.toMutableList()
        val newCapturedBlack = state.capturedBlack.toMutableList()
        if (move.capturedPiece != null) {
            if (move.capturedPiece.color == PieceColor.WHITE) {
                newCapturedWhite.add(move.capturedPiece)
            } else {
                newCapturedBlack.add(move.capturedPiece)
            }
        }

        // Check conditions for opponent
        val isOpponentInCheck = isKingInCheck(opponentColor, newBoard)

        // Temporary next state to test legal moves for opponent
        val tempState = GameState(
            board = newBoard,
            activeColor = opponentColor,
            castlingRights = newRights,
            enPassantTarget = newEnPassant,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove
        )

        val opponentLegalMoves = getLegalMoves(tempState)
        val isCheckmate = isOpponentInCheck && opponentLegalMoves.isEmpty()
        val isStalemate = !isOpponentInCheck && opponentLegalMoves.isEmpty()

        // Insufficient material check
        val insufficientMaterial = isInsufficientMaterial(newBoard)

        // 50-move rule
        val fiftyMoveDraw = newHalfMove >= 100

        val isDraw = isStalemate || insufficientMaterial || fiftyMoveDraw
        val drawReason = when {
            isStalemate -> "Stalemate - No legal moves available"
            insufficientMaterial -> "Draw - Insufficient material to checkmate"
            fiftyMoveDraw -> "Draw - 50-move rule reached without captures or pawn moves"
            else -> null
        }

        val winner = if (isCheckmate) state.activeColor else null

        // Build SAN notation
        val san = buildSan(state, move, isOpponentInCheck, isCheckmate)
        val moveWithSan = move.copy(san = san, fenBefore = fenBefore, isCheck = isOpponentInCheck, isCheckmate = isCheckmate)

        return GameState(
            board = newBoard,
            activeColor = opponentColor,
            castlingRights = newRights,
            enPassantTarget = newEnPassant,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove,
            moveHistory = state.moveHistory + moveWithSan,
            redoHistory = emptyList(),
            capturedWhite = newCapturedWhite,
            capturedBlack = newCapturedBlack,
            isCheck = isOpponentInCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isDraw = isDraw,
            drawReason = drawReason,
            winner = winner,
            whiteTimeSeconds = state.whiteTimeSeconds,
            blackTimeSeconds = state.blackTimeSeconds,
            isTimerRunning = state.isTimerRunning
        )
    }

    private fun buildSan(state: GameState, move: Move, isCheck: Boolean, isCheckmate: Boolean): String {
        if (move.isCastling) {
            val base = if (move.isKingsideCastle) "O-O" else "O-O-O"
            return when {
                isCheckmate -> "$base#"
                isCheck -> "$base+"
                else -> base
            }
        }

        val piece = move.piece
        val sb = StringBuilder()

        if (piece.type == PieceType.PAWN) {
            if (move.isCapture) {
                sb.append('a' + move.from.file)
                sb.append('x')
            }
            sb.append(move.to.algebraic)
            if (move.promotionPiece != null) {
                sb.append("=")
                sb.append(move.promotionPiece.sanLetter)
            }
        } else {
            sb.append(piece.type.sanLetter)

            // Disambiguation if another piece of same type can move to same target
            val candidates = getLegalMoves(state).filter {
                it.piece.type == piece.type && it.piece.color == piece.color && it.to == move.to && it.from != move.from
            }

            if (candidates.isNotEmpty()) {
                val sameFile = candidates.any { it.from.file == move.from.file }
                val sameRank = candidates.any { it.from.rank == move.from.rank }

                if (!sameFile) {
                    sb.append('a' + move.from.file)
                } else if (!sameRank) {
                    sb.append(move.from.rank + 1)
                } else {
                    sb.append(move.from.algebraic)
                }
            }

            if (move.isCapture) {
                sb.append('x')
            }
            sb.append(move.to.algebraic)
        }

        if (isCheckmate) {
            sb.append('#')
        } else if (isCheck) {
            sb.append('+')
        }

        return sb.toString()
    }

    fun isInsufficientMaterial(board: Array<Array<Piece?>>): Boolean {
        var whiteKnights = 0
        var whiteBishops = 0
        var blackKnights = 0
        var blackBishops = 0

        for (r in 0..7) {
            for (f in 0..7) {
                val p = board[r][f] ?: continue
                when (p.type) {
                    PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                    PieceType.KNIGHT -> if (p.color == PieceColor.WHITE) whiteKnights++ else blackKnights++
                    PieceType.BISHOP -> if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                    PieceType.KING -> {}
                }
            }
        }

        val whiteMinors = whiteKnights + whiteBishops
        val blackMinors = blackKnights + blackBishops

        // King vs King
        if (whiteMinors == 0 && blackMinors == 0) return true

        // King + Minor vs King
        if ((whiteMinors == 1 && blackMinors == 0) || (whiteMinors == 0 && blackMinors == 1)) return true

        // King + Bishop vs King + Bishop
        if (whiteKnights == 0 && blackKnights == 0 && whiteBishops == 1 && blackBishops == 1) {
            return true
        }

        return false
    }

    fun toFen(state: GameState): String {
        val sb = StringBuilder()
        for (r in 7 downTo 0) {
            var empty = 0
            for (f in 0..7) {
                val p = state.board[r][f]
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

        sb.append(if (state.activeColor == PieceColor.WHITE) " w " else " b ")

        val rights = StringBuilder()
        if (state.castlingRights.whiteKingside) rights.append('K')
        if (state.castlingRights.whiteQueenside) rights.append('Q')
        if (state.castlingRights.blackKingside) rights.append('k')
        if (state.castlingRights.blackQueenside) rights.append('q')
        sb.append(if (rights.isEmpty()) "-" else rights.toString())

        sb.append(' ')
        sb.append(state.enPassantTarget?.algebraic ?: "-")
        sb.append(' ')
        sb.append(state.halfMoveClock)
        sb.append(' ')
        sb.append(state.fullMoveNumber)

        return sb.toString()
    }

    fun exportPgn(state: GameState, whitePlayer: String = "White", blackPlayer: String = "Black", event: String = "Casual Match"): String {
        val sb = StringBuilder()
        sb.append("[Event \"$event\"]\n")
        sb.append("[Site \"AI Studio Chess\"]\n")
        sb.append("[Date \"2026.10.10\"]\n")
        sb.append("[Round \"1\"]\n")
        sb.append("[White \"$whitePlayer\"]\n")
        sb.append("[Black \"$blackPlayer\"]\n")
        val result = when {
            state.winner == PieceColor.WHITE -> "1-0"
            state.winner == PieceColor.BLACK -> "0-1"
            state.isDraw -> "1/2-1/2"
            else -> "*"
        }
        sb.append("[Result \"$result\"]\n\n")

        val history = state.moveHistory
        var moveNum = 1
        for (i in history.indices step 2) {
            sb.append("$moveNum. ")
            sb.append(history[i].san)
            if (i + 1 < history.size) {
                sb.append(" ")
                sb.append(history[i + 1].san)
            }
            sb.append(" ")
            moveNum++
        }
        sb.append(result)

        return sb.toString()
    }
}
