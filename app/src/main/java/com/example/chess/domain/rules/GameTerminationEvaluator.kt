package com.example.chess.domain.rules

import com.example.chess.domain.history.DrawRuleEvaluator
import com.example.chess.domain.history.RepetitionTracker
import com.example.chess.domain.model.BoardPosition
import com.example.chess.domain.model.GameResult
import com.example.chess.model.*

/**
 * Pure Kotlin evaluator for game state terminations, checks, checkmates, and draws.
 */
object GameTerminationEvaluator {

    /**
     * Evaluates the authoritative [GameResult] for [position].
     *
     * @param position Current position to evaluate.
     * @param historySignatures List of normalized position signatures from the game's previous turns.
     */
    fun evaluate(
        position: BoardPosition,
        historySignatures: List<String> = emptyList()
    ): GameResult {
        val activeColor = position.activeColor
        val opponentColor = activeColor.opposite()
        val isInCheck = CheckDetector.isKingInCheck(activeColor, position.board)
        val legalMoves = MoveGenerator.generateLegalMoves(position)

        // 1. Checkmate check (takes precedence over automatic draw rules if delivered on the final move)
        if (isInCheck && legalMoves.isEmpty()) {
            return GameResult.Checkmate(winner = opponentColor)
        }

        // 2. Stalemate check
        if (!isInCheck && legalMoves.isEmpty()) {
            return GameResult.Stalemate
        }

        // 3. Insufficient material
        if (DrawRuleEvaluator.isInsufficientMaterial(position.board)) {
            return GameResult.InsufficientMaterial
        }

        // 4. Repetition evaluation
        val (isThreefoldClaimable, isFivefoldAutomatic) = if (historySignatures.isNotEmpty()) {
            RepetitionTracker.evaluateRepetition(historySignatures, position)
        } else {
            Pair(false, false)
        }

        // 5. Move counter evaluation (50-move / 75-move)
        val (is50MoveClaimable, is75MoveAutomatic) = DrawRuleEvaluator.evaluateMoveCounterDraw(position.halfMoveClock)

        // Automatic draw conditions (FIDE Rules 9.6.1 and 9.6.2)
        if (isFivefoldAutomatic) {
            return GameResult.ThreefoldRepetition(isClaimable = true, isAutomatic = true)
        }
        if (is75MoveAutomatic) {
            return GameResult.FiftyMoveRule(isClaimable = true, isAutomatic = true)
        }

        // Claimable draw conditions
        if (isThreefoldClaimable) {
            return GameResult.ThreefoldRepetition(isClaimable = true, isAutomatic = false)
        }
        if (is50MoveClaimable) {
            return GameResult.FiftyMoveRule(isClaimable = true, isAutomatic = false)
        }

        // Check condition (game continues)
        if (isInCheck) {
            return GameResult.Check(checkedColor = activeColor)
        }

        return GameResult.Ongoing
    }
}
