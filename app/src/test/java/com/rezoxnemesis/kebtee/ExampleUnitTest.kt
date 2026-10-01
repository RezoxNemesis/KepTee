package com.rezoxnemesis.kebtee

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun appIdentityIsStable() {
        assertEquals("KebTee", "KebTee")
    }

    @Test
    fun accentPreferenceRoundTrips() {
        val accents = listOf(
            "violet" to Color(0xFF8B5CF6),
            "cyan" to Color(0xFF22D3EE),
            "pink" to Color(0xFFEC4899),
            "green" to Color(0xFF6EE7B7)
        )
        accents.forEach { (key, color) ->
            assertEquals(color, accentFromPreference(key))
            assertEquals(key, preferenceForAccent(color))
        }
    }

    @Test
    fun unknownAccentPreferenceFallsBackToViolet() {
        assertEquals(Color(0xFF8B5CF6), accentFromPreference("not-a-theme"))
        assertEquals(Color(0xFF8B5CF6), accentFromPreference(null))
    }
}
