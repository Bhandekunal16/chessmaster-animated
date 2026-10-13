# Architecture and Project Layout

Grandmaster Chess is a modern Android application built with Kotlin, Jetpack Compose, Coroutines, StateFlow, and a clean MVVM architecture with extracted domain controllers and repositories.

## High-Level Architecture

```text
MainActivity
    │
    ▼
ChessScreen (Jetpack Compose UI)
    │
    │ UI Actions / StateFlow Observation
    ▼
ChessViewModel (Presentation Coordination)
    ├── SettingsRepository   ──> Persisted settings & preferences
    ├── GameController       ──> Coordinates game state, turns, AI orchestration, moves
    │       ├── ChessEngine  ──> Rules engine, legal moves, validation, PGN
    │       ├── ChessAI      ──> Minimax move search & evaluation
    │       └── GameClock    ──> Timers, active clock switching, timeouts
    ├── PieceAnimation       ──> Interpolation paths & capture impact effects
    └── ChessSoundPlayer     ──> Audio playback & haptic feedback
```

`MainActivity` initializes `ChessViewModel` with constructor dependency injection (`@JvmOverloads` for Android lifecycle factory compatibility). `ChessScreen` observes immutable `StateFlow` streams and emits user actions.

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
│       │   │   │   ├── engine/            # Rules, AI, clock, and game controller
│       │   │   │   │   ├── ChessEngine.kt     # Legal moves, board state, rules, PGN
│       │   │   │   │   ├── ChessAI.kt         # AI search, evaluation, heuristics
│       │   │   │   │   ├── GameClock.kt       # Clock ticker, time controls, timeout
│       │   │   │   │   └── GameController.kt  # Game orchestration & AI job lifecycle
│       │   │   │   ├── model/             # Immutable pieces, squares, moves, GameState, GameSettings
│       │   │   │   ├── repository/        # SettingsRepository & SharedPreferences implementation
│       │   │   │   │   └── SettingsRepository.kt
│       │   │   │   ├── ui/                # Compose screens, board, player panels, dialogs
│       │   │   │   │   └── piece_sets/    # Custom Canvas renderers for piece themes
│       │   │   │   └── viewmodel/         # ChessViewModel presentation layer
│       │   │   └── ui/theme/              # Material 3 colors, typography, and shapes
│       │   └── res/                       # Resource drawables, launcher icons, strings
│       └── test/                          # Comprehensive local unit tests (Robolectric / JUnit 4)
│           └── java/com/example/chess/
│               ├── engine/                # GameClockTest, GameControllerTest
│               ├── repository/            # SettingsRepositoryTest
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
| `chess.model` | `GameState`, `Piece`, `Square`, `Move`, `GameSettings` | Immutable board domain definitions, time controls, and configurable preferences. |
| `chess.engine` | `ChessEngine` | Generates pseudo-legal/legal moves, enforces chess rules, handles castling, en passant, promotions, checks, and game termination conditions. |
| `chess.engine` | `ChessAI` | Minimax engine with alpha-beta pruning and piece-square evaluation for Novice, Intermediate, and Master levels. |
| `chess.engine` | `GameClock` | Owns timekeeping coroutines, remaining time StateFlow, active-turn switching, pause/resume, and timeout detection. |
| `chess.engine` | `GameController` | Central coordinator of game operations: handles player moves, coordinates asynchronous AI computation, manages undo/redo, restarts, and preserves state consistency. |
| `chess.repository` | `SettingsRepository` | Abstract interface and `SharedPreferencesSettingsRepository` implementation for persisting user preferences. |
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

## Testing Architecture

Comprehensive local JVM unit tests are organized by layer:
- **`GameClockTest`**: Verifies starting, stopping, active player toggling, increment logic, pause/resume, reset, timeout events, and coroutine cancellation.
- **`GameControllerTest`**: Verifies player moves, turn transitions, AI move execution, undo/redo stacks, restart during active AI computation, rejection of stale AI moves, and game-over transitions.
- **`SettingsRepositoryTest`**: Verifies loading defaults, persisting updates, and schema compatibility.
- **`ChessViewModelTest`**: Verifies UI state flows, square selections, promotion dialog flows, board orientation toggles, factory instantiation (`AndroidViewModelFactory`), and delegation to underlying controllers.
