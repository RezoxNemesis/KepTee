package com.rezoxnemesis.kebtee.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SilhouetteAnimationMathTest {
    @Test
    fun breathingPulseRepeatsAfterOnePeriod() {
        val sample = 2.25f
        assertEquals(
            SilhouetteAnimationMath.breathingPulse(sample),
            SilhouetteAnimationMath.breathingPulse(sample + 6f),
            0.0001f
        )
    }

    @Test
    fun sweepReturnsToItsStartWithoutAJump() {
        assertEquals(0f, SilhouetteAnimationMath.sweepProgress(0f), 0.0001f)
        assertEquals(
            SilhouetteAnimationMath.sweepProgress(0f),
            SilhouetteAnimationMath.sweepProgress(9f),
            0.0001f
        )
    }

    @Test
    fun breathingPulseStaysWithinItsDesignedRange() {
        for (second in 0..600) {
            val pulse = SilhouetteAnimationMath.breathingPulse(second / 100f)
            assertTrue(pulse >= 0.82f && pulse <= 1.0f)
        }
    }
}
