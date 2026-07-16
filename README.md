# Pebble Stone

A native Android take on **peg (marble) solitaire** — the English 33-hole cross
board — built with Kotlin and Jetpack Compose. The board and gemstone marbles are
drawn entirely in code with Compose `Canvas` (no image assets), styled after a
polished wooden solitaire set.

![board](docs/preview.svg)

## How to play

- The board starts full except for the centre hole.
- Tap a stone to select it. Legal destinations light up with an amber ring.
- Tap a highlighted hole to **jump** the stone over its neighbour; the jumped
  stone is removed.
- Keep going until no jumps remain. Finish with a single stone to win.

Controls: **Undo** steps back one move; **New Game** reshuffles the gemstones and
resets.

## Build & run

This is a standard Android Studio project.

1. Open the `PebbleStone/` folder in **Android Studio** (Koala 2024.1.1 or newer).
2. Let it sync — Android Studio provides Gradle and its bundled **JDK 17** (AGP
   8.5 requires JDK 17; don't build with a newer JDK from the command line).
3. Pick a device/emulator (API 26+) and press **Run**.

> Note: `gradle/wrapper/gradle-wrapper.jar` is not included in this scaffold. On
> first sync Android Studio regenerates it. To build from the command line
> instead, run `gradle wrapper --gradle-version 8.9` once (needs a local Gradle),
> then use `./gradlew assembleDebug`.

## Project layout

```
app/src/main/java/com/example/pebblestone/
├─ MainActivity.kt              # Compose entry point
├─ model/
│  ├─ GameState.kt              # immutable board + peg-solitaire rules
│  └─ GemPalette.kt             # the gemstone colour set
└─ ui/
   ├─ GameScreen.kt             # screen: header, status, controls, banner
   ├─ BoardView.kt              # Canvas board/marble rendering + tap hit-testing
   ├─ PebbleController.kt       # UI state: selection, moves, undo history
   ├─ ColorUtils.kt             # lighten/darken helpers
   └─ theme/Theme.kt            # colours + Material3 theme
```

All game rules live in `GameState` (pure, immutable, easily testable); the UI
layer only renders it and forwards taps.

## Tech

- Kotlin 2.0.20, Jetpack Compose (BOM 2024.09.00), Material 3
- `compileSdk` 34, `minSdk` 26, `targetSdk` 34
- Android Gradle Plugin 8.5.2, Gradle 8.9
