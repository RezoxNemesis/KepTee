# KebTee
**Your Phone, Your Style.**

KebTee is a native Android customization studio. The first vertical slice focuses on a polished dashboard and an animated, battery-conscious live wallpaper.

## Features in this starter
- Jetpack Compose + Material 3 dashboard
- Aurora live wallpaper implemented with Android's `WallpaperService`
- Wallpaper preview/apply hand-off to Android's system picker
- Dark AMOLED visual design
- CI workflow for Android build, unit tests, and lint

## Build requirements
- JDK 17
- Android SDK 35
- Gradle 8.9
- Android Studio with Kotlin/Compose support

## Build locally
If Gradle 8.9 is installed and Android SDK is configured:
```sh
gradle assembleDebug
gradle testDebugUnitTest
gradle lintDebug
```

## Platform boundaries
KebTee will use supported Android APIs and explicit user permissions. A regular app cannot guarantee replacing Realme UI's native notification shade, native volume panel, or protected system animations. Those features will be implemented as clearly labelled companion experiences where Android allows them.

## Roadmap
1. Establish reproducible CI and a working APK.
2. Expand Live Wallpaper Studio with presets and settings.
3. Add Theme Studio and wallpaper collections.
4. Explore launcher customization.
5. Add optional volume overlays and notification companion UI with explicit permissions.
6. Device testing, privacy review, and Play Store release preparation.
