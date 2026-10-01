# KepTee Complete Android Customization Design

Date: 2026-10-02
Status: Written spec complete; awaiting user review before implementation-plan handoff.

## 1. Goal

Turn KepTee from its current starter customization app into a polished, production-oriented Android customization studio with a cohesive visual system, a real built-in live-wallpaper library, a capable optional launcher, usable in-app/system companion controls for volume and notifications, resilient preferences, and green automated verification.

The experience should feel like one product rather than separate demo screens.

## 2. Current baseline

Repository: https://github.com/RezoxNemesis/KepTee

Current main commit: 1f2050321c75c7006ac4308449e3c9d748a0f3f5

Existing capabilities observed in main:
- Jetpack Compose + Material 3 dashboard
- native WallpaperService
- one bundled silhouette wallpaper asset
- continuous breathing/pulse animation
- theme accent persistence
- reduced-motion preference
- custom optional HomeLauncherActivity
- searchable app drawer and pinned favourites
- notification permission/test notification flow
- Android notification/settings shortcuts
- in-app volume controls
- Android settings panel shortcuts
- Android CI with debug build, unit tests, lint, emulator startup smoke checks and diagnostic artifacts

Current limitation:
- the live wallpaper system is effectively one scene and one source image;
- the visual system is not yet a complete customization studio;
- global protected system surfaces remain outside normal third-party app privileges.

## 3. Product principles

1. One visual language across dashboard, wallpaper studio, launcher, controls and dialogs.
2. Motion should be continuous, smooth and intentional; avoid abrupt restarts or visible loop seams.
3. Battery-aware rendering: pause while wallpaper is not visible; target efficient frame pacing; avoid network work in wallpaper services.
4. Built-in content works offline.
5. User data and preferences survive process death.
6. Features must degrade clearly when Android or device capabilities do not permit them.
7. Never present an in-app simulation as if it were a replacement for a protected Android system surface.
8. Every major behavior has deterministic tests where practical and CI verification.
9. Use only free capabilities of connected development services; do not purchase credits or trigger paid operations.

## 4. Live Wallpaper Studio

### 4.1 Built-in content target

Ship 30 polished built-in live wallpapers in v1, with the architecture capable of expanding to 50 without rewriting the renderer.

The 30 v1 scenes should be grouped into:
- Aurora / atmospheric
- Neon / cyber
- Fluid / liquid
- Geometry / minimal
- Space / celestial
- Nature-inspired abstract
- AMOLED / low-light
- Particle / kinetic

The user should be able to browse them as a gallery with:
- preview image
- title
- category
- motion style
- estimated battery profile
- AMOLED-friendly indicator
- selected/applied state

The assets are shipped directly with the application. The preferred implementation is compact scene definitions and procedural/vector rendering with small local preview assets rather than dozens of large looping videos. This keeps app size and runtime memory under control while preserving high-quality full-screen rendering.

### 4.2 Renderer

Create a reusable wallpaper-rendering engine with:
- scene registry
- frame clock
- continuous phase-based animation
- device-independent scaling
- portrait/landscape safe composition
- configurable speed/intensity
- brightness/contrast handling
- reduced-motion mode
- pause/resume on visibility
- deterministic scene seeds for repeatable previews

Supported visual primitives can include:
- gradients
- particle fields
- waves
- orbiting shapes
- glow layers
- noise-like motion approximations
- parallax layers
- slow camera transforms

Each scene must expose a stable specification rather than bespoke service logic.

### 4.3 Continuity

Animations must loop mathematically, not by restarting a video texture. Periodic functions should meet at the loop boundary for position and key visual parameters. Transitions between preview and applied state must not create obvious jumps.

### 4.4 Wallpaper controls

The wallpaper studio should expose:
- apply
- preview
- speed
- intensity
- color/variant where supported
- reduced motion
- battery mode
- favorite
- recently used

Settings must persist per wallpaper.

## 5. Visual design system

Create a single design system for KepTee.

### 5.1 Dashboard
- refined hero
- clearer hierarchy
- compact status indicators
- premium card surfaces
- restrained accent glow
- consistent icon language
- large touch targets
- responsive spacing for small/large phones

### 5.2 Navigation
Use a consistent primary navigation structure for:
- Home
- Wallpapers
- Launcher
- Controls
- Settings

Do not overload one screen with unrelated controls.

### 5.3 Dark/light modes
Support:
- AMOLED dark
- standard dark
- light

All surfaces, text, dividers and controls need sufficient contrast.

### 5.4 Motion
Centralize animation durations/easing. Respect reduced-motion preference.

## 6. Launcher

Upgrade the existing optional launcher into a usable home experience:
- home pages
- dock
- favourites
- app drawer
- search
- folders
- page indicators
- app sorting
- persistent layout
- configurable grid density
- configurable icon scale
- long-press/edit mode
- wallpaper integration
- launcher accent theme

The launcher must never silently replace the user's home app. Android's home-role/chooser flow remains explicit.

Optional supported enhancements:
- icon badges where available
- notification dots
- widget placement via normal launcher APIs if feasible
- app shortcuts
- lightweight launcher animations

