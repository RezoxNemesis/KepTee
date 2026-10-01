package com.rezoxnemesis.kebtee.themes

import androidx.compose.ui.graphics.Color
import com.rezoxnemesis.kebtee.Cyan
import com.rezoxnemesis.kebtee.Green
import com.rezoxnemesis.kebtee.Pink
import com.rezoxnemesis.kebtee.Violet

data class ThemePreset(
    val id: String,
    val name: String,
    val accent: Color,
    val motionIntensity: Float
)

object ThemePresets {
    val builtIns = listOf(
        ThemePreset("ultraviolet", "Ultraviolet", Violet, 1f),
        ThemePreset("cyber", "Cyber cyan", Cyan, 1f),
        ThemePreset("pulse", "Pulse pink", Pink, 0.9f),
        ThemePreset("mint", "Mint circuit", Green, 0.75f)
    )

    fun byId(id: String): ThemePreset? = builtIns.firstOrNull { it.id == id }
}
