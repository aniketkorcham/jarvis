# J.A.R.V.I.S. 2.0 — Realme 12 Pro / Android 16

Kotlin Android Studio starter project, targeted at Android API 36.

## Included
- Animated Iron Man-inspired blue reactor HUD
- English / Hindi / Marathi speech recognition language selection
- Text-to-speech replies (installed voice availability varies)
- OpenAI Responses API through a separate Node.js HTTPS backend
- Phone actions: camera, dialer, SMS composer, Maps, alarm, connectivity panel, app settings, battery settings
- Quick launch: YouTube, WhatsApp, Chrome
- Optional Porcupine "Jarvis" keyword detection in a microphone foreground service

## Requirements
- Android Studio with Android SDK Platform 36
- JDK 17
- Physical Android phone for microphone / wake word testing
- Picovoice Console account and a custom Android `jarvis.ppn` keyword model
- Node.js 18+ and an HTTPS backend host for OpenAI

## Build Android app
1. Extract the ZIP and open the `JarvisAI_Realme12Pro_v2` folder in Android Studio.
2. Install Android SDK Platform 36 when prompted and sync Gradle.
3. Copy `local.properties.example` to `local.properties`.
4. Add `PICOVOICE_ACCESS_KEY=your_real_key` to `local.properties`.
5. In Picovoice Console, create a custom Android keyword model for "Jarvis". Copy the downloaded `.ppn` file to:
   `app/src/main/assets/jarvis.ppn`
   Create the `assets` directory if required. Do not rename the file.
6. Build > Build Bundle(s) / APK(s) > Build APK(s).
7. Install on your Realme 12 Pro and grant microphone and notification permissions.

## Configure OpenAI
1. Deploy the `server` folder to a trusted HTTPS Node.js host.
2. Set `OPENAI_API_KEY` in the host's secret/environment settings. Never put it in the Android app or APK.
3. Run `npm install` and `npm start`.
4. Verify `https://YOUR_HOST/health`.
5. Change `backendUrl` in `MainActivity.kt` to `https://YOUR_HOST/chat`.
6. Rebuild and reinstall the Android app.

## Wake word notes
- Start wake-word detection from the visible app using the ENABLE / DISABLE button.
- Android 14+ restricts starting microphone foreground services from the background. Start it while JARVIS is visible.
- The persistent notification is required. The wake callback brings JARVIS to the foreground and announces detection; tap TALK to speak a command.
- The app does not listen secretly, bypass lock-screen security, or auto-start microphone capture after reboot.
- Porcupine access key and custom `.ppn` model are not bundled; obtain them from Picovoice Console and follow its current licensing terms.

## Realme battery / background behavior
Realme UI may stop background services to save battery. For testing, open the phone's app battery settings and allow background activity / set battery use to unrestricted if that option is available. Menu names vary by Realme UI release. Keep the persistent JARVIS notification enabled. Android may still stop the service under memory, privacy, or system restrictions.

## Phone automation safety
- SMS opens a draft/composer; the user must review and send.
- Dialer opens with no number selected; the user chooses whether to call.
- Maps opens a map surface; it does not silently start navigation.
- Wi-Fi / Bluetooth settings opens Android's system panel; the app does not silently toggle radios.
- Alarm opens the clock UI for confirmation.
- No root, accessibility scraping, hidden APIs, payment automation, or permission bypass is used.

## Language notes
Speech recognition is provided by Android's installed speech-recognition service. TTS is provided by the installed Android text-to-speech engine. Hindi and Marathi support depends on downloaded language packs / available voices. The selected language is a preference, not a guarantee that every device's recognizer supports it.
