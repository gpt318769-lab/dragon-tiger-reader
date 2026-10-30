# Dragon Tiger Floating Card Reader (Android 13)

A source project for an Android overlay + user-approved MediaProjection capture + ML Kit OCR prototype.

## Status (read this first)
- Implemented in source: overlay permission flow, explicit Android screen-capture consent, foreground service, movable overlay, ML Kit text OCR, experimental rank-token parsing, Dragon/Tiger comparison, small in-memory read history.
- NOT compiled or device-tested in this environment.
- Recognition is heuristic and is not reliable enough to use for betting. It may read labels, UI text, or unrelated numbers as ranks. The coordinate regions assume a landscape Phoenix-like layout and must be calibrated against real screenshots.
- It cannot see hidden cards, predict random outcomes, control Phoenix, or place bets.
- History is in memory and resets when the service stops.
- Android system secure-window protections may prevent capture of some app content.

## Build requirements
- Android Studio (recent stable)
- JDK 17
- Android SDK Platform 35 and Build Tools
- Internet for Gradle / ML Kit dependency download

## Build APK
1. Extract this ZIP.
2. Open the `DragonTigerFloatingReader` folder in Android Studio.
3. Let Gradle sync and download dependencies.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Android Studio outputs the APK under `app/build/outputs/apk/debug/`.

## Install and use on Android 13
1. Install the debug APK.
2. Open Dragon Tiger Reader.
3. Tap "Allow display over other apps" and enable it in Android settings.
4. Return and tap "Start screen reader".
5. Accept Android's screen-capture prompt.
6. Switch to Phoenix in landscape; the overlay should appear.
7. Tap the overlay header to stop the service. You can also stop it from the app.

## Important recognition calibration
The sample Phoenix screenshot showed the two cards near the upper portion of a landscape screen. The current OCR heuristic assigns the left-side rank to Dragon and right-side rank to Tiger. If the game layout differs, OCR may be wrong. Always verify the displayed card faces manually.

## Privacy
Screen capture starts only after the user taps Start and approves Android's system prompt. Captured frames are processed locally by ML Kit; this project does not upload screenshots.
