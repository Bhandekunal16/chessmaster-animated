# Architecture and Project Layout

Grandmaster Chess is a single-module Android application built with Kotlin, Jetpack Compose, and a presentation/state layer centered on `ChessViewModel`.

## High-level shape

```text
MainActivity
    |
    v
ChessScreen (Compose UI)
    |
    | user actions / StateFlow collection
    v
ChessViewModel
    |-- ChessEngine  -> legal moves, state transitions, game results
    |-- ChessAI      -> move search and evaluation
    |-- animation    -> moving/captured-piece and impact-effect state
    |-- audio        -> sound and haptic feedback
    |-- SharedPreferences -> persisted settings
    v
GameState, Move, Piece, and settings models
```

`MainActivity` creates the view model and renders `ChessScreen`. The screen collects view-model `StateFlow`s and renders the board, player panels, move history, controls, and dialogs. User input travels back to the view model, which coordinates rule validation, animations, AI turns, clocks, persistence, and feedback.

## Repository layout

```text
.
├── app/
│   ├── build.gradle.kts                 # Android module configuration and dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # Application/activity declaration
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt        # Compose application entry point
│       │   │   ├── chess/
│       │   │   │   ├── animation/         # Piece paths and capture-impact effects
│       │   │   │   ├── audio/             # Sound and haptic playback
│       │   │   │   ├── engine/            # Rules engine and computer opponent
│       │   │   │   ├── model/             # Immutable game, move, piece, and settings data
│       │   │   │   ├── ui/                # Screen, board, controls, dialogs, renderers
│       │   │   │   │   └── piece_sets/    # Canvas renderers for piece-set styles
│       │   │   │   └── viewmodel/         # UI state and game orchestration
│       │   │   └── ui/theme/              # App Material theme, colors, and typography
│       │   │   └── res/                   # Icons, colors, strings, XML configuration
│       │   ├── test/                      # Local unit and Robolectric tests
│       │   └── androidTest/               # Instrumented Android tests
├── gradle/
│   ├── libs.versions.toml                # Version catalog
│   └── wrapper/                          # Gradle Wrapper files
├── build.gradle.kts                      # Root build configuration
├── settings.gradle.kts                   # Included modules and repositories
├── gradlew / gradlew.bat                 # Gradle Wrapper launchers
├── build.debug.sh                        # Creates a debug keystore, then builds debug APK
├── README.md                             # Product features
└── ARCH.md                               # This document
```

## Core packages

| Package | Responsibility |
| --- | --- |
| `chess.model` | Defines the board domain: pieces, squares, moves, castling rights, `GameState`, and configurable game settings. |
| `chess.engine` | Generates and applies moves, enforces chess rules, identifies end conditions, and provides the minimax-based AI. |
| `chess.viewmodel` | Owns observable UI/game state and handles move selection, turn flow, timers, undo/redo, PGN generation, settings, and dialogs. |
| `chess.ui` | Contains Compose components for the game screen, board, player panels, action bar, settings, theme selector, and dialogs. |
| `chess.ui.piece_sets` | Draws the different visual piece styles on a Compose canvas. |
| `chess.animation` | Calculates trajectories and temporary visual state for moves, captures, castling, and impact particles. |
| `chess.audio` | Plays move/capture feedback and triggers haptics when enabled. |
| `ui.theme` | Applies the application-wide Compose color and typography theme. |

## Main game flow

1. A player taps a board square in `ChessBoard`.
2. `ChessViewModel` asks `ChessEngine` for legal moves and records the selection or chosen move.
3. The view model runs the move animation, sound, and haptic feedback, then applies the move through `ChessEngine`.
4. It evaluates the updated state for check, checkmate, stalemate, or draw conditions.
5. In player-vs-computer mode, it invokes `ChessAI` on a coroutine and animates the selected reply.
6. Updated `StateFlow` values cause `ChessScreen` and its child composables to recompose.

## State ownership

The view model is the single owner of mutable application state. Its public `StateFlow`s expose:

- game and settings data;
- selected square and legal destinations;
- animation, capture, and impact-effect state;
- AI-thinking and board-orientation state; and
- dialog visibility and pending pawn-promotion state.

Settings are stored in `SharedPreferences` and loaded when `ChessViewModel` is created. Game actions and visual feedback remain in memory for the active session.
