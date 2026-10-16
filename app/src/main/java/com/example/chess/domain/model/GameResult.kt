package com.example.chess.domain.model

import com.example.chess.model.PieceColor

/**
 * Authoritative, strongly-typed domain game result representing all standard chess terminations and states.
 */
sealed class GameResult {
    /**
     * The game is actively in progress.
     */
    object Ongoing : GameResult()

    /**
     * Active player is in check but has legal moves available.
     */
    data class Check(val checkedColor: PieceColor) : GameResult()

    /**
     * Active player has no legal moves and is in check.
     */
    data class Checkmate(override val winner: PieceColor) : GameResult()

    /**
     * Active player has no legal moves and is NOT in check.
     */
    object Stalemate : GameResult()

    /**
     * Neither player has enough material to deliver checkmate.
     */
    object InsufficientMaterial : GameResult()

    /**
     * The same position has occurred 3 times (claimable draw) or 5 times (mandatory automatic draw).
     */
    data class ThreefoldRepetition(
        val isClaimable: Boolean = true,
        val isAutomatic: Boolean = false
    ) : GameResult()

    /**
     * 50 full moves (100 half-moves) without a pawn move or capture (claimable)
     * or 75 full moves (150 half-moves) without pawn move or capture (mandatory automatic draw).
     */
    data class FiftyMoveRule(
        val isClaimable: Boolean = true,
        val isAutomatic: Boolean = false
    ) : GameResult()

    val isGameOver: Boolean
        get() = when (this) {
            is Ongoing, is Check -> false
            is Checkmate, Stalemate, InsufficientMaterial -> true
            is ThreefoldRepetition -> isAutomatic
            is FiftyMoveRule -> isAutomatic
        }

    val isDraw: Boolean
        get() = when (this) {
            Stalemate, InsufficientMaterial -> true
            is ThreefoldRepetition -> isAutomatic
            is FiftyMoveRule -> isAutomatic
            else -> false
        }

    val isCheckmate: Boolean
        get() = this is Checkmate

    val isCheck: Boolean
        get() = this is Check || this is Checkmate

    open val winner: PieceColor?
        get() = null

    val description: String
        get() = when (this) {
            is Ongoing -> "In progress"
            is Check -> "${checkedColor.name.lowercase().replaceFirstChar { it.uppercase() }} is in check"
            is Checkmate -> "Checkmate - ${winner.name.lowercase().replaceFirstChar { it.uppercase() }} wins"
            is Stalemate -> "Draw - Stalemate"
            is InsufficientMaterial -> "Draw - Insufficient material"
            is ThreefoldRepetition -> if (isAutomatic) "Draw - Fivefold repetition" else "Draw claimable - Threefold repetition"
            is FiftyMoveRule -> if (isAutomatic) "Draw - 75-move rule" else "Draw claimable - 50-move rule"
        }
}
