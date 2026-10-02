# KepTee (KebTee on device)

A native Android customisation app with a Compose dashboard, optional home launcher and a battery-aware live wallpaper. Android 8.0 (API 26) and above. The established on-device name and package `com.rezoxnemesis.kebtee` are retained for upgrade compatibility.

Features: persisted theme accents and reduced-motion preference; wallpaper preview through Android's picker; searchable installed-app drawer with pinned favourites; in-app volume controls; test notifications with runtime permission; shortcuts to supported Android settings panels. All seven dashboard tools share one scrollable page so they remain reachable on short screens.

Android controls the global notification shade, system volume popup and default-home selection. KepTee does not require privileged access and does not claim to replace those protected surfaces. Existing silhouette artwork and adaptive launcher branding are preserved.

The app uses local preferences and platform APIs; no backend, account or paid API is required. Source is split between the dashboard, home launcher and wallpaper renderer. Device tests include startup/recreation and landscape tool navigation. Maestro covers dashboard dialogs; GitHub Actions enables KVM before emulator startup.

For device tests with a connected emulator: `./gradlew connectedDebugAndroidTest`.

## Build and verification

Requirements: a complete JDK 17 or 21, Android SDK platform 35 and build tools, and internet for the first dependency download. The Gradle 8.9 wrapper verifies its distribution checksum. No paid runtime service is required.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew assembleRelease
```

Release APKs are unsigned until a maintainer configures their own signing key outside source control. Never distribute a debug-signed APK as a production release. Keep application IDs stable to preserve upgrade compatibility. See `docs/ENGINEERING_STATUS.md` for verification evidence and remaining release gates.
