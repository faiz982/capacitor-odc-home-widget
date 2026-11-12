package com.odchomewidget

import org.json.JSONException
import org.json.JSONObject

/** Checks the WIDGET JSON before it is saved, with messages fit to show to a developer. */
object WidgetValidator {
    /** @throws IllegalArgumentException naming the first problem. */
    fun validate(kind: WidgetKind, json: String): JSONObject {
        val obj = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("data must be a JSON object string: ${e.message}")
        }
        val layout = obj.valueOf("layout")
            ?: throw IllegalArgumentException("${kind.name} data needs a \"layout\" field")
        if (layout !is JSONObject) {
            throw IllegalArgumentException("\"layout\" must be an object like {\"type\":\"column\",\"children\":[...]}")
        }
        LayoutRenderer.validate(layout)
        obj.obj("theme")?.let { Themes.validate(it) }
        obj.valueOf("background")?.let { bg ->
            if (bg !is JSONObject) {
                throw IllegalArgumentException("\"background\" must be an object like {\"gradient\":[\"#8BE04E\",\"#1BB8A9\"]}")
            }
            WidgetBackground.validate(bg)
        }
        return obj
    }
}
