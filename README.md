# LanPlay

A native Android app for playing card games with friends over the same Wi-Fi or phone hotspot. No internet, accounts or servers needed: one phone hosts, the others join.

The first game is **Combine Maths** ("Dhappa"): everyone holds number and operator cards, a target number is drawn, and the first player to build an expression that hits it wins the round. A player who passes can be caught by someone who spots the answer.

## Features

- 2 to 6 players per room, or a solo mode
- Join by room ID (found automatically on the network), by scanning the host's QR code, or from a nearby-rooms list
- Lobby with ready toggles and admin-controlled settings (turn timer, catch window, scoring, live result)
- Admin can remove players; reconnecting players reclaim their seat
- Everyone sees the current player's row being built live
- Draws lean toward cards that keep the target reachable
- Sound, vibration on your turn, light and dark themes

## How it works

One phone is the host and runs the only copy of the rules. Players send requests, the host checks them and broadcasts the new state, so phones cannot get out of sync or cheat.

```
UI (Compose) -> Session -> GameClient <-WebSocket-> HostServer -> GameEngine -> new GameState -> everyone
```

| Package | What it does |
|---|---|
| `game/combinemaths` | Rules: `GameEngine` (pure referee), `Expression` parser, `Rational` maths, `Solver` |
| `net` | `HostServer` (Ktor WebSocket), `GameClient`, protocol messages, QR join links |
| `platform` | `Session`, Wi-Fi address and binding, mDNS room discovery, foreground service, feedback, settings |
| `ui` | Jetpack Compose screens, theme, card components, the expression builder |

## Tech

Kotlin, Jetpack Compose, Ktor (CIO) WebSockets, kotlinx.serialization, Android NSD (mDNS), ZXing, Google code scanner. minSdk 26.

## Build and run

Requirements: Android Studio with its bundled JDK, and an Android SDK.

```bash
./gradlew :app:installDebug      # install on a connected phone or emulator
./gradlew :app:testDebugUnitTest # run unit tests
./gradlew :app:assembleRelease   # signed release APK (needs keystore.properties)
```

Release signing reads `keystore.properties` in the project root (git-ignored). Without it the release build is unsigned.

## Playing

1. Phone A: turn on its hotspot (or use a shared Wi-Fi). Phones B, C join the same network.
2. Install the app on every phone.
3. A: Games, Combine Maths, Host a game. B: type the room ID or scan the QR.
4. Everyone taps Ready, the admin starts.

If joining fails on a home router, check that the router's client or AP isolation is off, or use one phone's hotspot instead.

See [TESTING.md](TESTING.md) for a real-phone checklist and how to test with two emulators.

## Tests

JVM unit tests cover the expression rules, the game engine, the smart draw, and a real host with bot clients over a socket.
