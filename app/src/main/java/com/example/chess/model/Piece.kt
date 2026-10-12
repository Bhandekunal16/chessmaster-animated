package com.example.chess.model

enum class PieceType(val symbol: String, val sanLetter: String, val value: Int) {
    PAWN("P", "", 100),
    KNIGHT("N", "N", 320),
    BISHOP("B", "B", 330),
    ROOK("R", "R", 500),
    QUEEN("Q", "Q", 900),
    KING("K", "K", 20000)
}

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Square out of bounds: file=$file, rank=$rank" }
    }

    val algebraic: String
        get() = "${('a' + file)}${rank + 1}"

    companion object {
        fun fromAlgebraic(alg: String): Square? {
            if (alg.length != 2) return null
            val f = alg[0].lowercaseChar() - 'a'
            val r = alg[1] - '1'
            return if (f in 0..7 && r in 0..7) Square(f, r) else null
        }
    }
}

data class Piece(
    val id: String,
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    companion object {
        private var idCounter = 0
        fun create(type: PieceType, color: PieceColor, hasMoved: Boolean = false): Piece {
            return Piece(
                id = "${color.name.first()}_${type.name}_${++idCounter}",
                type = type,
                color = color,
                hasMoved = hasMoved
            )
        }
    }
}
