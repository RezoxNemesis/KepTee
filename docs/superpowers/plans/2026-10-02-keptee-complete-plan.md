# KepTee Complete Customization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn KepTee into a polished, usable Android customization studio with 30 built-in procedural live wallpapers, a stronger launcher, polished controls/notifications/themes, and green CI.

**Architecture:** Decompose the current monolithic `MainActivity.kt` into focused feature packages while preserving the working Android entry points. Replace the one-image wallpaper service with a shared, phase-based procedural scene renderer and a versioned scene catalog, so all wallpapers run continuously without shipping 30 large video files. Persist user-facing customization state in a small local preference-backed repository.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Android WallpaperService, SharedPreferences initially (preserve current storage model), JUnit 4, Android emulator smoke tests, Gradle 8.9, Android SDK 35.

**Spec:** `docs/superpowers/specs/2026-10-02-keptee-complete-design.md`

## Global Constraints

- Use only free capabilities of connected development services.
- Ship 30 built-in wallpaper scenes in v1; architecture must permit 50.
- Wallpaper rendering must be local/offline and perform no network work.
- Pause rendering when wallpaper is not visible.
- Respect reduced-motion and battery-aware modes.
- Do not claim to replace protected Android system surfaces.
- Preserve the explicit Android home/notification/overlay permission model.
- Add tests before production behavior changes and verify the full repository suite before marking a task done.
- Extend the existing CI workflow rather than replacing it.

## Review Focus

1. **Scene continuity:** animation state must be periodic with no visible jump at loop boundaries.
2. **Resource pressure:** frame rendering must not allocate large objects repeatedly or load all wallpaper assets simultaneously.
3. **Lifecycle:** WallpaperService must stop work when hidden/destroyed and recover after surface recreation.
4. **Persistence:** wallpaper/theme/launcher preferences must survive process recreation.
5. **Platform capability boundaries:** unavailable system controls must open the supported Android panel or show an accurate capability state instead of faking system behavior.

---

### Task 1: Create isolated implementation branch and plan baseline

**Files:**
- Branch: `superpowers/2026-10-02-keptee-complete`

**Interfaces:**
- Starts from: `superpowers/2026-10-02-keptee-complete-design`
- Produces: isolated branch for all implementation work.

- [ ] Create implementation branch from the approved design branch.
- [ ] Confirm current baseline commit and existing CI run #27.
- [ ] Keep the design and plan documents unchanged on the implementation branch.

### Task 2: Extract wallpaper domain model and catalog

**Files:**
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperScene.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperCatalog.kt`
- Create: `app/src/test/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperCatalogTest.kt`

**Interfaces:**
- `enum class WallpaperCategory`
- `enum class WallpaperRendererType`
- `data class WallpaperScene(id, title, category, rendererType, palette, batteryProfile, amoledFriendly)`
- `object WallpaperCatalog { val scenes: List<WallpaperScene>; fun byId(id: String): WallpaperScene? }`

- [ ] Write failing tests for exactly 30 catalog entries, unique IDs, non-empty metadata, category coverage, and stable lookup.
- [ ] Run the focused test and verify it fails because the catalog does not yet exist.
- [ ] Implement the model and catalog with 30 curated procedural scenes.
- [ ] Run the focused test and verify it passes.
- [ ] Run all unit tests.

### Task 3: Build the procedural wallpaper renderer

**Files:**
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperRenderMath.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperRenderer.kt`
- Modify: `app/src/main/java/com/rezoxnemesis/kebtee/wallpaper/KebTeeLiveWallpaperService.kt`
- Create: `app/src/test/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperRenderMathTest.kt`

**Interfaces:**
- `fun loopPhase(timeSeconds: Float, periodSeconds: Float): Float`
- `fun periodicWave(timeSeconds: Float, periodSeconds: Float): Float`
- `class WallpaperRenderer`
- `fun draw(canvas: Canvas, scene: WallpaperScene, state: WallpaperRenderState)`

- [ ] Write failing tests for phase normalization, periodic boundary equality, reduced-motion behavior, and bounded effect parameters.
- [ ] Run focused tests and verify correct failures.
- [ ] Implement a reusable procedural renderer supporting gradients, particles, waves, orbiting geometry, glow and parallax-like motion.
- [ ] Implement 30 scene mappings using compact parameter definitions instead of bespoke renderer code.
- [ ] Integrate the renderer into `KebTeeLiveWallpaperService`.
- [ ] Ensure visibility/surface destruction cancels callbacks and stops rendering.
- [ ] Run focused + full unit tests.
- [ ] Verify no network access and no per-frame large allocations in the renderer code review.

### Task 4: Build wallpaper studio UI and persistence

**Files:**
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperPreferences.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperStudioScreen.kt`
- Create: `app/src/test/java/com/rezoxnemesis/kebtee/wallpaper/WallpaperPreferencesTest.kt`
- Modify: `app/src/main/java/com/rezoxnemesis/kebtee/MainActivity.kt` or extracted navigation shell.

**Interfaces:**
- `data class WallpaperSettings(speed, intensity, colorVariant, reducedMotion, batteryMode)`
- `class WallpaperPreferences`
- `fun settingsFor(sceneId: String): WallpaperSettings`
- `fun saveSettings(sceneId: String, settings: WallpaperSettings)`

- [ ] Write failing tests for default settings, per-wallpaper persistence, bounds, favorites and recently-used state.
- [ ] Implement preference storage.
- [ ] Build a gallery with 30 scene cards, categories, selected state, favorites and preview/apply actions.
- [ ] Add speed/intensity/reduced-motion/battery controls.
- [ ] Reuse the existing wallpaper picker flow for actual application.
- [ ] Verify process recreation restores settings.

### Task 5: Establish the KepTee design system

**Files:**
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/ui/designsystem/Color.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/ui/designsystem/Theme.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/ui/designsystem/Dimens.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/ui/designsystem/Motion.kt`
- Create: reusable surface/control components as needed.
- Modify: screens currently embedding hardcoded tokens.

