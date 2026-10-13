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

### 💾 Persistent Preferences
- All game preferences (board theme, piece style, AI difficulty, timer presets, animation speed, audio, and haptics) are saved and restored across sessions via `SettingsRepository`.

## Architecture & Code Quality

- **MVVM + Clean Domain Separation**:
  - `ChessEngine`: Pure chess rules, legal move generation, validation, and PGN generation.
  - `ChessAI`: Minimax search and positional evaluation heuristics.
  - `GameClock`: Dedicated coroutine-driven timer lifecycle manager.
  - `GameController`: Coordinates turns, moves, asynchronous AI worker lifecycles, and undo/redo history.
  - `SettingsRepository`: Data layer abstraction for preference persistence.
  - `ChessViewModel`: Focused presentation coordinator exposing reactive `StateFlow`s to Jetpack Compose.
- **Tested**: Over 30 local JVM unit tests covering `GameClock`, `GameController`, `SettingsRepository`, and `ChessViewModel`.

For in-depth architectural details, see [architecture.md](architecture.md).
