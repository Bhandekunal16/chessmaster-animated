package com.example.chess.engine

import com.example.chess.domain.history.DrawRuleEvaluator
import com.example.chess.domain.history.RepetitionTracker
import com.example.chess.domain.model.BoardPosition
import com.example.chess.domain.model.GameResult
import com.example.chess.domain.rules.CheckDetector
import com.example.chess.domain.rules.GameTerminationEvaluator
import com.example.chess.domain.rules.MoveGenerator
import com.example.chess.model.*

/**
 * Facade coordinating chess operations with the pure Kotlin domain layer.
 * Preserves 100% backward compatibility for all existing controllers, models, and tests.
 */
object ChessEngine {

    fun createInitialState(): GameState {
        val initialPosition = BoardPosition.createInitial()
        val initialSignature = RepetitionTracker.computeNormalizedSignature(initialPosition)

        return GameState(
            board = initialPosition.board,
            activeColor = initialPosition.activeColor,
            castlingRights = initialPosition.castlingRights,
            enPassantTarget = initialPosition.enPassantTarget,
            halfMoveClock = initialPosition.halfMoveClock,
            fullMoveNumber = initialPosition.fullMoveNumber,
            gameResult = GameResult.Ongoing,
            positionSignatures = listOf(initialSignature)
        )
    }

    fun getLegalMoves(state: GameState, square: Square? = null): List<Move> {
        return MoveGenerator.generateLegalMoves(state.toBoardPosition(), square)
    }

    fun getPseudoLegalMoves(state: GameState, from: Square): List<Move> {
        return MoveGenerator.generatePseudoLegalMoves(state.toBoardPosition(), from)
    }

    fun isSquareAttacked(square: Square, byColor: PieceColor, board: Array<Array<Piece?>>): Boolean {
        return CheckDetector.isSquareAttacked(square, byColor, board)
    }

    fun findKingSquare(color: PieceColor, board: Array<Array<Piece?>>): Square? {
        return CheckDetector.findKingSquare(color, board)
    }

    fun isKingInCheck(color: PieceColor, board: Array<Array<Piece?>>): Boolean {
        return CheckDetector.isKingInCheck(color, board)
    }

    fun simulateMove(board: Array<Array<Piece?>>, move: Move): Array<Array<Piece?>> {
        return MoveGenerator.simulateMoveBoard(board, move)
    }

    fun applyMove(state: GameState, move: Move): GameState {
        val currentPosition = state.toBoardPosition()
        val fenBefore = currentPosition.toFen()

        // Apply move through domain MoveGenerator
        val nextPosition = MoveGenerator.applyMove(currentPosition, move)

        // Track position repetition signature
        val nextSignature = RepetitionTracker.computeNormalizedSignature(nextPosition)
        val updatedSignatures = state.positionSignatures + nextSignature

        // Evaluate authoritative domain GameResult
        val gameResult = GameTerminationEvaluator.evaluate(nextPosition, updatedSignatures)

        val isCheck = gameResult.isCheck
        val isCheckmate = gameResult.isCheckmate
        val isStalemate = gameResult is GameResult.Stalemate
        val isDraw = gameResult.isDraw
        val drawReason = if (isDraw) gameResult.description else null
        val winner = gameResult.winner

        // Build standard SAN notation
        val san = buildSan(state, move, isCheck, isCheckmate)
        val moveWithSan = move.copy(
            san = san,
            fenBefore = fenBefore,
            isCheck = isCheck,
            isCheckmate = isCheckmate
        )

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

        return GameState(
            board = nextPosition.board,
            activeColor = nextPosition.activeColor,
            castlingRights = nextPosition.castlingRights,
            enPassantTarget = nextPosition.enPassantTarget,
            halfMoveClock = nextPosition.halfMoveClock,
            fullMoveNumber = nextPosition.fullMoveNumber,
            moveHistory = state.moveHistory + moveWithSan,
            redoHistory = emptyList(),
            capturedWhite = newCapturedWhite,
            capturedBlack = newCapturedBlack,
            isCheck = isCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isDraw = isDraw,
            drawReason = drawReason,
            winner = winner,
            whiteTimeSeconds = state.whiteTimeSeconds,
            blackTimeSeconds = state.blackTimeSeconds,
            isTimerRunning = state.isTimerRunning,
            gameResult = gameResult,
            positionSignatures = updatedSignatures
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

            // Disambiguation if another piece of same type and color can move to the same target
            val candidates = getLegalMoves(state).filter {
                it.piece.type == piece.type &&
                it.piece.color == piece.color &&
                it.to == move.to &&
                it.from != move.from
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
        return DrawRuleEvaluator.isInsufficientMaterial(board)
    }

    fun toFen(state: GameState): String {
        return state.toBoardPosition().toFen()
    }

    fun fromFen(fen: String): GameState {
        val pos = BoardPosition.fromFen(fen)
        val sig = RepetitionTracker.computeNormalizedSignature(pos)
        val result = GameTerminationEvaluator.evaluate(pos, listOf(sig))

        return GameState(
            board = pos.board,
            activeColor = pos.activeColor,
            castlingRights = pos.castlingRights,
            enPassantTarget = pos.enPassantTarget,
            halfMoveClock = pos.halfMoveClock,
            fullMoveNumber = pos.fullMoveNumber,
            isCheck = result.isCheck,
            isCheckmate = result.isCheckmate,
            isStalemate = result is GameResult.Stalemate,
            isDraw = result.isDraw,
            drawReason = if (result.isDraw) result.description else null,
            winner = result.winner,
            gameResult = result,
            positionSignatures = listOf(sig)
        )
    }

    /**
     * Executes Perft (performance test) recursively down to [depth], returning total leaf node count.
     */
    fun perft(position: BoardPosition, depth: Int): Long {
        if (depth <= 0) return 1L
        val moves = MoveGenerator.generateLegalMoves(position)
        if (depth == 1) return moves.size.toLong()

        var nodes = 0L
        for (m in moves) {
            val nextPos = MoveGenerator.applyMove(position, m)
            nodes += perft(nextPos, depth - 1)
        }
        return nodes
    }

    fun perft(state: GameState, depth: Int): Long {
        return perft(state.toBoardPosition(), depth)
    }

    fun exportPgn(
        state: GameState,
        whitePlayer: String = "White",
        blackPlayer: String = "Black",
        event: String = "Casual Match"
    ): String {
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
