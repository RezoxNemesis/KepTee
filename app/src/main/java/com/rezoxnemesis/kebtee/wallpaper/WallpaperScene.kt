package com.rezoxnemesis.kebtee.wallpaper

/**
 * Selectable wallpaper scenes. ORIGINAL remains the default so the existing approved
 * artwork and user experience are preserved unless the user explicitly chooses another scene.
 */
enum class WallpaperScene(
    val id: String,
    val title: String,
    val isVideo: Boolean
) {
    ORIGINAL("original", "Original Silhouette", false),
    ASCENSION_STILL("ascension_still", "Ascension Still", false),
    ASCENSION_FLOW("ascension_flow", "Ascension Flow", true),
    AURA_PULSE("aura_pulse", "Aura Pulse", true);

    companion object {
        fun fromId(value: String?): WallpaperScene =
            entries.firstOrNull { it.id == value } ?: ORIGINAL
    }
}
