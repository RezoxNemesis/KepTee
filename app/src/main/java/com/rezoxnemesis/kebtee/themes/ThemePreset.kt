package com.rezoxnemesis.kebtee.themes

import androidx.compose.ui.graphics.Color

data class ThemePreset(
    val id: String,
    val name: String,
    val accent: Color,
    val motionIntensity: Float
)

object ThemePresets {
    private val violet = Color(0xFF8B5CF6)
    private val cyan = Color(0xFF22D3EE)
    private val pink = Color(0xFFEC4899)
    private val green = Color(0xFF6EE7B7)

    val builtIns = listOf(
        ThemePreset("ultraviolet", "Ultraviolet", violet, 1f),
        ThemePreset("cyber", "Cyber cyan", cyan, 1f),
        ThemePreset("pulse", "Pulse pink", pink, 0.9f),
        ThemePreset("mint", "Mint circuit", green, 0.75f)
    )

    fun byId(id: String): ThemePreset? = builtIns.firstOrNull { it.id == id }
}
