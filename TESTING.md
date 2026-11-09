# Testing LanPlay on real phones

What is already proven (no phones needed): the unit tests cover the rules, the imagined-bracket checker, the
multiplayer engine and the host/client over a real socket. Two emulators were also played against each other
(host, join, lobby settings, target draw, turns, live row watching, catch window, false catch, round scores).
What can ONLY be proven on real devices (hotspot, discovery, QR, haptics, sound) is listed below.

## Two emulators on one Mac (no phones)
Emulators cannot discover each other, so a debug build can connect straight to a host:
1. Start two emulators. Install the app on both (`./gradlew :app:installDebug`).
2. On the host emulator: `adb -s emulator-5554 forward tcp:8765 tcp:8765`, then host a game.
3. On the other: `adb -s emulator-5556 shell am start -n com.bumbaiya.lanplay/.MainActivity --es lanplay_join 10.0.2.2:8765 --es lanplay_name Riya`
4. Remove the forward (`adb -s emulator-5554 forward --remove-all`) before running the unit tests, or NetworkTest fails on the port.
The `lanplay_join` shortcut only works in debuggable builds.

## Setup
1. Phone A (host): turn on the hotspot. Phone B (and C...): join that hotspot's Wi-Fi.
2. Install the app on every phone (`./gradlew :app:installDebug` with each phone connected).

## Checklist
- [ ] A: Games > Combine Maths > Host a game > Create room. Room ID and QR show; hotspot address looks like 192.168.x.x.
- [ ] B: Join a game > type the room ID > Join room. (Relies on mDNS discovery. If "not found", use the QR step.)
- [ ] B: Join a game > Scan QR code > scan A's QR. (First use downloads Google's scanner module; needs Play services.)
- [ ] Rooms nearby list on B's Join screen shows the room by itself.
- [ ] Lobby: both phones see each other, Ready toggles sync. Only the admin sees Change settings; changes show on B.
- [ ] Start is disabled until 2+ players are all ready.
- [ ] A random player draws the target (1-3 cards, their choice).
- [ ] Turns: draw one card, build with drag and drop (hold a number over another to raise it), tap + / - to tilt, tap `*` to cycle its operator, Submit, Pass.
- [ ] Wrong answer: Submit locks for the penalty time. Right answer ends the round and scores.
- [ ] Pass on one phone: the other phone shows the DHAPPA catch button for the catch window; a valid catch gives points, a false one puts the catcher out.
- [ ] Turn timer runs out: counts as a pass. Timers on both phones agree.
- [ ] Phone B turns Wi-Fi off and on in a game: it should reconnect and keep its seat.
- [ ] Beep and vibration on your turn; Settings screen toggles them. Dark and light theme from Settings.
- [ ] Rotate / different phone sizes still look right.

## Known gaps
- Host must keep the app open (a foreground service keeps it alive, but battery savers can still kill it).
- If a hotspot blocks multicast, typing the room ID will not find the room. Use the QR code.
- Sound is basic beeps (no audio files yet). No results history.
- The QR scanner is Google's own scanner screen, not the camera frame drawn in the design.
