package com.rezoxnemesis.kebtee.notifications

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class NotificationRecord(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

object NotificationFeedStore {
    private const val PREFS = "kebtee_notifications"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 40

    fun add(context: Context, record: NotificationRecord) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = read(prefs.getString(KEY_ITEMS, "[]"))
            .filterNot { it.key == record.key }
            .toMutableList()
        items.add(0, record)
        prefs.edit().putString(KEY_ITEMS, encode(items.take(MAX_ITEMS))).apply()
    }

    fun remove(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ITEMS, encode(read(prefs.getString(KEY_ITEMS, "[]")).filterNot { it.key == key })).apply()
    }

    fun list(context: Context): List<NotificationRecord> =
        read(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]"))

    private fun encode(items: List<NotificationRecord>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("key", item.key)
                put("packageName", item.packageName)
                put("appLabel", item.appLabel)
                put("title", item.title)
                put("text", item.text)
                put("timestamp", item.timestamp)
            })
        }
    }.toString()

    private fun read(raw: String?): List<NotificationRecord> = runCatching {
        val array = JSONArray(raw ?: "[]")
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(NotificationRecord(
                    key = item.optString("key"),
                    packageName = item.optString("packageName"),
                    appLabel = item.optString("appLabel"),
                    title = item.optString("title"),
                    text = item.optString("text"),
                    timestamp = item.optLong("timestamp")
                ))
            }
        }
    }.getOrElse { emptyList() }
}