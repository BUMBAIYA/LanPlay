# LanPlay: Google Play listing

Copy each block into the matching Play Console field.

## App name (max 30)
LanPlay: Combine Math Cards

## Short description (max 80)
Build the target number from cards. Play with friends on the same Wi-Fi.

## Full description (max 4000)
LanPlay turns one Wi-Fi network or phone hotspot into a card table. No internet, no accounts, no sign-up. One player hosts a room, friends join with a 4-character room ID or a QR code, and you play together on your own phones.

COMBINE: THE FIRST GAME
Combine is a fast number game for 2 to 6 players.
- A random player draws 1 to 3 number cards. Side by side they make the target, like 67.
- Everyone builds an expression from their number cards and operator cards (+, -, x, /) that equals the target.
- Hit it first and you win the round. Get it wrong and you take a short time penalty.
- Think someone passed on a target they could have hit? Catch them and shout DHAPPA.
- First to the points goal wins the series.

PLAY YOUR WAY
- Host a room or join one in seconds.
- Join with a room ID, a QR code, or pick a nearby room from the list.
- Host settings: turn timer, penalty time, catch window, points to win.
- Solo mode: pick a target (or roll a random one) and beat the 60-second clock.
- Watch other players think in real time and peek at their cards.

BUILT FOR THE ROOM
- Works on a shared Wi-Fi or one phone's hotspot. No internet needed.
- Fun sounds, a tick in the last seconds of your turn, and a buzz when it is your turn.
- Landscape layout, flat bold art, no ads.
- Lose your connection? Rejoin the game. Leave on purpose and your seat is gone.

More games for the room are on the way.

## Category and tags
- Type: Game
- Category: Card (alternative: Puzzle)
- Tags: Card, Math, Multiplayer, Local multiplayer, Casual

## Contact details
- Website: https://github.com/BUMBAIYA/LanPlay
- Email: [your support email]
- Privacy policy URL: [host privacy-policy.md and paste the URL here, e.g. a GitHub Pages link]

## Graphics checklist
- App icon: 512 x 512 PNG (from the launcher icon artwork)
- Feature graphic: 1024 x 500 PNG
- Phone screenshots: at least 2 (landscape 16:9 is fine), ideally 4 to 8:
  1. Game list / Combine menu
  2. Lobby with several players
  3. Table on your turn
  4. Catch moment (DHAPPA)
  5. Solo run
  6. You won! result

## Content rating (IARC questionnaire answers)
- Category: Game
- Violence: none. Blood: none. Fear: none. Sexual content: none.
- Profanity or crude humour: none. Controlled substances: none.
- Gambling or simulated gambling: no. Real-money features: no.
- User-generated content shared between users: players choose a display name, shown to others in the same room only.
- Users can interact or exchange information: yes (names and moves inside a local room).
- Shares location with other users: no. Allows purchases: no.
- Expected result: low age rating (Everyone / PEGI 3 range).

## Data safety form
- Does the app collect or share user data with the developer or third parties? **No data is sent to the developer or any third party.**
- Display name: typed by the player, kept on the device, and sent over the local network only to the host and players of the same room. Not collected by the developer.
- Data encrypted in transit: not applicable (local network only, nothing leaves the network).
- Users can request data deletion: nothing is stored on any server. Uninstalling removes the name.
- No ads, no analytics, no crash reporting SDK, no account.

## App content declarations
- Ads: **No ads**
- Target audience: **13 and over** (avoids the Families programme rules)
- News app / COVID / government / financial features: No
- App access: all features available without login
- Health apps: No

## Permissions to explain
| Permission | Why |
| --- | --- |
| INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE | Open a local game server and connect to the host on the same Wi-Fi or hotspot. No internet servers are contacted. |
| CHANGE_WIFI_MULTICAST_STATE | Find nearby rooms on the local network (mDNS). |
| FOREGROUND_SERVICE + FOREGROUND_SERVICE_CONNECTED_DEVICE | Keep the hosted room alive while the host's screen is off or the app is in the background. Declare type: connectedDevice, reason: hosts a local multiplayer game server for nearby devices. |
| POST_NOTIFICATIONS | Required to show the ongoing "hosting a room" notification. |
| VIBRATE | Buzz when it is your turn and in the last seconds of the timer. |
| (cleartext traffic) | The game uses plain WebSockets on the local network only. |

QR scanning uses Google's code scanner from Play services, so the app does not request the camera permission itself.

## Release notes (first release)
First release of LanPlay with Combine: host or join a room on your Wi-Fi, or play solo against the clock.

## Release steps recap
1. Create the app in Play Console (name, default language, Game, Free).
2. Fill the declarations above, store listing, and privacy policy URL.
3. Upload LanPlay-release.aab to Internal testing, install from the Play link on a real phone.
4. If the account is a new personal account: run Closed testing with 12+ testers for 14 days, then apply for production.
5. Create the production release, add the release notes, and send for review.
