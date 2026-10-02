package com.rezoxnemesis.kebtee.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperRenderMathTest {
    @Test
    fun loopPhaseWrapsToZeroAtExactPeriod() {
        assertEquals(0f, WallpaperRenderMath.loopPhase(0f, 6f), 0.0001f)
        assertEquals(0f, WallpaperRenderMath.loopPhase(6f, 6f), 0.0001f)
        assertEquals(0.5f, WallpaperRenderMath.loopPhase(3f, 6f), 0.0001f)
    }

    @Test
    fun periodicWaveMatchesAtLoopBoundary() {
        val start = WallpaperRenderMath.periodicWave(0f, 8f)
        val end = WallpaperRenderMath.periodicWave(8f, 8f)
        assertEquals(start, end, 0.0001f)
    }

    @Test
    fun reducedMotionCollapsesAnimationToStableValues() {
        assertEquals(0f, WallpaperRenderMath.motionAmount(reducedMotion = true, batteryMode = false), 0.0001f)
        assertEquals(0.35f, WallpaperRenderMath.motionAmount(reducedMotion = false, batteryMode = true), 0.0001f)
        assertEquals(1f, WallpaperRenderMath.motionAmount(reducedMotion = false, batteryMode = false), 0.0001f)
    }

    @Test
    fun particleSeedIsDeterministic() {
        val a = WallpaperRenderMath.seedUnit(1234)
        val b = WallpaperRenderMath.seedUnit(1234)
        assertEquals(a, b, 0.0001f)
        assertTrue(a in 0f..1f)
    }
}
