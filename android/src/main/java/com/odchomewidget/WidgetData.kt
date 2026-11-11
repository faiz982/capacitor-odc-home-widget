package com.odchomewidget

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class WidgetLink(val text: String, val deepLink: String)

sealed class WidgetData {
    data class Count(
        val title: String,
        val value: String,
        val subtitle: String,
        val deepLink: String,
    ) : WidgetData()

    data class Items(
        val title: String,
        val items: List<WidgetLink>,
        val deepLink: String,
    ) : WidgetData()

    data class Actions(
        val title: String,
        val actions: List<WidgetLink>,
    ) : WidgetData()
}

/**
 * Parses the JSON shapes:
 *   COUNT   {title, value, subtitle, deepLink}
 *   LIST    {title, items:[{text, deepLink}], deepLink}   (first 3 items used)
 *   ACTIONS {title, actions:[{label, deepLink}]}          (first 3 actions used)
 *
 * Keys are matched case-insensitively, because ODC's JSONSerialize writes
 * structure attribute names as they are named in Studio (Title, Value, ...).
 */
object WidgetDataParser {
    const val MAX_ROWS = 3

    /** @throws IllegalArgumentException with a message fit to show to a developer. */
    fun parse(kind: WidgetKind, json: String): WidgetData {
        val obj = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("data must be a JSON object string: ${e.message}")
        }
        return when (kind) {
            WidgetKind.COUNT -> {
                if (obj.find("value") == null) {
                    throw IllegalArgumentException("COUNT data needs a \"value\" field")
                }
                WidgetData.Count(
                    title = obj.str("title"),
                    value = obj.str("value"),
                    subtitle = obj.str("subtitle"),
                    deepLink = obj.str("deepLink"),
                )
            }
            WidgetKind.LIST -> WidgetData.Items(
                title = obj.str("title"),
                items = links(obj, kind, "items", "text"),
                deepLink = obj.str("deepLink"),
            )
            WidgetKind.ACTIONS -> WidgetData.Actions(
                title = obj.str("title"),
                actions = links(obj, kind, "actions", "label"),
            )
        }
    }

    private fun links(obj: JSONObject, kind: WidgetKind, field: String, textField: String): List<WidgetLink> {
        val arr = obj.find(field) as? JSONArray
            ?: throw IllegalArgumentException("${kind.name} data needs a \"$field\" array")
        return (0 until minOf(arr.length(), MAX_ROWS)).map { i ->
            val item = arr.optJSONObject(i)
                ?: throw IllegalArgumentException("$field[$i] must be an object")
            WidgetLink(text = item.str(textField), deepLink = item.str("deepLink"))
        }
    }

    /** Case-insensitive key lookup; returns null for missing keys and JSON null. */
    private fun JSONObject.find(name: String): Any? {
        val key = if (has(name)) name else keys().asSequence().firstOrNull { it.equals(name, ignoreCase = true) }
        if (key == null || isNull(key)) return null
        return opt(key)
    }

    private fun JSONObject.str(name: String): String = find(name)?.toString() ?: ""
}
