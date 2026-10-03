# KepTee implementation status

## Working in the Android app
- Custom launcher Home screen with device-local pinned favourites and a searchable All Apps drawer.
- Genuine `WallpaperService` integration registered with Android's live-wallpaper picker.
- Four wallpaper scenes: Original Silhouette, Ascension Still, Ascension Flow, and Aura Pulse.
- Still scenes use KepTee-rendered glow, particle, touch, time, charging and depth effects; motion scenes use Android's media pipeline and loop silently.
- Wallpaper scene, performance and motion preferences persist locally and survive process death.
- In-app video preview uses the same packaged motion media and falls back to a still scene when motion is intentionally reduced.
- Wallpaper rendering stops or reduces work when hidden, non-interactive, locked/dimmed, in battery saver, or reduced-motion modes.
- Volume Lab changes supported Android audio streams from inside KepTee.
- Notifications panel includes a real test notification and a shortcut to Android notification settings.
- Control Center offers one-tap access to Android's Internet/Wi-Fi, Bluetooth, brightness, volume, and notification panels.

## Android platform boundaries
- Android still owns the global system volume popup, notification shade / Quick Settings, live-wallpaper destination choices, and default-home selection. KepTee opens supported system surfaces rather than pretending to replace protected OS UI.
- Volume Lab can adjust audio streams in-app, but some devices link ringtone and notification streams or restrict certain stream changes.
- Live video wallpaper playback depends on the device's Android media decoder. If a video cannot be prepared, the wallpaper renderer falls back to a still scene instead of deliberately leaving a black surface.
- Release signing remains external to source control; debug signing is not a production release path.

## Verification
GitHub Actions builds debug and unsigned release variants, runs unit tests and Android lint, launches the app on an emulator, and runs connected Android instrumentation. The Maestro workflow separately exercises onboarding, wallpaper video preview and persistence, performance controls, reset behaviour, launcher opening, and app-drawer search.

The latest fully observed green verification before subsequent hardening commits was Android CI #145 and KepTee Maestro Android UI Tests #72 on commit `56ed4f58284e17aeaf8b4cb92d4030ec9c90b63a`. Any newer head should be judged by its own checks.
