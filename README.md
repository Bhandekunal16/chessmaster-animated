# Grandmaster Chess

A modern, full-featured Android chess game built with Kotlin, Jetpack Compose, Material Design 3, and a clean, testable MVVM architecture.

## Features

### ♟️ Complete Chess Rules & Game Engine
- Full chess rules compliance: legal move validation, checks, checkmates, stalemates, fifty-move rule, threefold repetition, and draw detection.
- Special move execution: castling (kingside & queenside), en passant captures, and pawn promotion dialog with Queen, Rook, Bishop, or Knight selection.
- Move history tracking, move notation, and PGN export with one-tap clipboard copy.
- Undo and redo support with state stack restoration.
- Board orientation flip toggle to play from either White or Black perspective.
- In-game AI hints powered by the chess engine evaluation.

### 🤖 Intelligent AI Opponent
- Play locally against a friend (Player vs Player) or challenge the built-in AI (Player vs Computer).
- Three selectable difficulty levels: Novice, Intermediate, and Master with alpha-beta minimax search and piece-square positional heuristics.
- Asynchronous AI calculation running safely off the UI thread (`Dispatchers.Default`) with cooperative cancellation checks (`ensureActive()`).
- Race condition protection with state-generation tokens, stale move rejection, and board interaction locking during engine calculation.

### ⏱️ Dynamic Game Clocks & Time Controls
- Multiple customizable time control modes:
  - **No Timer** (Unlimited casual play)
  - **Bullet** (1 min / 2 min)
  - **Blitz** (3 min / 5 min)
  - **Rapid** (10 min / 15 min)
  - **Classical** (30 min)
  - **Custom Increments** (e.g. 3 min + 2s, 5 min + 3s, 10 min + 5s)
- Automatic turn-based clock switching and flag-fall timeout detection.

### 🎨 Customizable Themes & Visual Piece Sets
- **8 Board Themes**: Classic Wood, Emerald Green, Midnight Blue, Charcoal Minimal, Regal Purple, Sunset Amber, Rose Quartz, and Cyber Neon.
- **10 Unique Piece Styles**:
  - **Classic Staunton**: Traditional tournament wooden piece aesthetics.
  - **Modern Vector**: Clean, contemporary geometric silhouettes.
  - **Minimalist**: Subtle, elegant outline representations.
  - **Fantasy**: Mystical RPG-themed pieces with ornate crowns and helms.
  - **Cyber Neon**: Vibrant luminous glowing futuristic vectors.
  - **Retro Pixel**: Nostalgic 8-bit arcade style piece designs.
  - **Isometric 3D**: Elevated perspective with shaded depth.
  - **Gothic**: Bold medieval fortress and iron aesthetics.
  - **Glass Crystal**: Sleek translucent frosted facets.
  - **Monochrome**: High-contrast, sharp black & white minimalism.
- Live theme preview selector dialog.

### ✨ Rich Visual Animations & Sound Effects
- Smooth piece movement animations with customizable speeds: Cinematic, Normal, Rapid, or Instant.
- Animated capture impact effects and particle bursts.
- Procedural audio feedback for standard moves, captures, checks, castling, and game results.
- Tactile haptic feedback for user interactions and key game events.

### 💾 Isolated Persistence & Process Death Survival
- **Preference Persistence (`SettingsRepository`)**: All user preferences (board theme, piece style, AI difficulty, timer presets, animation speed, audio, and haptics) are completely decoupled from UI logic and stored persistently.
- **Active Game Session Survival (`ActiveGameRepository`)**: Matches automatically survive app backgrounding and Android process death. The full board position, active turn, move history, redo stack, and clock times are restored seamlessly upon relaunch.
- **Deterministic State Reconstruction**: Replays validated move history from the initial layout to accurately reconstruct exact castling rights, en passant availability, captured pieces, and half/full-move counts.
- **Concurrency & Monotonic Sequence Protection**: Monotonically increasing sequence counters (`saveSequence`) prevent out-of-order asynchronous snapshot writes from overwriting newer state.
- **Process Death Clock Safety**: Timers are safely restored in a paused state, preventing unfair timeouts or flag falls while the application was inactive.

## Architecture & Code Quality

- **Pure Kotlin Domain Layer (`com.example.chess.domain`)**:
  - `model/`: Immutable `BoardPosition`, `GameResult`, `CastlingRights`.
  - `rules/`: Authoritative `MoveGenerator` (distinguishing pseudo-legal from fully legal moves with king safety ray checks), `CheckDetector`, `PositionValidator`, and `GameTerminationEvaluator` (checkmate, stalemate, draws).
  - `history/`: `RepetitionTracker` (normalized position signatures, threefold claimable vs fivefold automatic) and `DrawRuleEvaluator` (insufficient material, 50-move claimable vs 75-move automatic).
  - Zero Android dependencies, 100% deterministic, and shared across AI, game orchestration, and UI.
- **MVVM + Clean Layer Separation**:
  - `ChessEngine`: Facade providing backward-compatible APIs while delegating rule enforcement to `com.example.chess.domain`. Includes PGN generation.
  - `ChessAI`: Alpha-beta minimax search with iterative deepening and positional heuristics on `Dispatchers.Default`.
  - `GameClock`: Dedicated coroutine-driven timer lifecycle manager with pause/resume and active turn toggling.
  - `GameController`: Coordinates turns, moves, asynchronous AI worker lifecycles, and undo/redo history.
  - `SettingsRepository`: Data layer abstraction for preference persistence.
  - `ActiveGameRepository`: Dedicated data layer abstraction for active match session persistence, move history serialization, and process death state reconstruction.
  - `ChessViewModel`: Focused presentation coordinator exposing reactive `StateFlow`s to Jetpack Compose.
- **Lifecycle-Aware UI Observation**:
  - Jetpack Compose screens observe all ViewModel flows via `collectAsStateWithLifecycle()`, guaranteeing that UI collection pauses when the app is stopped or in the background to prevent CPU and battery waste.
  - Strict separation between durable domain data and transient UI-only state (animations, selections, promotion prompts, sound effects).
- **Extensively Tested (88 tests)**:
  - Domain tests: `MoveGeneratorTest`, `SpecialMovesTest`, `GameTerminationTest`, `PerftTest` (canonical node counts for initial position & Kiwipete).
  - Application & Integration tests: `GameClockTest`, `GameControllerTest`, `SettingsRepositoryTest`, `ActiveGameRepositoryTest`, `ChessViewModelTest`, `ShareAppTest`, `GrandmasterTopAppBarTest` (Robolectric & Compose UI interaction tests).
- **Responsive Top App Bar (`GrandmasterTopAppBar`)**:
  - Material 3 responsive toolbar built with dynamic `BoxWithConstraints` and `Modifier.weight()` layout.
  - Compact Difficulty Selector with dropdown menu for switching AI levels (Novice, Intermediate, Master) or 2-Player mode without text wrapping.
  - Graceful single-line title handling and evenly distributed, vertically aligned action buttons with accessible 48dp touch targets.
- **Native Android Sharesheet (`ShareAppHelper`)**:
  - Implements implicit `Intent.ACTION_SEND` with `Intent.createChooser()` for sharing Grandmaster Chess across all compatible installed apps (WhatsApp, Telegram, Gmail, Messages, Quick Share, etc.).
  - Accessible via the TopAppBar Share action button and the dedicated Game Settings card.

For in-depth architectural details, see [architecture.md](architecture.md).


