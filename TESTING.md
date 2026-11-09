# Testing LanPlay on real phones

What is already proven (no phones needed): 38 unit tests cover the rules, the multiplayer engine and the
host/client over a real socket. On the emulator I also checked host -> lobby -> start -> turns with a
second (bot) player. What can ONLY be proven on real devices is listed below.

## Setup
1. Phone A (host): turn on the hotspot. Phone B (and C...): join that hotspot's Wi-Fi.
2. Install the app on every phone (`./gradlew :app:installDebug` with each phone connected).

## Checklist
- [ ] A: Games > Combine Maths > Host a game > Create room. Room ID and QR show; hotspot address looks like 192.168.x.x.
- [ ] B: Join a game > type the room ID > Join room. (Relies on mDNS discovery. If "not found", use the QR step.)
- [ ] B: Join a game > Scan QR code > scan A's QR. (First use downloads Google's scanner module; needs Play services.)
- [ ] Rooms nearby list on B's Join screen shows the room by itself.
- [ ] Lobby: both phones see each other, Ready toggles sync, admin changes settings and B sees them update.
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
- Sound is basic beeps (no audio files yet); animations: card tilt only so far.
- No app icon/branding yet, no results history.
