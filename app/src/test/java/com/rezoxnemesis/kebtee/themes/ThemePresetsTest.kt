package com.rezoxnemesis.kebtee.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePresetsTest {
    @Test
    fun builtInThemesAreUniqueAndFindable() {
        assertEquals(4, ThemePresets.builtIns.size)
        assertEquals(4, ThemePresets.builtIns.map { it.id }.toSet().size)
        assertTrue(ThemePresets.builtIns.all { ThemePresets.byId(it.id) == it })
    }
}
