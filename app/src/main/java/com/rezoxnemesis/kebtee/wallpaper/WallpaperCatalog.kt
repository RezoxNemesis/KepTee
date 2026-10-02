package com.rezoxnemesis.kebtee.wallpaper

object WallpaperCatalog {
    val scenes: List<WallpaperScene> = listOf(
        scene("aurora-01", "Arctic Veil", WallpaperCategory.AURORA, WallpaperRendererType.AURORA, 0xFF14213D, 0xFF2E86AB, 0xFF9AE6B4, BatteryProfile.BALANCED, true),
        scene("aurora-02", "Polar Bloom", WallpaperCategory.AURORA, WallpaperRendererType.AURORA, 0xFF101B3D, 0xFF7C3AED, 0xFF67E8F9, BatteryProfile.BALANCED, true),
        scene("aurora-03", "Violet Dawn", WallpaperCategory.AURORA, WallpaperRendererType.AURORA, 0xFF24104F, 0xFFBE185D, 0xFFF0ABFC, BatteryProfile.BALANCED, true),
        scene("aurora-04", "Ocean Halo", WallpaperCategory.AURORA, WallpaperRendererType.AURORA, 0xFF06283D, 0xFF0EA5E9, 0xFF5EEAD4, BatteryProfile.LOW, true),

        scene("neon-01", "Neon Pulse", WallpaperCategory.NEON, WallpaperRendererType.NEON_GRID, 0xFF050816, 0xFF1D4ED8, 0xFF22D3EE, BatteryProfile.ACTIVE, true),
        scene("neon-02", "Electric Alley", WallpaperCategory.NEON, WallpaperRendererType.NEON_GRID, 0xFF09051A, 0xFF7C3AED, 0xFFF472B6, BatteryProfile.ACTIVE, true),
        scene("neon-03", "Laser Grid", WallpaperCategory.NEON, WallpaperRendererType.NEON_GRID, 0xFF031016, 0xFF0F766E, 0xFF5EEAD4, BatteryProfile.BALANCED, true),
        scene("neon-04", "Midnight Circuit", WallpaperCategory.NEON, WallpaperRendererType.NEON_GRID, 0xFF030712, 0xFF1E40AF, 0xFFC4B5FD, BatteryProfile.BALANCED, true),

        scene("fluid-01", "Liquid Cyan", WallpaperCategory.FLUID, WallpaperRendererType.FLUID_WAVES, 0xFF04151F, 0xFF075985, 0xFF67E8F9, BatteryProfile.BALANCED, true),
        scene("fluid-02", "Ink Bloom", WallpaperCategory.FLUID, WallpaperRendererType.FLUID_WAVES, 0xFF0B0714, 0xFF581C87, 0xFFC084FC, BatteryProfile.BALANCED, true),
        scene("fluid-03", "Rose Current", WallpaperCategory.FLUID, WallpaperRendererType.FLUID_WAVES, 0xFF1A0710, 0xFF9F1239, 0xFFFDA4AF, BatteryProfile.BALANCED, true),
        scene("fluid-04", "Deep Tide", WallpaperCategory.FLUID, WallpaperRendererType.FLUID_WAVES, 0xFF02151D, 0xFF155E75, 0xFF2DD4BF, BatteryProfile.LOW, true),

        scene("geometry-01", "Glass Geometry", WallpaperCategory.GEOMETRY, WallpaperRendererType.GEOMETRY, 0xFF0A0F1D, 0xFF1E293B, 0xFF93C5FD, BatteryProfile.LOW, true),
        scene("geometry-02", "Prism Room", WallpaperCategory.GEOMETRY, WallpaperRendererType.GEOMETRY, 0xFF160B26, 0xFF4C1D95, 0xFFE9D5FF, BatteryProfile.BALANCED, true),
        scene("geometry-03", "Chrome Orbit", WallpaperCategory.GEOMETRY, WallpaperRendererType.GEOMETRY, 0xFF080B12, 0xFF334155, 0xFFCBD5E1, BatteryProfile.LOW, true),
        scene("geometry-04", "Soft Blocks", WallpaperCategory.GEOMETRY, WallpaperRendererType.GEOMETRY, 0xFF111827, 0xFF374151, 0xFF6EE7B7, BatteryProfile.LOW, true),

        scene("space-01", "Deep Space", WallpaperCategory.SPACE, WallpaperRendererType.SPACE, 0xFF02030A, 0xFF172554, 0xFF93C5FD, BatteryProfile.BALANCED, true),
        scene("space-02", "Solar Drift", WallpaperCategory.SPACE, WallpaperRendererType.SPACE, 0xFF100804, 0xFF7C2D12, 0xFFFCD34D, BatteryProfile.BALANCED, false),
        scene("space-03", "Violet Orbit", WallpaperCategory.SPACE, WallpaperRendererType.SPACE, 0xFF070315, 0xFF581C87, 0xFFD8B4FE, BatteryProfile.BALANCED, true),
        scene("space-04", "Moon Garden", WallpaperCategory.SPACE, WallpaperRendererType.SPACE, 0xFF04070F, 0xFF164E63, 0xFF99F6E4, BatteryProfile.LOW, true),

        scene("nature-01", "Firefly Meadow", WallpaperCategory.NATURE, WallpaperRendererType.NATURE, 0xFF03130A, 0xFF166534, 0xFFFDE68A, BatteryProfile.BALANCED, true),
        scene("nature-02", "Night Fern", WallpaperCategory.NATURE, WallpaperRendererType.NATURE, 0xFF06110D, 0xFF14532D, 0xFF86EFAC, BatteryProfile.LOW, true),
        scene("nature-03", "Sunset Canopy", WallpaperCategory.NATURE, WallpaperRendererType.NATURE, 0xFF180B05, 0xFF9A3412, 0xFFFDE68A, BatteryProfile.BALANCED, false),

        scene("amoled-01", "Black Ember", WallpaperCategory.AMOLED, WallpaperRendererType.AMOLED_GLOW, 0xFF000000, 0xFF1C1917, 0xFFF97316, BatteryProfile.LOW, true),
        scene("amoled-02", "Black Ice", WallpaperCategory.AMOLED, WallpaperRendererType.AMOLED_GLOW, 0xFF000000, 0xFF0F172A, 0xFF38BDF8, BatteryProfile.LOW, true),
        scene("amoled-03", "Black Orchid", WallpaperCategory.AMOLED, WallpaperRendererType.AMOLED_GLOW, 0xFF000000, 0xFF1F062B, 0xFFE879F9, BatteryProfile.LOW, true),

        scene("particle-01", "Particle Rain", WallpaperCategory.PARTICLE, WallpaperRendererType.PARTICLE_FLOW, 0xFF020617, 0xFF0F172A, 0xFF22D3EE, BatteryProfile.ACTIVE, true),
        scene("particle-02", "Stardust", WallpaperCategory.PARTICLE, WallpaperRendererType.PARTICLE_FLOW, 0xFF030712, 0xFF172554, 0xFFF8FAFC, BatteryProfile.ACTIVE, true),
        scene("particle-03", "Rose Dust", WallpaperCategory.PARTICLE, WallpaperRendererType.PARTICLE_FLOW, 0xFF12050C, 0xFF4C0519, 0xFFF9A8D4, BatteryProfile.ACTIVE, true),
        scene("particle-04", "Emerald Drift", WallpaperCategory.PARTICLE, WallpaperRendererType.PARTICLE_FLOW, 0xFF02120A, 0xFF14532D, 0xFF6EE7B7, BatteryProfile.BALANCED, true),

        scene("aurora-05", "Glacier Bloom", WallpaperCategory.AURORA, WallpaperRendererType.AURORA, 0xFF061A2B, 0xFF2563EB, 0xFFA5F3FC, BatteryProfile.LOW, true),
        scene("aurora-06", "Solar Aurora", WallpaperCategory.AURORA, WallpaperRendererType.AURORA, 0xFF160B24, 0xFF9D174D, 0xFFFDE68A, BatteryProfile.BALANCED, false),
        scene("neon-05", "Cyber Lagoon", WallpaperCategory.NEON, WallpaperRendererType.NEON_GRID, 0xFF020B1A, 0xFF0369A1, 0xFF67E8F9, BatteryProfile.ACTIVE, true),
        scene("neon-06", "Ultraviolet Run", WallpaperCategory.NEON, WallpaperRendererType.NEON_GRID, 0xFF10051F, 0xFF6D28D9, 0xFFF0ABFC, BatteryProfile.ACTIVE, true),
        scene("fluid-05", "Opal Current", WallpaperCategory.FLUID, WallpaperRendererType.FLUID_WAVES, 0xFF081225, 0xFF0F766E, 0xFF99F6E4, BatteryProfile.BALANCED, true),
        scene("geometry-05", "Satin Prism", WallpaperCategory.GEOMETRY, WallpaperRendererType.GEOMETRY, 0xFF0C1020, 0xFF4338CA, 0xFFF5D0FE, BatteryProfile.LOW, true),
        scene("space-05", "Nebula Gate", WallpaperCategory.SPACE, WallpaperRendererType.SPACE, 0xFF08051B, 0xFF4C1D95, 0xFFF0ABFC, BatteryProfile.BALANCED, true),
        scene("nature-04", "Mosslight", WallpaperCategory.NATURE, WallpaperRendererType.NATURE, 0xFF06120B, 0xFF3F6212, 0xFFD9F99D, BatteryProfile.LOW, true),
        scene("amoled-04", "Obsidian Flame", WallpaperCategory.AMOLED, WallpaperRendererType.AMOLED_GLOW, 0xFF000000, 0xFF292015, 0xFFFBBF24, BatteryProfile.LOW, true),
        scene("particle-05", "Blue Comet Dust", WallpaperCategory.PARTICLE, WallpaperRendererType.PARTICLE_FLOW, 0xFF020617, 0xFF1E3A8A, 0xFFBAE6FD, BatteryProfile.ACTIVE, true)
    )

    fun byId(id: String): WallpaperScene? = scenes.firstOrNull { it.id == id }

    private fun scene(
        id: String,
        title: String,
        category: WallpaperCategory,
        rendererType: WallpaperRendererType,
        primary: Long,
        secondary: Long,
        accent: Long,
        batteryProfile: BatteryProfile,
        amoledFriendly: Boolean
    ) = WallpaperScene(
        id = id,
        title = title,
        category = category,
        rendererType = rendererType,
        palette = WallpaperPalette(
            primary = primary.toInt(),
            secondary = secondary.toInt(),
            accent = accent.toInt()
        ),
        batteryProfile = batteryProfile,
        amoledFriendly = amoledFriendly
    )
}
