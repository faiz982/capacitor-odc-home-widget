package com.odchomewidget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Buttons that work on the home screen without opening the app. Any element can
 * have an "action":
 *
 * | type    | fields                                   | does                                   |
 * |---------|------------------------------------------|----------------------------------------|
 * | open    | deepLink                                 | opens the app (widgetTap event)        |
 * | set     | key, value                               | sets a value                           |
 * | toggle  | key                                      | flips a true/false value               |
 * | add     | key, by (1), min, max                    | adds to a number                       |
 * | cycle   | key, values [..]                         | moves to the next value in the list    |
 * | refresh |                                          | runs the widget's "refresh" now        |
 * | http    | url, method (POST), headers, body, key, merge (true) | calls a URL in the background |
 *
 * set / toggle / add / cycle redraw at once and fire `widgetAction` in the app.
 * Every action may have an "id", passed back in the event.
 */
object WidgetActions {
    val TYPES = listOf("open", "set", "toggle", "add", "cycle", "refresh", "http")
    const val ACTION_RUN = "com.odchomewidget.ACTION_RUN"
    const val EXTRA_ACTION = "odcWidgetAction"

    fun validate(action: JSONObject, path: String) {
        val type = action.str("type").trim().lowercase()
        if (type !in TYPES) throw IllegalArgumentException("$path.type \"${action.str("type")}\" must be one of ${TYPES.joinToString()}")
        fun need(field: String) {
            if (action.str(field).isBlank() && action.valueOf(field) !is JSONArray) {
                throw IllegalArgumentException("$path needs \"$field\" for a $type action")
            }
        }
        when (type) {
            "open" -> need("deepLink")
            "set" -> need("key")
            "toggle", "add" -> need("key")
            "cycle" -> {
                need("key")
                if (action.arr("values")?.length()?.let { it > 0 } != true) {
                    throw IllegalArgumentException("$path needs a non-empty \"values\" array for a cycle action")
                }
            }
            "http" -> {
                need("url")
                val url = action.str("url")
                if (!Values.hasPlaceholder(url) && !url.startsWith("https://", true) && !url.startsWith("http://", true)) {
                    throw IllegalArgumentException("$path.url must start with https://")
                }
            }
        }
    }

    /** The tap target for an element's (already resolved) [action]. */
    fun pendingIntent(context: Context, action: JSONObject, slot: Int): PendingIntent {
        val type = action.str("type").trim().lowercase()
        if (type == "open") return Taps.open(context, WidgetKind.WIDGET, action.str("deepLink"), slot)
        val intent = Intent(context, WidgetActionReceiver::class.java)
            .setAction(ACTION_RUN)
            .putExtra(EXTRA_ACTION, action.toString())
        return PendingIntent.getBroadcast(
            context,
            slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Runs a state action (set / toggle / add / cycle) and returns the new value. */
    fun applyLocal(context: Context, action: JSONObject): Any? {
        val type = action.str("type").trim().lowercase()
        val key = action.str("key").trim()
        var result: Any? = null
        WidgetStore.updateValues(context) { values ->
            val current = values.valueOf(key)
            result = when (type) {
                "set" -> action.valueOf("value") ?: ""
                "toggle" -> !Values.truthy(current ?: false)
                "add" -> {
                    val now = (current as? Number)?.toDouble() ?: current?.toString()?.toDoubleOrNull() ?: 0.0
                    var next = now + (action.num("by") ?: 1.0)
                    action.num("min")?.let { if (next < it) next = it }
                    action.num("max")?.let { if (next > it) next = it }
                    if (next == Math.floor(next)) next.toLong() else next
                }
                "cycle" -> {
                    val options = action.arr("values") ?: JSONArray()
                    val index = (0 until options.length()).firstOrNull { options.opt(it).toString() == current?.toString() } ?: -1
                    options.opt((index + 1) % options.length())
                }
                else -> return@updateValues
            }
            values.keyFor(key)?.let { if (it != key) values.remove(it) }
            values.put(key, result)
        }
        return result
    }

    /** Queues a `widgetAction` event and hands it to the app if it is running. */
    fun report(context: Context, action: JSONObject, value: Any?, ok: Boolean = true, error: String = "") {
        val event = JSONObject()
            .put("type", action.str("type").lowercase())
            .put("id", action.str("id"))
            .put("key", action.str("key"))
            .put("value", value ?: JSONObject.NULL)
            .put("ok", ok)
            .put("error", error)
        WidgetStore.addEvent(context, event)
        OdcHomeWidgetPlugin.deliverPendingEvents()
    }
}

/** Receives button taps from the home screen. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetActions.ACTION_RUN) return
        val action = runCatching { JSONObject(intent.getStringExtra(WidgetActions.EXTRA_ACTION) ?: return) }.getOrNull() ?: return
        val app = context.applicationContext
        when (action.str("type").trim().lowercase()) {
            "set", "toggle", "add", "cycle" -> {
                val pending = goAsync()
                Thread {
                    try {
                        val value = WidgetActions.applyLocal(app, action)
                        WidgetRenderer.refresh(app, WidgetKind.WIDGET)
                        WidgetActions.report(app, action, value)
                        // Images that a new value points at are fetched in the background.
                        WidgetSync.enqueue(app, WidgetSync.JOB_REDRAW, null)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
            "refresh" -> WidgetSync.enqueue(app, WidgetSync.JOB_REFRESH, null)
            "http" -> WidgetSync.enqueue(app, WidgetSync.JOB_HTTP, action)
        }
    }
}
