# GemLeap

> **GemLeap** (formerly "Pebble Stone"). Package id: `com.spore.gemleap`.

Release & publishing docs: [`docs/store-listing.md`](docs/store-listing.md) ·
[`docs/release-checklist.md`](docs/release-checklist.md)

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
app/src/main/java/com/spore/gemleap/
├─ MainActivity.kt              # Compose entry point
├─ model/
│  ├─ GameState.kt              # immutable board + peg-solitaire rules
│  ├─ GemPalette.kt             # the gemstone colour set
│  └─ GameStats.kt              # lifetime play statistics
├─ data/
│  └─ StatsRepository.kt        # persists stats via SharedPreferences
└─ ui/
   ├─ GameScreen.kt             # screen: header, status, streak, controls, stats dialog
   ├─ BoardView.kt              # Canvas board/marble rendering + tap hit-testing
   ├─ PebbleController.kt       # UI state: selection, moves, undo, hint, stats
   ├─ ColorUtils.kt             # lighten/darken helpers
   └─ theme/Theme.kt            # colours + Material3 theme
```

## Retention features

- **Daily challenge** — a date-seeded board (same for everyone that day) with a
  separate daily win streak. Every starting vacancy is solvable to one stone, so
  each day is fair. Toggle **Free / Daily** at the top.
- **Stats & streaks** — games played/won, win rate, best result, current/best
  win streak, plus daily streak — persisted across launches, shown in a dialog.
- **Hint** — highlights one legal move with a pulsing ring (always free).
- **Undo** — takes back the last jump. Players who haven't removed ads watch a
  rewarded ad first (`RewardedAdManager`); if none is ready the undo is granted free.
- **Animated jumps** — the moving stone slides into place while the jumped stone
  fades and shrinks away.
- **Sound & haptics** — a wooden "tock" + capture pop per move, win/lose cues,
  and a light haptic tick. Mute toggle in the header (persisted). Effects are
  small generated WAVs in `res/raw`, played via `SoundPool`.

Free-play boards randomise the starting vacancy and gem colours each game for
variety; the daily board is fixed per calendar day.

## Monetization (Phase 2)

Package `monetization/` wires up ads and in-app purchase:

- **Interstitial ad** (AdMob) shown at most once every `GAMES_PER_INTERSTITIAL`
  games, gated behind GDPR consent (Google UMP) and skipped once ads are removed.
- **Remove Ads** — a one-time Play Billing purchase (`remove_ads`) that disables
  interstitials. Billing is the source of truth; a cached flag gives instant UI.
  A **Restore** action re-checks ownership.

All ad ids in `monetization/AdIds.kt` are **Google's official TEST ids** and the
AdMob app id in `AndroidManifest.xml` is the sample id — safe for development.

### Before publishing / testing purchases

1. **AdMob:** create your app + an interstitial unit. Put the real app id in
   `AndroidManifest.xml` (`APPLICATION_ID` meta-data) and the real unit id in
   `AdIds.kt`, then set `AdIds.USE_TEST = false`.
2. **Play Console → Monetize → In-app products:** create a product with id
   **`remove_ads`** (matches `AdIds.PRODUCT_REMOVE_ADS`), set a price, activate it.
3. **Upload a signed build to a testing track** (Internal testing). Play Billing
   only works for apps distributed through Play — you cannot buy from a local
   debug build.
4. **License testers:** Play Console → Setup → License testing → add your Google
   account so purchases are free test transactions.
5. Install via the testing-track link with that account, then buy Remove Ads.

All game rules live in `GameState` (pure, immutable, easily testable); the UI
layer only renders it and forwards taps.

## Tech

- Kotlin 2.0.20, Jetpack Compose (BOM 2024.09.00), Material 3
- `compileSdk` 34, `minSdk` 26, `targetSdk` 34
- Android Gradle Plugin 8.5.2, Gradle 8.9
