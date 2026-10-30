# Dragon Tiger Floating Reader (Android 10+)

Experimental floating screen reader for visible Dragon/Tiger cards. It does not modify Phoenix, predict hidden cards, or guarantee outcomes.

## Phone quick start
1. Install and open **Dragon Tiger Reader**. The permission flow begins automatically.
2. Allow **Display over other apps** when Android opens Settings, then return to the reader.
3. Approve Android's **Start recording or casting** screen-capture prompt.
4. The floating reader starts automatically and the app moves to the background. Switch to Phoenix.
5. Tap the panel header (×) or use **Stop floating reader** in the app to stop it.

Android requires explicit screen-capture approval each time the reader starts. The app cannot bypass that system prompt.

## Build APK with GitHub Actions
1. Upload the contents of this project folder to the root of your GitHub repository.
2. Open **Actions** and select **Build Android APK**.
3. Select **Run workflow** if it did not start automatically after the push.
4. After a successful run, download the artifact `dragon-tiger-reader-debug-apk`.
5. Extract the artifact ZIP to get `app-debug.apk`.

## Limitations
- OCR is experimental and can misread card ranks or unrelated text. Verify each visible card.
- It reads only cards visible on screen; it cannot see hidden cards or predict random outcomes.
- Android 10 (API 29) and later are targeted. Overlay permission and screen-capture consent are required.
