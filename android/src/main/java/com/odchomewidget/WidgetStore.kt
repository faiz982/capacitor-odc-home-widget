package com.odchomewidget

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the last JSON the app sent (one entry per widget kind), the brand theme,
 * and widget toggle events waiting to be delivered to the app. Widgets read from
 * here, so they keep their data while the app is closed and after a reboot.
 */
object WidgetStore {
    private const val PREFS_NAME = "odc_home_widget"
    private const val KEY_THEME = "__theme"
    private const val KEY_EVENTS = "__events"
    private const val MAX_EVENTS = 100

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Writes synchronously so a refresh right after this call sees the new data. */
    fun save(context: Context, kind: WidgetKind, json: String): Boolean =
        prefs(context).edit().putString(kind.name, json).commit()

    fun read(context: Context, kind: WidgetKind): String? =
        prefs(context).getString(kind.name, null)

    fun clear(context: Context, kind: WidgetKind): Boolean =
        prefs(context).edit().remove(kind.name).commit()

    fun saveTheme(context: Context, json: String?): Boolean =
        prefs(context).edit().apply { if (json == null) remove(KEY_THEME) else putString(KEY_THEME, json) }.commit()

    fun readTheme(context: Context): String? = prefs(context).getString(KEY_THEME, null)

    @Synchronized
    fun addEvent(context: Context, event: JSONObject) {
        val events = readEvents(context)
        events.put(event)
        while (events.length() > MAX_EVENTS) events.remove(0)
        prefs(context).edit().putString(KEY_EVENTS, events.toString()).commit()
    }

    /** Returns and removes every pending event, oldest first. */
    @Synchronized
    fun drainEvents(context: Context): List<JSONObject> {
        val events = readEvents(context)
        if (events.length() == 0) return emptyList()
        prefs(context).edit().remove(KEY_EVENTS).commit()
        return events.objects(MAX_EVENTS)
    }

    private fun readEvents(context: Context): JSONArray =
        runCatching { JSONArray(prefs(context).getString(KEY_EVENTS, "[]")) }.getOrDefault(JSONArray())
}
