package com.example.chess.model

enum class GameMode {
    PVP, // Two players on same device (Pass & Play)
    PVC, // Player vs Computer
    LOCAL_MULTIPLAYER // Local Wi-Fi Multiplayer
}

enum class AIDifficulty(val label: String, val depth: Int, val description: String) {
    EASY("Novice", 1, "Casual play, quick responses, occasional tactical oversights"),
    MEDIUM("Intermediate", 3, "Solid tactical awareness, positional evaluations"),
    HARD("Master", 4, "Deep alpha-beta search, quiescence, aggressive counterplay")
}

enum class AnimationSpeed(val label: String, val durationMultiplier: Float) {
    SLOW("Cinematic", 1.5f),
    NORMAL("Normal", 1.0f),
    FAST("Rapid", 0.6f),
    OFF("Instant", 0f)
}

enum class BoardTheme(
    val title: String,
    val subtitle: String,
    val lightSquare: Long,
    val darkSquare: Long,
    val boardBorder: Long,
    val highlightColor: Long,
    val lastMoveColor: Long,
    val checkColor: Long,
    val legalMoveColor: Long,
    val captureTargetColor: Long,
    val accentColor: Long
) {
    CLASSIC_WOOD(
        "Classic Wood",
        "Light oak & dark walnut",
        0xFFEADECA, 0xFF8C532B, 0xFF5C3317,
        0x99F59E0B, 0x66F59E0B, 0x99EF4444, 0x9910B981, 0xCCEF4444, 0xFFF59E0B
    ),
    TOURNAMENT(
        "Tournament",
        "Traditional cream & tournament green",
        0xFFEEEED2, 0xFF769656, 0xFF466332,
        0x99FACC15, 0x66FACC15, 0x99DC2626, 0x9922C55E, 0xCCEF4444, 0xFF22C55E
    ),
    MIDNIGHT(
        "Midnight",
        "Dark charcoal & muted slate",
        0xFF334155, 0xFF1E293B, 0xFF0F172A,
        0x9938BDF8, 0x6638BDF8, 0x99F43F5E, 0x9906B6D4, 0xCCF43F5E, 0xFF38BDF8
    ),
    OCEAN(
        "Ocean",
        "Soft blue & deep navy",
        0xFFD0E8F2, 0xFF3B6978, 0xFF204051,
        0x9900ADB5, 0x6600ADB5, 0x99FF5722, 0x990284C7, 0xCCFF5722, 0xFF00ADB5
    ),
    EMERALD(
        "Emerald",
        "Pale ivory & rich emerald green",
        0xFFE8EDCC, 0xFF4D7C0F, 0xFF365314,
        0x99FACC15, 0x66FACC15, 0x99DC2626, 0x9984CC16, 0xCCDC2626, 0xFF84CC16
    ),
    ROYAL_GOLD(
        "Royal Gold",
        "Warm ivory, luxurious gold & deep brown",
        0xFFFBF7EE, 0xFF92672B, 0xFF654316,
        0x99F59E0B, 0x66F59E0B, 0x99DC2626, 0x99EAB308, 0xCCEF4444, 0xFFF59E0B
    ),
    NEON_CYBER(
        "Neon Cyberpunk",
        "Glowing cyan & magenta on dark board",
        0xFF1E293B, 0xFF090D16, 0xFF06B6D4,
        0x9906B6D4, 0x6606B6D4, 0x99F43F5E, 0x99EC4899, 0xCCF43F5E, 0xFF06B6D4
    ),
    MINIMAL(
        "Minimal",
        "Neutral grayscale, clean & distraction-free",
        0xFFE2E8F0, 0xFF64748B, 0xFF334155,
        0x9994A3B8, 0x6694A3B8, 0x99EF4444, 0x9964748B, 0xCCEF4444, 0xFF94A3B8
    )
}

enum class PieceSet(val title: String, val description: String) {
    CLASSIC_STAUNTON("Classic Staunton", "Traditional tournament pieces with sculpted silhouettes"),
    MODERN_MINIMAL("Modern Minimal", "Clean, bold geometric silhouettes with precision cuts"),
    REALISTIC_3D("3D Realistic", "Multi-toned bevel gradients, gloss, and depth lighting"),
    FLAT_VECTOR("Flat Vector", "Crisp, lightweight SVG-style with sharp inner contours"),
    FANTASY("Fantasy", "Mythic ornate silhouettes, spiked crests, and flared armor"),
    NEON("Neon", "Futuristic glowing neon lines with high-contrast electric cores"),
    ROYAL("Royal", "Imperial luxury pieces with gold accents and jewel emblems"),
    PIXEL_ART("Pixel Art", "Retro 8-bit stepped pixel silhouette aesthetic"),
    WOODEN_CARVED("Handcrafted Wood", "Warm grain textures, lathed concentric contours & timber finish"),
    CYBER_MECHA("Cyber Mecha", "Armored sci-fi exoskeletons with hex plating and illuminated optics")
}

enum class TimeControl(val label: String, val totalSeconds: Int, val incrementSeconds: Int) {
    UNLIMITED("No Timer", 0, 0),
    BLITZ_5("Blitz 5 min", 300, 0),
    RAPID_10("Rapid 10 min", 600, 0),
    CLASSICAL_30("Classical 30 min", 1800, 0),
    BULLET_3("Bullet 3 min", 180, 0),
    RAPID_15_10("Rapid 15|10", 900, 10)
}

data class GameSettings(
    val mode: GameMode = GameMode.PVC,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val playerColor: PieceColor = PieceColor.WHITE,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC_WOOD,
    val pieceSet: PieceSet = PieceSet.CLASSIC_STAUNTON,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val is3DView: Boolean = false,
    val autoFlipBoard: Boolean = false,
    val timeControl: TimeControl = TimeControl.UNLIMITED,
    val syncThemeEffects: Boolean = true
)

data class GameState(
    val board: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    val activeColor: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val moveHistory: List<Move> = emptyList(),
    val redoHistory: List<Move> = emptyList(),
    val capturedWhite: List<Piece> = emptyList(),
    val capturedBlack: List<Piece> = emptyList(),
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false,
    val drawReason: String? = null,
    val winner: PieceColor? = null,
    val whiteTimeSeconds: Int = 0,
    val blackTimeSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val gameResult: com.example.chess.domain.model.GameResult = com.example.chess.domain.model.GameResult.Ongoing,
    val positionSignatures: List<String> = emptyList()
) {
    val isGameOver: Boolean
        get() = isCheckmate || isStalemate || isDraw || gameResult.isGameOver

    fun getPiece(square: Square): Piece? = board[square.rank][square.file]

    fun cloneBoard(): Array<Array<Piece?>> {
        return Array(8) { r ->
            Array(8) { f ->
                board[r][f]
            }
        }
    }

    fun toBoardPosition(): com.example.chess.domain.model.BoardPosition {
        return com.example.chess.domain.model.BoardPosition(
            board = cloneBoard(),
            activeColor = activeColor,
            castlingRights = castlingRights,
            enPassantTarget = enPassantTarget,
            halfMoveClock = halfMoveClock,
            fullMoveNumber = fullMoveNumber
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        if (activeColor != other.activeColor) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantTarget != other.enPassantTarget) return false
        if (moveHistory != other.moveHistory) return false
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
        result = 31 * result + moveHistory.hashCode()
        return result
    }
}
