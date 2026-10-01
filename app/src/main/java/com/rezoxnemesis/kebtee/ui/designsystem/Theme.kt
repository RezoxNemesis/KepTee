package com.rezoxnemesis.kebtee.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KebTeeColors = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF001014),
    secondary = Color(0xFF8B5CF6),
    tertiary = Color(0xFFEC4899),
    background = Color(0xFF070B18),
    onBackground = Color(0xFFF3F6FF),
    surface = Color(0xFF11182B),
    onSurface = Color(0xFFF3F6FF),
    surfaceVariant = Color(0xFF182440),
    onSurfaceVariant = Color(0xFF9EABC9)
)

@Composable
fun KebTeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KebTeeColors,
        content = content
    )
}
