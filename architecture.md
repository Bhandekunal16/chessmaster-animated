# Architecture and Project Layout

Grandmaster Chess is a modern Android application built with Kotlin, Jetpack Compose, Coroutines, StateFlow, and a clean MVVM architecture with extracted domain controllers and repositories.

## High-Level Architecture

```text
MainActivity
    │
    ▼
ChessScreen (Jetpack Compose UI)
    │
    │ UI Actions / StateFlow Observation (Lifecycle-Aware via collectAsStateWithLifecycle)
    ▼
ChessViewModel (Presentation Coordination)
    ├── SettingsRepository      ──> Persisted settings & preferences (SharedPreferences)
    ├── ActiveGameRepository    ──> Versioned active match snapshots & process death survival
    ├── GameController          ──> Coordinates game state, turns, AI orchestration, moves
    │       ├── ChessEngine     ──> Rules engine, legal moves, validation, PGN
    │       ├── ChessAI         ──> Minimax move search & evaluation (Dispatchers.Default)
    │       └── GameClock       ──> Timers, active clock switching, timeouts
    ├── PieceAnimation          ──> Interpolation paths & capture impact effects
    └── ChessSoundPlayer        ──> Audio playback & haptic feedback
```

`MainActivity` initializes `ChessViewModel` with constructor dependency injection (`@JvmOverloads` for Android lifecycle factory compatibility with `by viewModels()`). `ChessScreen` observes immutable `StateFlow` streams via `collectAsStateWithLifecycle()` and emits user actions.

## Repository Layout

```text
.
├── app/
│   ├── build.gradle.kts                 # Android module configuration and dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # Application & activity declarations
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt        # Application entry point and theme host
│       │   │   ├── chess/
│       │   │   │   ├── animation/         # Piece trajectories and particle effect models
│       │   │   │   ├── audio/             # Sound player and haptic feedback
│       │   │   │   ├── domain/            # Pure Kotlin, zero-dependency chess domain layer
│       │   │   │   │   ├── history/       # RepetitionTracker, DrawRuleEvaluator
│       │   │   │   │   ├── model/         # BoardPosition, GameResult, CastlingRights
│       │   │   │   │   └── rules/         # MoveGenerator, CheckDetector, PositionValidator, GameTerminationEvaluator
│       │   │   │   ├── engine/            # Rules facade, AI, clock, and game controller
│       │   │   │   │   ├── ChessEngine.kt     # High-level facade delegating to pure domain layer, PGN
│       │   │   │   │   ├── ChessAI.kt         # AI search, evaluation, heuristics
│       │   │   │   │   ├── GameClock.kt       # Clock ticker, time controls, timeout
│       │   │   │   │   └── GameController.kt  # Game orchestration & AI job lifecycle
│       │   │   │   ├── model/             # Immutable pieces, squares, moves, GameState, GameSettings
│       │   │   │   ├── repository/        # Isolated persistence layer abstractions and implementations
│       │   │   │   │   ├── SettingsRepository.kt    # User preferences & configuration storage
│       │   │   │   │   └── ActiveGameRepository.kt  # Active match snapshots & process death survival
│       │   │   │   ├── ui/                # Compose screens, board, player panels, dialogs
│       │   │   │   │   └── piece_sets/    # Custom Canvas renderers for piece themes
│       │   │   │   └── viewmodel/         # ChessViewModel presentation layer
│       │   │   └── ui/theme/              # Material 3 colors, typography, and shapes
│       │   └── res/                       # Resource drawables, launcher icons, strings
│       └── test/                          # Comprehensive local unit tests (74 tests: JVM & Robolectric)
│           └── java/com/example/chess/
│               ├── domain/                # MoveGeneratorTest, GameTerminationTest, SpecialMovesTest, PerftTest
│               ├── engine/                # GameClockTest, GameControllerTest
│               ├── repository/            # SettingsRepositoryTest, ActiveGameRepositoryTest
│               └── viewmodel/             # ChessViewModelTest
├── gradle/
│   └── libs.versions.toml                # Gradle Version Catalog
├── build.gradle.kts                      # Root Gradle configuration
├── settings.gradle.kts                   # Included modules and repository setup
├── architecture.md                       # Architecture and project layout documentation
└── README.md                             # Application overview and feature guide
```

## Core Packages and Components

