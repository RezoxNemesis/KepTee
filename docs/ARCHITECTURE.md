# KepTee architecture

## Current implementation

- `app`: Android application built with Jetpack Compose. It contains onboarding/settings surfaces and the custom Home launcher activity.
- Launcher Home: searchable All Apps drawer, device-local pinned items, pages and dock, folders, drag/drop placement, shortcuts/widgets, hidden apps, and persisted layout preferences.
- Wallpaper: Android `WallpaperService` registered with the system live-wallpaper picker. Still scenes are rendered by KepTee; motion scenes use Android's media playback pipeline.
- Wallpaper lifecycle: rendering work is reduced or stopped when the wallpaper is not visible or when supported battery-saver/reduced-motion conditions call for it. Video playback has a still-scene fallback.
- Volume Lab: adjusts supported Android audio streams from within the app.
- Notifications and Control Center: provide a test notification and shortcuts to supported Android system settings/panels. They do not replace protected system UI.

## Runtime and persistence

- Launcher layout, wallpaper choices, and performance/motion preferences are stored locally.
- Launcher layout normalization validates app keys, deduplicates placements, bounds page/item counts, and constrains supported preference values before use.
- Android system surfaces and package availability remain authoritative; the app must handle missing activities, security restrictions, and device-specific behavior gracefully.

## Platform boundaries

Ordinary third-party apps cannot silently replace Realme/Android's protected notification shade, global Quick Settings, native volume popup, boot animation, live-wallpaper destination chooser, or default-home selection. KepTee must use supported Android intents and settings flows and must not present a preview as a system-wide replacement.

Audio-stream behavior can differ by device; some devices link ringtone and notification streams or restrict changes. Motion wallpaper decoding also depends on the device media decoder, so a still fallback is required when playback cannot be prepared.

Release signing remains outside source control. Debug signing is not a production release path.

## Verification

GitHub Actions builds the debug APK and unsigned release variant, runs unit tests and Android lint, starts an Android emulator for a launch smoke test, and runs connected instrumentation where configured. The separate Maestro workflow exercises onboarding, wallpaper preview/persistence, performance controls, reset behavior, launcher opening, and app-drawer search.

Always judge a commit by its own workflow results. A prior green run does not verify a newer commit. CI/emulator success does not replace physical-device validation, especially for OEM-specific launcher behavior, wallpaper battery/thermal behavior, audio streams, and system settings transitions.

## Remaining work

- Validate the latest changes on a physical device, including default-home selection and return-to-launcher flows.
- Continue regression coverage for usage-based app sorting, drag/drop and folder persistence, process recreation, and wallpaper lifecycle transitions.
- Keep the implementation status document aligned with observed behavior and clearly distinguish verified behavior from device-dependent or unverified behavior.
