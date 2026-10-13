package com.example.chess.engine

import com.example.chess.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.random.Random

object ChessAI {

    // Piece-Square Tables (oriented for White, mirrored for Black)
    private val PAWN_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_TABLE = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_TABLE = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  5,  0,  0,  5,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  0, 10, 15, 15, 10,  0,-10,
        -10,  5,  5, 15, 15,  5,  5,-10,
        -10,  5,  5,  5,  5,  5,  5,-10,
        -10,  0,  5,  0,  0,  5,  0,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
    )

    private val QUEEN_TABLE = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_MIDDLE_TABLE = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    private val KING_END_TABLE = intArrayOf(
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    )

    suspend fun findBestMove(state: GameState, difficulty: AIDifficulty): Move? {
        currentCoroutineContext().ensureActive()
        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) return null

        if (difficulty == AIDifficulty.EASY) {
            // Easy: Evaluate 1 ply, occasionally make a fun random move
            if (Random.nextFloat() < 0.25f) {
                return legalMoves.random()
            }
            return legalMoves.maxByOrNull { move ->
                val nextState = ChessEngine.applyMove(state, move)
                val score = evaluateBoard(nextState.board) * (if (state.activeColor == PieceColor.WHITE) 1 else -1)
                score + Random.nextInt(-30, 30)
            }
        }

        val depth = difficulty.depth
        val isMaximizing = state.activeColor == PieceColor.WHITE
        var bestMove: Move? = null
        var bestScore = if (isMaximizing) -1_000_000 else 1_000_000

        val orderedMoves = orderMoves(state, legalMoves)

        for (move in orderedMoves) {
            currentCoroutineContext().ensureActive()
            val nextState = ChessEngine.applyMove(state, move)
            val score = minimax(
                nextState,
                depth - 1,
                -1_000_000,
                1_000_000,
                !isMaximizing,
                difficulty == AIDifficulty.HARD
            )

            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }
        }

        return bestMove ?: legalMoves.first()
    }

    private suspend fun minimax(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        useQuiescence: Boolean
    ): Int {
        currentCoroutineContext().ensureActive()
        if (state.isGameOver) {
            return when {
                state.isCheckmate -> if (state.winner == PieceColor.WHITE) 900_000 + depth else -900_000 - depth
                state.isDraw -> 0
                else -> 0
            }
        }

        if (depth <= 0) {
            return if (useQuiescence) {
                quiescence(state, alpha, beta, 2)
            } else {
                evaluateBoard(state.board)
            }
        }

        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) {
            return if (state.isCheck) {
                if (state.activeColor == PieceColor.WHITE) -900_000 else 900_000
            } else 0
        }

        val orderedMoves = orderMoves(state, legalMoves)
        var curAlpha = alpha
        var curBeta = beta

        if (isMaximizing) {
            var maxEval = -1_000_000
            for (move in orderedMoves) {
                currentCoroutineContext().ensureActive()
                val nextState = ChessEngine.applyMove(state, move)
                val eval = minimax(nextState, depth - 1, curAlpha, curBeta, false, useQuiescence)
                maxEval = maxOf(maxEval, eval)
                curAlpha = maxOf(curAlpha, eval)
                if (curBeta <= curAlpha) break // Beta cutoff
            }
            return maxEval
        } else {
            var minEval = 1_000_000
            for (move in orderedMoves) {
                currentCoroutineContext().ensureActive()
                val nextState = ChessEngine.applyMove(state, move)
                val eval = minimax(nextState, depth - 1, curAlpha, curBeta, true, useQuiescence)
                minEval = minOf(minEval, eval)
                curBeta = minOf(curBeta, eval)
                if (curBeta <= curAlpha) break // Alpha cutoff
            }
            return minEval
        }
    }

    private suspend fun quiescence(state: GameState, alpha: Int, beta: Int, qDepth: Int): Int {
        currentCoroutineContext().ensureActive()
        val standPat = evaluateBoard(state.board)
        if (qDepth <= 0) return standPat

        val isWhite = state.activeColor == PieceColor.WHITE
        var curAlpha = alpha
        var curBeta = beta

        if (isWhite) {
            if (standPat >= curBeta) return curBeta
            if (curAlpha < standPat) curAlpha = standPat

            val captureMoves = ChessEngine.getLegalMoves(state).filter { it.isCapture }
            for (move in orderMoves(state, captureMoves)) {
                currentCoroutineContext().ensureActive()
                val nextState = ChessEngine.applyMove(state, move)
                val score = quiescence(nextState, curAlpha, curBeta, qDepth - 1)
                if (score >= curBeta) return curBeta
                if (score > curAlpha) curAlpha = score
            }
            return curAlpha
        } else {
            if (standPat <= curAlpha) return curAlpha
            if (curBeta > standPat) curBeta = standPat

            val captureMoves = ChessEngine.getLegalMoves(state).filter { it.isCapture }
            for (move in orderMoves(state, captureMoves)) {
                currentCoroutineContext().ensureActive()
                val nextState = ChessEngine.applyMove(state, move)
                val score = quiescence(nextState, curAlpha, curBeta, qDepth - 1)
                if (score <= curAlpha) return curAlpha
                if (score < curBeta) curBeta = score
            }
            return curBeta
        }
    }

    private fun orderMoves(state: GameState, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            if (move.isCapture && move.capturedPiece != null) {
                // MVV - LVA: 10 * victim - attacker
                score += (10 * move.capturedPiece.type.value) - move.piece.type.value
            }
            if (move.promotionPiece != null) {
                score += move.promotionPiece.value
            }
            if (move.isCastling) {
                score += 50
            }
            score
        }
    }

    fun evaluateBoard(board: Array<Array<Piece?>>): Int {
        var score = 0
        var totalMajorMinors = 0

        for (r in 0..7) {
            for (f in 0..7) {
                val p = board[r][f] ?: continue
                if (p.type != PieceType.PAWN && p.type != PieceType.KING) {
                    totalMajorMinors++
                }
            }
        }
        val isEndgame = totalMajorMinors <= 4

        for (r in 0..7) {
            for (f in 0..7) {
                val p = board[r][f] ?: continue
                val isWhite = p.color == PieceColor.WHITE
                val multiplier = if (isWhite) 1 else -1

                // Material
                var pieceVal = p.type.value

                // Positional PST (Index: rank 7 down to 0 for White)
                val sqIdx = if (isWhite) {
                    (7 - r) * 8 + f
                } else {
                    r * 8 + f
                }

                val pstVal = when (p.type) {
                    PieceType.PAWN -> PAWN_TABLE[sqIdx]
                    PieceType.KNIGHT -> KNIGHT_TABLE[sqIdx]
                    PieceType.BISHOP -> BISHOP_TABLE[sqIdx]
                    PieceType.ROOK -> ROOK_TABLE[sqIdx]
                    PieceType.QUEEN -> QUEEN_TABLE[sqIdx]
                    PieceType.KING -> if (isEndgame) KING_END_TABLE[sqIdx] else KING_MIDDLE_TABLE[sqIdx]
                }

                score += multiplier * (pieceVal + pstVal)
            }
        }

        return score
    }
}
