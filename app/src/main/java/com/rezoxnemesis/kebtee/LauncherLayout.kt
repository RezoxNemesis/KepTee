package com.rezoxnemesis.kebtee

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HomeItem(val id: String, val apps: List<String>, val name: String = "Folder", val silver: Boolean = false) {
    val isFolder get() = id.startsWith("folder:")
}

data class LauncherLayout(
    val pages: List<List<HomeItem>> = listOf(emptyList()),
    val dock: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val columns: Int = 4,
    val iconSize: Int = 56,
    val labels: Boolean = true,
    val locked: Boolean = false,
    val drawerStyle: String = "Grid",
    val folderStyle: String = "Glass",
    val swipeDown: String = "Search",
    val doubleTap: String = "Search"
) {
    fun normalized(): LauncherLayout {
        val seen = mutableSetOf<String>()
        val itemIds = mutableSetOf<String>()
        val cleanPages = pages.take(12).map { page ->
            page.take(100).mapNotNull { item ->
                // Do not mark overflow members as seen: they may have a valid slot later.
                val members = item.apps.asSequence().filter { validAppKey(it) && it !in seen }
                    .distinct().take(100).toList()
                if (members.isEmpty()) null else {
                    seen.addAll(members)
                    if (!item.isFolder && members.size == 1) {
                        itemIds.add(members.first())
                        HomeItem(members.first(), members)
                    } else {
                        val base = if (item.isFolder && item.id.length <= 500) item.id else "folder:${UUID.randomUUID()}"
                        var id = base
                        var suffix = 1
                        while (!itemIds.add(id)) id = "$base:${suffix++}"
                        item.copy(id = id, apps = members, name = item.name.take(48))
                    }
                }
            }
        }.ifEmpty { listOf(emptyList()) }
        return copy(pages = cleanPages, dock = dock.filter(::validAppKey).distinct().take(5),
            hidden = hidden.filter(::validAppKey).take(2000).toSet(), columns = columns.coerceIn(3, 6),
            iconSize = iconSize.coerceIn(40, 72), drawerStyle = drawerStyle.takeIf { it in listOf("Grid", "List") } ?: "Grid",
            folderStyle = folderStyle.takeIf { it in listOf("Glass", "Radial") } ?: "Glass",
            swipeDown = swipeDown.takeIf { it in ACTIONS } ?: "Search", doubleTap = doubleTap.takeIf { it in ACTIONS } ?: "Search")
    }

    fun remove(key: String): LauncherLayout = copy(pages = pages.map { page -> page.mapNotNull { item ->
        val apps = item.apps - key
        when {
            key !in item.apps -> item
            apps.isEmpty() -> null
            apps.size == 1 -> HomeItem(apps.first(), apps)
            else -> item.copy(apps = apps)
        }
    } }, dock = dock - key)

    fun place(key: String, page: Int): LauncherLayout {
        if (locked || !validAppKey(key)) return this
        val clean = remove(key)
        val availablePages = clean.pages.ifEmpty { listOf(emptyList()) }
        val destination = page.coerceIn(0, availablePages.lastIndex)
        // Refuse a full destination before removing the source from its current location.
        if (availablePages[destination].size >= 100) return this
        return clean.copy(pages = availablePages.mapIndexed { index, items ->
            if (index == destination) items + HomeItem(key, listOf(key)) else items
        })
    }

    /** Drag onto an existing item to group; order is preserved inside the folder. */
    fun group(key: String, targetId: String): LauncherLayout {
        if (locked || !validAppKey(key)) return this
        val target = pages.flatten().firstOrNull { it.id == targetId } ?: return this
        if (key in target.apps || target.apps.size >= 100) return this
        val clean = remove(key)
        return clean.copy(pages = clean.pages.map { items -> items.map { item ->
            if (item.id == targetId) item.copy(id = if (item.isFolder) item.id else "folder:${UUID.randomUUID()}", apps = item.apps + key) else item
        } })
    }

    companion object {
        val ACTIONS = listOf("Search", "All apps", "Edit home", "Settings", "None")
        fun validAppKey(value: String): Boolean = value.length in 3..500 && value.count { it == '/' } == 1 &&
            value.substringBefore('/').isNotBlank() && value.substringAfter('/').isNotBlank()
    }
}

