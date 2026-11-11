package com.odchomewidget

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Checks that the JSON sent for a kind has the fields its template needs. */
object WidgetValidator {
    /** Name of the array (or value) each kind requires; null when nothing is required. */
    fun requiredField(kind: WidgetKind): String? = when (kind) {
        WidgetKind.COUNT -> "value"
        WidgetKind.LIST, WidgetKind.ARTICLES, WidgetKind.GRID, WidgetKind.CHECKLIST -> "items"
        WidgetKind.ACTIONS -> "actions"
        WidgetKind.INFO -> "rows"
        WidgetKind.STATS, WidgetKind.TOGGLES -> "tiles"
        WidgetKind.MESSAGES -> "messages"
        WidgetKind.HERO -> null
        WidgetKind.WIDGET -> "layout"
    }

    /** @throws IllegalArgumentException with a message fit to show to a developer. */
    fun validate(kind: WidgetKind, json: String): JSONObject {
        val obj = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("data must be a JSON object string: ${e.message}")
        }
        val field = requiredField(kind) ?: return obj
        val value = obj.valueOf(field)
            ?: throw IllegalArgumentException("${kind.name} data needs a \"$field\" field")
        if (kind == WidgetKind.WIDGET) {
            if (value !is JSONObject) throw IllegalArgumentException("WIDGET \"layout\" must be an object like {\"type\":\"column\",\"children\":[...]}")
            LayoutRenderer.validate(value)
            obj.obj("theme")?.let { Themes.validate(it) }
            obj.valueOf("background")?.let { bg ->
                if (bg !is JSONObject) throw IllegalArgumentException("WIDGET \"background\" must be an object like {\"gradient\":[\"#8BE04E\",\"#1BB8A9\"]}")
                WidgetBackground.validate(bg)
            }
            return obj
        }
        if (field != "value" && value !is JSONArray) {
            throw IllegalArgumentException("${kind.name} \"$field\" must be an array")
        }
        if (kind == WidgetKind.TOGGLES || kind == WidgetKind.CHECKLIST) {
            obj.arr(field).objects(WidgetRenderer.MAX_COLLECTION_ITEMS).forEachIndexed { i, item ->
                if (item.str("id").isBlank()) {
                    throw IllegalArgumentException("$field[$i] needs an \"id\" so taps can be reported to the app")
                }
            }
        }
        obj.obj("theme")?.let { Themes.validate(it) }
        return obj
    }
}
