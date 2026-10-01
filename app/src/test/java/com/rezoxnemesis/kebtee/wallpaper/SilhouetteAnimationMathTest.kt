package com.rezoxnemesis.kebtee.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SilhouetteAnimationMathTest {
    @Test
    fun breathingPulseStaysWithinDesignedBrightnessRange() {
        (0..120).forEach { step ->
            val value = SilhouetteAnimationMath.breathingPulse(step / 10f)
            assertTrue("pulse below minimum: $value", value >= 0.82f)
            assertTrue("pulse above maximum: $value", value <= 1.0f)
        }
    }

    @Test
    fun breathingPulseRepeatsAfterOnePeriod() {
        val sampleTimes = listOf(0f, 0.5f, 1.5f, 3f, 5.25f)
        sampleTimes.forEach { time ->
            assertEquals(
                SilhouetteAnimationMath.breathingPulse(time),
                SilhouetteAnimationMath.breathingPulse(time + 6f),
                0.0001f
            )
        }
    }

    @Test
    fun lightSweepMovesSmoothlyFromStartToEndAndBack() {
        assertEquals(0f, SilhouetteAnimationMath.sweepProgress(0f), 0.0001f)
        assertEquals(1f, SilhouetteAnimationMath.sweepProgress(4.5f), 0.0001f)
        assertEquals(0f, SilhouetteAnimationMath.sweepProgress(9f), 0.0001f)
        (0..90).forEach { step ->
            val value = SilhouetteAnimationMath.sweepProgress(step / 10f)
            assertTrue("sweep out of range: $value", value >= 0f && value <= 1f)
        }
    }
}
