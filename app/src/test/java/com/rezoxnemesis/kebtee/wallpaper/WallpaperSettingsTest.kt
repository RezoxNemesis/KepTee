package com.rezoxnemesis.kebtee.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperSettingsTest {
    @Test fun corruptedOrOutOfRangeValuesHaveSafeBounds() {
        val settings = WallpaperSettings(speed = Float.NaN, glow = Float.POSITIVE_INFINITY, particleDensity = 900, fps = 900).normalized()
        assertEquals(1f, settings.speed, 0f)
        assertEquals(0.45f, settings.glow, 0f)
        assertEquals(60, settings.particleDensity)
        assertEquals(120, settings.fps)
        assertEquals(0, WallpaperSettings(particleDensity = -1).normalized().particleDensity)
    }

    @Test fun reducedMotionStopsAnimationEvenWhenChargingOrPowerSaverChanges() {
        val settings = WallpaperSettings(reduceMotion = true)
        assertEquals(0L, settings.frameIntervalMillis(false, false))
        assertEquals(0L, settings.frameIntervalMillis(true, true))
        assertEquals(0, settings.activeParticleCount(false, false))
    }

    @Test fun systemAndUserSaverLimitWork() {
        assertEquals(100L, WallpaperSettings(fps = 60).frameIntervalMillis(true, false))
        assertEquals(100L, WallpaperSettings(batterySaver = true).frameIntervalMillis(false, false))
        assertEquals(0, WallpaperSettings().activeParticleCount(true, false))
        assertEquals(0, WallpaperSettings(batterySaver = true).activeParticleCount(false, false))
    }

    @Test fun lockBehaviourAndFrameRateControlsAreApplied() {
        assertEquals(100L, WallpaperSettings().frameIntervalMillis(false, true))
        assertEquals(16L, WallpaperSettings(fps = 60, dimOnLock = false).frameIntervalMillis(false, true))
        assertEquals(66L, WallpaperSettings(fps = 15).frameIntervalMillis(false, false))
        assertEquals(42, WallpaperSettings(particleDensity = 42).activeParticleCount(false, false))
    }

    @Test fun tiltMotionRespectsUserAndPowerConstraints() {
        assertEquals(true, WallpaperSettings().tiltEnabled(false, false))
        assertEquals(false, WallpaperSettings(tiltMotion = false).tiltEnabled(false, false))
        assertEquals(false, WallpaperSettings(reduceMotion = true).tiltEnabled(false, false))
        assertEquals(false, WallpaperSettings(batterySaver = true).tiltEnabled(false, false))
        assertEquals(false, WallpaperSettings().tiltEnabled(true, false))
        assertEquals(false, WallpaperSettings().tiltEnabled(false, true))
        assertEquals(true, WallpaperSettings(dimOnLock = false).tiltEnabled(false, true))
    }

    @Test fun displayRefreshRateCapsRequestedWallpaperFps() {
        val settings = WallpaperSettings(fps = 120)
        assertEquals(16L, settings.frameIntervalMillis(false, false, 60f))
        assertEquals(11L, settings.frameIntervalMillis(false, false, 90f))
        assertEquals(8L, settings.frameIntervalMillis(false, false, 120f))
        assertEquals(33L, WallpaperSettings(fps = 30).frameIntervalMillis(false, false, 120f))
        assertEquals(8L, settings.frameIntervalMillis(false, false, Float.NaN))
    }

    @Test fun renderBudgetIsSafeBeforePersistenceNormalization() {
        assertEquals(66L, WallpaperSettings(fps = Int.MIN_VALUE).frameIntervalMillis(false, false))
        assertEquals(33L, WallpaperSettings(fps = 16).frameIntervalMillis(false, false))
        assertEquals(8L, WallpaperSettings(fps = Int.MAX_VALUE).frameIntervalMillis(false, false))
        assertEquals(8L, WallpaperSettings(fps = 120).frameIntervalMillis(false, false))
        assertEquals(0, WallpaperSettings(particleDensity = Int.MIN_VALUE).activeParticleCount(false, false))
        assertEquals(60, WallpaperSettings(particleDensity = Int.MAX_VALUE).activeParticleCount(false, false))
    }

    @Test fun videoMotionRespectsVisibilityIndependentPowerConstraints() {
        val video = WallpaperSettings(scene = WallpaperScene.ASCENSION_FLOW)
        assertEquals(true, video.videoMotionEnabled(false, false))
        assertEquals(false, WallpaperSettings().videoMotionEnabled(false, false))
        assertEquals(false, video.copy(reduceMotion = true).videoMotionEnabled(false, false))
        assertEquals(false, video.copy(batterySaver = true).videoMotionEnabled(false, false))
        assertEquals(false, video.videoMotionEnabled(true, false))
        assertEquals(false, video.videoMotionEnabled(false, true))
        assertEquals(true, video.copy(dimOnLock = false).videoMotionEnabled(false, true))
    }

    @Test fun sceneIdsRoundTripAndUnknownValuesFallBackToOriginal() {
        assertEquals(WallpaperScene.ORIGINAL, WallpaperScene.fromId(null))
        assertEquals(WallpaperScene.ORIGINAL, WallpaperScene.fromId("not-a-real-scene"))
        WallpaperScene.entries.forEach { scene ->
            assertEquals(scene, WallpaperScene.fromId(scene.id))
        }
        assertEquals(false, WallpaperScene.ORIGINAL.isVideo)
        assertEquals(false, WallpaperScene.ASCENSION_STILL.isVideo)
        assertEquals(true, WallpaperScene.ASCENSION_FLOW.isVideo)
        assertEquals(true, WallpaperScene.AURA_PULSE.isVideo)
    }
}