object LauncherLayoutStore {
    const val FILE = "kebtee_launcher"
    const val KEY = "layout_v1"

    fun read(prefs: SharedPreferences): LauncherLayout {
        val values = prefs.all
        val stored = values[KEY] as? String
        if (stored != null) try { return decode(stored) } catch (_: IllegalArgumentException) { }
        // Migrate the previous release's keys from the same preference file.
        fun keys(key: String) = (values[key] as? Set<*>)?.filterIsInstance<String>()
            .orEmpty().filter(LauncherLayout::validAppKey)
        val favorites = keys("favorites").sorted()
        return LauncherLayout(
            pages = favorites.chunked(100).map { page -> page.map { HomeItem(it, listOf(it)) } },
            dock = favorites.take(5), hidden = keys("hidden_apps").toSet(),
            columns = values["grid_columns"] as? Int ?: 4,
            iconSize = values["icon_size_dp"] as? Int ?: 56,
            labels = values["show_labels"] as? Boolean ?: true,
            locked = values["layout_locked"] as? Boolean ?: false
        ).normalized()
    }

    fun write(prefs: SharedPreferences, layout: LauncherLayout) {
        prefs.edit().putString(KEY, encode(layout.normalized())).apply()
    }

    fun encode(layout: LauncherLayout): String = JSONObject().apply {
        put("version", 1)
        put("pages", JSONArray().apply { layout.pages.forEach { page -> put(JSONArray().apply { page.forEach { item ->
            put(JSONObject().put("id", item.id).put("apps", JSONArray(item.apps)).put("name", item.name).put("silver", item.silver))
        } }) } })
        put("dock", JSONArray(layout.dock)); put("hidden", JSONArray(layout.hidden.toList()))
        put("columns", layout.columns); put("iconSize", layout.iconSize); put("labels", layout.labels); put("locked", layout.locked)
        put("drawerStyle", layout.drawerStyle); put("folderStyle", layout.folderStyle)
        put("swipeDown", layout.swipeDown); put("doubleTap", layout.doubleTap)
    }.toString()

    fun decode(raw: String): LauncherLayout {
        require(raw.length <= 1_000_000) { "Backup is too large" }
        try {
            val json = JSONObject(raw)
            require(json.getInt("version") == 1) { "Unsupported layout version" }
            val pages = json.getJSONArray("pages")
            require(pages.length() in 1..12) { "Invalid page count" }
            val parsed = (0 until pages.length()).map { i ->
                val page = pages.getJSONArray(i)
                require(page.length() <= 100) { "Too many items" }
                (0 until page.length()).map { j ->
                    val item = page.getJSONObject(j)
                    val id = item.getString("id")
                    require(id.length in 1..520)
                    HomeItem(id, strings(item.getJSONArray("apps")), item.optString("name", "Folder"), item.optBoolean("silver"))
                }
            }
            return LauncherLayout(parsed, strings(json.optJSONArray("dock")), strings(json.optJSONArray("hidden")).toSet(),
                json.optInt("columns", 4), json.optInt("iconSize", 56), json.optBoolean("labels", true), json.optBoolean("locked"),
                json.optString("drawerStyle", "Grid"), json.optString("folderStyle", "Glass"),
                json.optString("swipeDown", "Search"), json.optString("doubleTap", "Search")).normalized()
        } catch (e: org.json.JSONException) { throw IllegalArgumentException("Invalid layout file", e) }
    }

    private fun strings(array: JSONArray?): List<String> = if (array == null) emptyList() else {
        require(array.length() <= 2000)
        (0 until array.length()).map { array.getString(it) }
    }
}
