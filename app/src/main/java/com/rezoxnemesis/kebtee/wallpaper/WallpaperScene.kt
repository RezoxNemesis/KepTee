package com.rezoxnemesis.kebtee.wallpaper

enum class WallpaperCategory {
    AURORA, NEON, FLUID, GEOMETRY, SPACE, NATURE, AMOLED, PARTICLE, SILHOUETTE
}

enum class WallpaperRendererType {
    AURORA, NEON_GRID, FLUID_WAVES, GEOMETRY, SPACE, NATURE, AMOLED_GLOW, PARTICLE_FLOW, SILHOUETTE
}

enum class BatteryProfile {
    LOW, BALANCED, ACTIVE
}

data class WallpaperPalette(
    val primary: Int,
    val secondary: Int,
    val accent: Int
)

data class WallpaperScene(
    val id: String,
    val title: String,
    val category: WallpaperCategory,
    val rendererType: WallpaperRendererType,
    val palette: WallpaperPalette,
    val batteryProfile: BatteryProfile,
    val amoledFriendly: Boolean
)
