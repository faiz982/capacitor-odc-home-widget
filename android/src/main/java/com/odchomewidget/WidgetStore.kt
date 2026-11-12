package com.odchomewidget

import android.content.Context
import android.content.SharedPreferences

/**
 * Keeps the last JSON the app sent and the brand theme. Widgets read from here,
 * so they keep their data while the app is closed and after a reboot.
 */
object WidgetStore {
    private const val PREFS_NAME = "odc_home_widget"
    private const val KEY_THEME = "__theme"

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
}