**Interfaces:**
- Shared Material theme
- Light / dark / AMOLED appearance modes
- Shared motion tokens
- Shared cards, chips, section headers, sliders and settings rows

- [ ] Add tests only for deterministic theme/persistence mapping where appropriate.
- [ ] Replace duplicated hardcoded design tokens.
- [ ] Preserve current brand identity while tightening spacing, hierarchy and contrast.
- [ ] Ensure reduced-motion routes through the common motion system.

### Task 6: Split the current monolithic dashboard

**Files:**
- Modify: `app/src/main/java/com/rezoxnemesis/kebtee/MainActivity.kt`
- Create feature screens under `ui/home`, `ui/controls`, `ui/settings`, `themes`.

**Interfaces:**
- MainActivity handles Android lifecycle/permission/system-intent responsibilities.
- Composables receive explicit state and callbacks instead of owning platform plumbing.

- [ ] Extract dashboard, controls, theme, notifications and settings sections.
- [ ] Keep behavior identical while changing structure.
- [ ] Remove duplicated state handling and dead branches.
- [ ] Run unit tests + lint + debug assembly.

### Task 7: Upgrade Theme Studio

**Files:**
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/themes/ThemePreset.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/themes/ThemeRepository.kt`
- Create: `app/src/main/java/com/rezoxnemesis/kebtee/themes/ThemeStudioScreen.kt`
- Create: matching tests.

**Interfaces:**
- `data class ThemePreset`
- CRUD for presets
- Apply/duplicate/delete/rename

- [ ] TDD theme preset persistence and application.
- [ ] Implement palette + appearance + wallpaper + motion linkage.
- [ ] Make theme application update dashboard and launcher accent state.

### Task 8: Upgrade launcher persistence and UX

**Files:**
- Modify: `HomeLauncherActivity.kt`
- Create: launcher state/repository/models as needed.
- Create: launcher tests.

**Interfaces:**
- persistent page/dock/favorite/folder/grid state
- search/filter
- edit mode

- [ ] TDD launcher state serialization and search/folder logic.
- [ ] Implement persistent home pages, dock, folders, grid density and icon scale.
- [ ] Add edit mode and clearer empty states.
- [ ] Keep Android home-role/chooser behavior explicit.
- [ ] Keep launcher startup within emulator smoke test coverage.

### Task 9: Upgrade Volume Lab and supported system controls

**Files:**
- Extract controls from `MainActivity.kt` into `controls/VolumeController.kt`, `controls/SystemPanelController.kt`, and UI.
- Add tests for stream mapping/capability decisions.

- [ ] TDD platform capability mapping.
- [ ] Improve slider UX, labels, mute states and device-specific limitations.
- [ ] Add optional custom HUD only where supported.
- [ ] Ensure unsupported direct-control paths open the proper Android panel.

### Task 10: Upgrade notifications

**Files:**
- Create notification feature package.
- Modify manifest only for explicit, supported service declarations.
- Create tests for permission/capability state.

- [ ] TDD notification capability model.
- [ ] Improve permission onboarding and test notification.
- [ ] Add explicit NotificationListenerService only with user enablement.
- [ ] Add a local notification feed when access is granted.
- [ ] Clearly distinguish app notifications, listener data, and system shade.

### Task 11: Wire the unified navigation and polished settings

**Files:**
- Navigation shell and settings screens.
- Existing Android settings shortcuts.

- [ ] Build the five primary areas: Home, Wallpapers, Launcher, Controls, Settings.
- [ ] Add search/discovery where useful.
- [ ] Add diagnostics and capability status.
- [ ] Verify large and small phone layouts through emulator smoke checks.

### Task 12: Expand CI verification

**Files:**
- Modify: `.github/workflows/android-ci.yml`
- Add or update diagnostics/test configuration.

- [ ] Add wallpaper-catalog/unit regression gate.
- [ ] Keep assembleDebug, testDebugUnitTest, lintDebug and emulator smoke checks.
- [ ] Validate MainActivity and HomeLauncherActivity launch.
- [ ] Add a lightweight wallpaper-service registration/application check that is practical on the emulator.
- [ ] Keep diagnostics uploaded on failure.
- [ ] Upload debug APK only after successful verification.

### Task 13: Final regression and release verification

**Files:**
- Documentation/status updates.

- [ ] Run the complete unit suite.
- [ ] Run lint and debug assembly.
- [ ] Run the full CI workflow.
- [ ] Inspect workflow job logs and artifacts.
- [ ] Verify the APK artifact exists.
- [ ] Update `docs/IMPLEMENTATION_STATUS.md` and `README.md` to reflect actual features and platform boundaries.
- [ ] Only after fresh evidence confirms all gates, mark the KepTee stage complete.
