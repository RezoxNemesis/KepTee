package com.rezoxnemesis.kebtee

/**
 * Pure ranking policy for launcher-local Recent and Most used drawer modes.
 * Input order is preserved for ties, which keeps the existing alphabetical app ordering stable.
 */
internal data class LauncherUsageEntry(
    val key: String,
    val lastUsedMillis: Long,
    val launchCount: Int
)

internal fun orderLauncherUsage(
    entries: List<LauncherUsageEntry>,
    mode: String
): List<LauncherUsageEntry> = when (mode) {
    "Recent" -> entries
        .filter { it.lastUsedMillis > 0L }
        .sortedByDescending { it.lastUsedMillis }

    "Most used" -> entries
        .filter { it.launchCount > 0 }
        .sortedByDescending { it.launchCount }

    else -> entries
}
