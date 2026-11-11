package com.odchomewidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject

/**
 * Handles taps that change state on the widget itself (TOGGLES tiles and
 * CHECKLIST rows) without opening the app. The new state is saved and redrawn,
 * and a `widgetToggle` event is queued for the app: delivered right away when
 * the app is running, otherwise the next time it starts or resumes.
 */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Taps.ACTION_TOGGLE) return
        val kind = WidgetKind.parse(intent.getStringExtra(Taps.EXTRA_KIND)) ?: return
        val itemId = intent.getStringExtra(Taps.EXTRA_ITEM_ID) ?: return
        val on = flip(context, kind, itemId) ?: return
        WidgetRenderer.refresh(context, kind)
        WidgetStore.addEvent(
            context,
            JSONObject().put("kind", kind.name).put("id", itemId).put("on", on),
        )
        OdcHomeWidgetPlugin.deliverPendingEvents()
    }

    /** Flips the item's boolean in the saved JSON and returns its new value. */
    private fun flip(context: Context, kind: WidgetKind, itemId: String): Boolean? {
        val json = WidgetStore.read(context, kind)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
        val listField = if (kind == WidgetKind.TOGGLES) "tiles" else "items"
        val stateField = if (kind == WidgetKind.TOGGLES) "on" else "checked"
        val list = json.arr(listField) ?: return null
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            if (item.str("id") != itemId) continue
            val newValue = !item.bool(stateField)
            item.put(item.keyFor(stateField) ?: stateField, newValue)
            WidgetStore.save(context, kind, json.toString())
            return newValue
        }
        return null
    }
}