| Package | Component / Class | Responsibility |
| --- | --- | --- |
| `chess.domain.model` | `BoardPosition`, `GameResult`, `CastlingRights` | Pure Kotlin immutable position representations, active colors, castling permissions, en passant targets, move counters, and strongly-typed game outcome states. Zero Android dependencies. |
| `chess.domain.rules` | `MoveGenerator`, `CheckDetector`, `PositionValidator`, `GameTerminationEvaluator` | Authoritative rule enforcement: pseudo-legal move generation, pinned piece king safety, double checks, castling conditions, en passant validation, promotions, and termination detection (Checkmate, Stalemate, Draws). |
| `chess.domain.history` | `RepetitionTracker`, `DrawRuleEvaluator` | Position signature hashing, threefold/fivefold repetition detection, 50-move/75-move half-move evaluation, and insufficient material recognition. |
| `chess.model` | `GameState`, `Piece`, `Square`, `Move`, `GameSettings` | Core domain definitions, time controls, move descriptors, and configurable preferences. |
| `chess.engine` | `ChessEngine` | High-level engine facade providing clean backward-compatible APIs to UI and AI while delegating authoritative rule evaluation to `chess.domain`. Includes PGN generation and Perft debugging. |
| `chess.engine` | `ChessAI` | Minimax engine with alpha-beta pruning and piece-square evaluation for Novice, Intermediate, and Master levels. |
| `chess.engine` | `GameClock` | Owns timekeeping coroutines, remaining time StateFlow, active-turn switching, pause/resume, and timeout detection. |
| `chess.engine` | `GameController` | Central coordinator of game operations: handles player moves, coordinates asynchronous AI computation, manages undo/redo, restarts, and preserves state consistency. |
| `chess.repository` | `SettingsRepository` | Abstract interface and `SharedPreferencesSettingsRepository` implementation for persisting user preferences. |
| `chess.repository` | `ActiveGameRepository` | Abstract interface and `SharedPreferencesActiveGameRepository` for atomic snapshot persistence across process death. |
| `chess.repository` | `GameSnapshotSerializer` | JSON serializer, schema version validator (`CURRENT_SCHEMA_VERSION = 1`), and deterministic game state reconstructor. |
| `chess.viewmodel` | `ChessViewModel` | Presentation layer coordinator: exposes read-only StateFlows to Compose, manages square selection, dialog visibility, pawn promotion prompts, board flip orientation, animation orchestration, and sound dispatch. |
| `chess.ui` | `ChessScreen`, `ChessBoard`, `ChessPlayerPanels`, `ChessControls` | Jetpack Compose UI rendering the board, captured pieces, clocks, history, and action controls. |
| `chess.ui.piece_sets` | `ModernPieceRenderer`, `StauntonPieceRenderer`, `ThemedPieceRenderer` | High-fidelity canvas drawing implementations for all 10 piece visual styles. |
| `chess.animation` | `PieceAnimation` | Calculates smooth interpolation paths, capture velocities, and impact particle vectors. |
| `chess.audio` | `ChessSoundPlayer` | Plays procedural sound synthesis and haptic vibrations for moves, captures, checks, and game endings. |

## Separation of Responsibilities & State Flow

1. **User Interaction**:
   - The user taps a square on `ChessBoard`.
   - The UI delegates to `ChessViewModel.onSquareClicked(square)`.

2. **Presentation Handling**:
   - `ChessViewModel` computes legal moves for the selected piece and manages selection state.
   - When a valid destination is selected, the ViewModel triggers move animations via `_animatedPiece` and initiates sound/haptics.

3. **Game Orchestration (`GameController`)**:
   - The move is dispatched to `gameController.makeMove(move)`.
   - `GameController` executes the move in `ChessEngine`, toggles `GameClock` to the opponent, and updates `_gameState`.
   - If playing in Player-vs-Computer mode, `GameController` cancels any obsolete AI job and launches a structured coroutine on `Dispatchers.Default` for `ChessAI.findBestMove()`.
   - Stale AI computations are automatically discarded if the game was restarted, undone, or modified before the AI completed its search.

4. **Clocks and Timers (`GameClock`)**:
   - Operates independent ticker flows using structured coroutines.
   - Triggers timeouts deterministically and notifies `GameController` when a player's flag falls.

5. **Settings & Persistence (`SettingsRepository`)**:
   - Read/write access to user preferences (board theme, piece theme, sound, haptics, timer settings, AI level) is managed through `SettingsRepository`.
   - Storage details (`SharedPreferences`) are completely decoupled from ViewModel presentation logic.

6. **Active Game Persistence (`ActiveGameRepository`)**:
   - Automatically serializes versioned snapshots of active matches whenever a move is completed or undone.
   - Excludes ephemeral UI state (animations, piece highlights, promotion dialogs, audio effects).
   - Enforces monotonic sequence numbers (`saveSequence`) to eliminate out-of-order write race conditions.
   - On ViewModel creation or system process recreation, asynchronously loads and deterministically replays moves to restore the exact board state, turn, castling rights, and captured pieces.
   - Restores clocks in a paused state to prevent unfair time forfeiture during user absence.

## Persistence Isolation & Process Death Restoration

The application strictly separates preferences persistence from active match session persistence:

