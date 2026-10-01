# KebTee architecture

## Current
- `app`: Compose dashboard
- `wallpaper`: Android WallpaperService and animated Aurora renderer

## Planned
- `themes`: palette, presets, wallpaper themes
- `launcher`: optional launcher activity, never silently replace default home
- `notifications`: notification listener only after explicit user enablement
- `volume`: optional overlay with explicit overlay permission
- `compat`: feature detection and Realme/Android capability messaging

## Platform boundaries
Ordinary apps generally cannot replace Realme's native notification shade, native volume panel, boot animation, or protected system transitions. Never imply a mock preview is a system-wide replacement.

## Live wallpaper
- Use WallpaperService and BIND_WALLPAPER
- Stop rendering while not visible
- Keep frame rate modest and test battery/thermal impact
- Avoid network access in the rendering service