## 7. Volume experience

Provide a polished in-app Volume Lab:
- media
- ringtone
- alarm
- notification where the platform exposes independent streams
- accessibility-aware labels
- mute state
- device capability messaging

Add an optional custom volume HUD only where platform permissions and lifecycle constraints allow it.

The custom HUD must be clearly described as a companion surface. It must not claim to replace the protected Android system volume panel.

## 8. Notifications

Provide a polished notification experience:
- notification permission onboarding on supported Android versions
- notification settings shortcut
- test notification
- notification capability status
- optional NotificationListenerService integration only after explicit user enablement
- local notification feed inside KepTee when access is granted
- per-app notification grouping/filtering

The product must clearly distinguish:
1. notifications generated by KepTee,
2. notifications readable through explicit Notification Listener access,
3. the Android system notification shade, which remains OS-owned.

## 9. Control center / quick controls

Create a premium control dashboard that provides supported shortcuts for:
- Wi-Fi/internet settings
- Bluetooth
- brightness
- volume
- notification settings
- display settings
- home settings
- live wallpaper selection

Where Android does not expose direct control without privileged APIs, open the appropriate system panel instead of faking state changes.

## 10. Themes

Expand Theme Studio into reusable theme profiles:
- accent palette
- wallpaper link
- launcher accent
- card/surface treatment
- text contrast mode
- motion profile
- icon treatment where supported

Users can save, duplicate, rename, delete and apply local theme presets.

## 11. Architecture

Recommended package boundaries:

- core/designsystem: colors, typography, spacing, shapes, motion, reusable components
- wallpaper: scene model, scene registry, renderer, animation math, wallpaper service, preview renderer
- launcher: app discovery, persistent layout, home screen, app drawer, search, folders, launcher preferences
- controls: volume, system panels, capability detection
- notifications: permission flow, test notification, optional listener service, notification feed
- themes: theme model, persistence, theme application
- settings: preferences, reduced motion, battery mode, diagnostics

The existing MainActivity should be decomposed instead of continuing to grow as one file.

## 12. Data model

Persistent local state should include:

### WallpaperPreset
- id
- title
- category
- rendererType
- previewResource
- parameters
- batteryProfile
- amoledFriendly
- favourite
- lastAppliedAt

### WallpaperSettings
- speed
- intensity
- colorVariant
- reducedMotion
- batteryMode

### ThemePreset
- id
- name
- accent
- appearanceMode
- wallpaperId
- launcherSettings

### LauncherState
- pages
- dock
- favourites
- folder definitions
- grid configuration
- icon size

## 13. Testing strategy

Before implementation code for each new behavior:
- add focused unit test first
- run and observe the expected failure
- implement the minimum behavior
- rerun the focused test
- run the whole repository test suite

Core tests should cover:
- wallpaper registry completeness
- scene parameter bounds
- periodic animation continuity
- reduced-motion behavior
- wallpaper setting persistence
- theme persistence
- launcher layout persistence
- app filtering/search
- folder operations
- notification capability state
- volume mapping/capability behavior

Emulator/device verification should cover:
- MainActivity startup
- launcher startup
- wallpaper service registration/application flow
- notification permission path
- settings launches
- no crash while recreating activities where supported
- no runaway wallpaper thread after visibility/surface destruction

## 14. CI

Extend the existing workflow, not replace it.

Required gates:
- assembleDebug
- testDebugUnitTest
- lintDebug
- emulator startup smoke tests
- relevant activity launch checks
- diagnostics on failure
- debug APK artifact on success

Add deterministic tests for the wallpaper catalog and renderer math so the built-in 30-scene library cannot silently regress.

## 15. Performance constraints

- no network access from wallpaper rendering
- pause rendering when not visible
- avoid unnecessary allocations in frame loops
- cache static scene resources
- use bounded particle counts
- allow low-power/battery mode to reduce frame rate/effect complexity
- never load all large raster content simultaneously when unnecessary
- previews should be cheaper than full live rendering

## 16. Platform boundaries

KepTee may implement:
- its own launcher activity
- its own UI
- live WallpaperService
- in-app volume controls
- optional overlay surfaces subject to explicit permission
- notification listener features subject to explicit user permission
- shortcuts into Android system settings

KepTee may not claim to replace protected Android UI such as the system notification shade, Quick Settings, or global volume panel through ordinary app APIs.

## 17. Completion criteria

KepTee is considered complete for this stage when:
- the redesigned app has a consistent visual system
- 30 built-in live wallpapers are shipped locally
- all 30 render continuously with seamless periodic animation
- wallpaper browsing, preview, apply, favourites and per-wallpaper settings work
- launcher supports persistent home, dock, app drawer, search and folders
- volume controls are usable with capability-aware behavior
- notification flows and optional listener-based feed work with explicit access
- system control shortcuts are polished and accurate
- themes are persistent and reusable
- reduced-motion and battery-aware modes work
- unit tests cover deterministic core logic
- CI build, unit tests, lint and emulator smoke verification are green
- the debug APK is produced as a validated artifact
- known Android platform limitations are accurately documented