```text
┌───────────────────────────────────────┐       ┌────────────────────────────────────────┐
│          SettingsRepository           │       │          ActiveGameRepository          │
│  (Theme, Sound, Haptics, Timer, AI)   │       │   (Moves, Clocks, Redo, Game Mode)     │
└──────────────────┬────────────────────┘       └───────────────────┬────────────────────┘
                   │                                                │
                   └──────────────────┬─────────────────────────────┘
                                      ▼
                                ChessViewModel
                                      │
            ┌─────────────────────────┴──────────────────────────┐
            ▼                                                    ▼
      Durable Domain State                             Transient Presentation State
 (GameState, GameSettings)                       (SelectedSquare, Animations, Dialogs)
```

1. **Preference Persistence (`SettingsRepository`)**:
   - Exposes reactive `StateFlow<GameSettings>` with automatic persistence to `SharedPreferences`.
   - Outdated or missing preferences safely fall back to sensible defaults.
   - Mockable via `FakeSettingsRepository` in unit tests.

2. **Active Match Snapshotting (`ActiveGameRepository`)**:
   - Encapsulates snapshot serialization with `GameSnapshotSerializer` using `CURRENT_SCHEMA_VERSION = 1`.
   - Rejects corrupted or incompatible schema versions without crashing, cleanly falling back to initial game setup.
   - Writes are safeguarded by a monotonic `saveSequence` counter, ensuring older asynchronous background writes can never overwrite newer game moves.

3. **Deterministic Board State Reconstruction**:
   - Rather than storing raw or potentially desynchronized piece arrays, `ActiveGameSnapshot` stores the compact list of executed moves.
   - `GameSnapshotSerializer.reconstructGameState()` validates each move against the official chess rules from the starting position.
   - Rebuilds exact castling permissions, en passant target squares, captured piece lists for White and Black, and half-move / full-move clocks.

4. **Process Death Clock Safety**:
   - Wall-clock time elapsed while the app is killed or device is asleep is not subtracted from the player's clock to prevent instant timeouts upon returning.
   - Clocks are restored with their saved remaining seconds and placed in a paused state until the user resumes play.

5. **Constructor Dependency Injection**:
   - `ChessViewModel` provides full constructor dependency injection with default instances (`@JvmOverloads`), allowing seamless instantiation by `androidx.lifecycle.viewmodel` / `AndroidViewModelFactory` in production while accepting fakes (`FakeSettingsRepository`, `FakeActiveGameRepository`, controllable dispatchers) during local unit tests.

## Lifecycle-Aware State Flow & Presentation Boundaries

