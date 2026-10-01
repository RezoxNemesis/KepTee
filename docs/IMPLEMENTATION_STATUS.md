# KebTee implementation status

## Working in the Android app
- Custom launcher Home screen with device-local pinned favourites and searchable All Apps drawer.
- Live wallpaper service registered with Android's wallpaper picker.
- Theme accent and reduced-motion preferences persist locally.
- Volume Lab changes supported Android audio streams from inside KebTee.
- Notifications panel includes a real test notification and a shortcut to Android notification settings.

## Android platform boundaries
- Android still owns the global system volume popup and notification shade / Quick Settings. A normal app cannot replace these system surfaces merely by drawing a custom UI. Replacing them globally would require privileged/system-level integration or other device-specific mechanisms.
- Volume Lab can adjust audio streams in-app, but some devices link ringtone and notification streams or restrict certain stream changes.
- The bundled monochrome reference artwork is now the source image for both the wallpaper service and the dashboard preview. The live service animates brightness and scale only; it no longer reconstructs the figure from shapes.
- Home screen widgets, gesture customization, icon packs, and a fully custom notification shade are not claimed as complete.

## Verification
Every pushed change is checked by the Android CI workflow: debug APK assembly, unit tests, lint, and APK artifact upload.