1. **Lifecycle-Aware Collection**:
   - Compose screens use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` to observe state flows.
   - When the hosting Activity is in the background or stopped, UI collection halts to conserve device resources and battery.
   - Reconnection upon Activity resume automatically receives the latest upstream `StateFlow` value without replaying stale one-time triggers.

2. **Strict Presentation Boundary**:
   - Transient visual states (such as selected squares, destination highlights, movement animation coordinates, piece defeat velocities, particle bursts, and promotion modals) reside strictly in the presentation layer and are never persisted.
   - On configuration changes (such as screen rotation) or process recreation, transient states reset safely to clean states, preventing lingering dialogs or orphaned animation frames.

## AI Concurrency & Cancellation Safety Architecture

To guarantee maximum UI responsiveness and prevent race conditions or corrupted board states during intensive engine calculations, the application enforces the following concurrency design:

1. **Dispatcher Offloading**:
   - Computationally intensive alpha-beta minimax search, iterative tree evaluation, and quiescence evaluations run exclusively on `Dispatchers.Default` via `GameController(computeDispatcher = Dispatchers.Default)`.
   - The main Android UI thread remains completely free for 60fps animations and fluid touch feedback.

2. **Cooperative Cancellation**:
   - `ChessAI.findBestMove`, `minimax`, and `quiescence` incorporate cooperative coroutine cancellation checks (`ensureActive()`) across move evaluation loops and recursion depths.
   - Any coroutine cancellation (triggered by user undo, game restart, new game, or timeout) terminates the search immediately, conserving CPU and battery resources.
   - `CancellationException` is strictly preserved and rethrown to prevent swallowing coroutine lifecycle signals.

3. **Generation Counter & Stale Result Rejection**:
   - `GameController` maintains a monotonically increasing `gameGeneration` counter.
   - Whenever a move is executed, an undo is requested, or the game is reset, `gameGeneration` increments and any existing `aiJob` is promptly cancelled.
   - Before applying any computed AI move, `GameController` validates that:
     1. The generation token captured at the start of the search still matches the current `gameGeneration`.
     2. The board is still in the exact matching position (`snapshot == _gameState.value`).
     3. The selected move is strictly legal on the current board.
   - Any obsolete or stale search result is rejected and dropped without mutating state.

4. **Conflicting Board Move Prevention**:
   - While the AI is calculating (`isThinking == true`), both the domain controller (`GameController.applyMove`) and the presentation layer (`ChessViewModel.onSquareClicked`, `ChessBoard.clickable`) reject user piece selections and destination moves.
   - Action controls like Undo, Redo, and Hint are disabled in `ChessActionBar` during active computation.

## Pure Kotlin Domain Layer (`com.example.chess.domain`)

The application isolates all chess logic, board representations, move generation, and termination evaluation into a zero-dependency pure Kotlin package:

1. **Model Encapsulation (`com.example.chess.domain.model`)**:
   - `BoardPosition`: Immutable snapshot containing the 8x8 piece array, `activeColor`, `castlingRights`, `enPassantTarget`, `halfMoveClock`, and `fullMoveNumber`. Supports FEN serialization and deserialization.
   - `CastlingRights`: Immutable record of kingside and queenside rights for White and Black.
   - `GameResult`: Strongly-typed sealed hierarchy (`Ongoing`, `Check`, `Checkmate`, `Stalemate`, `InsufficientMaterial`, `ThreefoldRepetition`, `FiftyMoveRule`, `Timeout`, `Resignation`). Exposes `isGameOver`, `isDraw`, `isCheck`, and `winner`.

2. **Authoritative Rule Enforcement (`com.example.chess.domain.rules`)**:
   - `MoveGenerator`: Differentiates pseudo-legal moves from legal moves. King safety checks verify that every generated move does not leave the active player's king in check. Handles pinned pieces along rank/file/diagonal rays, double checks (forcing king movement), castling requirements (path clear, not in check, not passing through check), en passant pawn captures (including horizontal discovery pins), and pawn promotions to all four pieces.
   - `CheckDetector`: Validates single check, double check, and square attack rays.
   - `PositionValidator`: Validates square coordinates, boundaries, and board configurations.
   - `GameTerminationEvaluator`: Evaluates final positions deterministically. Correctly prioritizes checkmate over automatic draw thresholds (FIDE 9.6.2).

3. **History & Draw Evaluation (`com.example.chess.domain.history`)**:
   - `RepetitionTracker`: Generates normalized FEN-like position signatures (board layout, active color, castling rights, and legally reachable en passant targets). Tracks threefold repetition (claimable draw) and fivefold repetition (automatic draw).
   - `DrawRuleEvaluator`: Tracks 50-move rule (100 half-moves, claimable draw) and 75-move rule (150 half-moves, automatic draw). Evaluates insufficient material conditions (K vs K, K+B vs K, K+N vs K, same-color bishops vs opposite-color bishops).

## Testing Architecture

Comprehensive local JVM unit tests (83 tests in total) cover all layers:
- **`MoveGeneratorTest`**: Verifies legal move generation across starting position, piece pins along attack rays, discovered check detection, double checks forcing king moves, and attacks preventing king escapes.
- **`SpecialMovesTest`**: Verifies castling (kingside/queenside, in check, through check, rights lost on rook capture), en passant (validity window, expiration, horizontal ray pin protection), and pawn promotion (quiet, capture, all 4 pieces).
- **`GameTerminationTest`**: Verifies Fool's Mate checkmate, classic stalemate, insufficient material variations, 50-move claimable vs 75-move automatic draws, threefold claimable vs fivefold automatic repetition, and checkmate precedence.
- **`PerftTest`**: Performance and correctness testing against canonical reference perft node counts (Starting position depths 1-4, Kiwipete depth 1-3, position 3 pins & pawns, position 4 promotions, and position 5 promotions under check).
- **`GameClockTest`**: Verifies starting, stopping, active player toggling, increment logic, pause/resume, reset, timeout events, and coroutine cancellation.
- **`GameControllerTest`**: Verifies player moves, turn transitions, AI move execution, undo/redo stacks, restart during active AI computation, rejection of stale AI moves, move rejection while thinking, and game-over transitions.
- **`SettingsRepositoryTest`**: Verifies loading defaults, persisting updates, and schema compatibility.
- **`ActiveGameRepositoryTest`**: Verifies JSON serialization round-trips, deterministic state reconstruction, schema version rejection, sequence-based out-of-order write rejection, and active game clearing.
- **`ChessViewModelTest`**: Verifies UI state flows, square selections, promotion dialog flows, board orientation toggles, factory instantiation (`AndroidViewModelFactory`), active game restoration on process recreation, and delegation to underlying controllers.
- **`ShareAppTest`**: Verifies native Android Sharesheet integration, `ACTION_SEND` intent creation, `text/plain` MIME typing, chooser creation with `FLAG_ACTIVITY_NEW_TASK`, promotional message formatting, ViewModel delegation, and Compose UI interactions in TopAppBar and Settings.
- **`ExampleUnitTest`**: Verifies core chess engine rules, checkmate detection, legal move generators, and AI search results.


