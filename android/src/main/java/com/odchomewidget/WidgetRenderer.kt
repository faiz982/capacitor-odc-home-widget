package com.odchomewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/** Turns saved JSON into RemoteViews and wires taps back into the host app. */
object WidgetRenderer {
    const val EXTRA_DEEP_LINK = "odcWidgetDeepLink"
    const val EXTRA_KIND = "odcWidgetKind"

    private val LIST_ROWS = intArrayOf(R.id.odc_widget_row1, R.id.odc_widget_row2, R.id.odc_widget_row3)
    private val ACTION_BUTTONS = intArrayOf(R.id.odc_widget_action1, R.id.odc_widget_action2, R.id.odc_widget_action3)

    /** Redraws every widget of [kind] currently on the home screen. */
    fun refresh(context: Context, kind: WidgetKind) {
        val manager = AppWidgetManager.getInstance(context)
        update(context, manager, kind, manager.getAppWidgetIds(kind.componentName(context)))
    }

    fun update(context: Context, manager: AppWidgetManager, kind: WidgetKind, ids: IntArray) {
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(context, kind))
    }

    fun build(context: Context, kind: WidgetKind): RemoteViews {
        val data = WidgetStore.read(context, kind)?.let { json ->
            runCatching { WidgetDataParser.parse(kind, json) }.getOrNull()
        }
        return when (data) {
            is WidgetData.Count -> count(context, data)
            is WidgetData.Items -> list(context, data)
            is WidgetData.Actions -> actions(context, data)
            null -> empty(context, kind)
        }
    }

    private fun layoutFor(kind: WidgetKind) = when (kind) {
        WidgetKind.COUNT -> R.layout.odc_widget_count
        WidgetKind.LIST -> R.layout.odc_widget_list
        WidgetKind.ACTIONS -> R.layout.odc_widget_actions
    }

    private fun empty(context: Context, kind: WidgetKind): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(kind))
        views.setViewVisibility(R.id.odc_widget_empty, View.VISIBLE)
        views.setViewVisibility(R.id.odc_widget_content, View.GONE)
        views.setOnClickPendingIntent(R.id.odc_widget_root, tapIntent(context, kind, null, 0))
        return views
    }

    private fun content(context: Context, kind: WidgetKind, title: String): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(kind))
        views.setViewVisibility(R.id.odc_widget_empty, View.GONE)
        views.setViewVisibility(R.id.odc_widget_content, View.VISIBLE)
        setTextOrHide(views, R.id.odc_widget_title, title)
        return views
    }

    private fun count(context: Context, data: WidgetData.Count): RemoteViews {
        val views = content(context, WidgetKind.COUNT, data.title)
        views.setTextViewText(R.id.odc_widget_value, data.value)
        setTextOrHide(views, R.id.odc_widget_subtitle, data.subtitle)
        views.setOnClickPendingIntent(R.id.odc_widget_root, tapIntent(context, WidgetKind.COUNT, data.deepLink, 0))
        return views
    }

    private fun list(context: Context, data: WidgetData.Items): RemoteViews {
        val views = content(context, WidgetKind.LIST, data.title)
        views.setOnClickPendingIntent(R.id.odc_widget_root, tapIntent(context, WidgetKind.LIST, data.deepLink, 0))
        views.setViewVisibility(R.id.odc_widget_list_none, if (data.items.isEmpty()) View.VISIBLE else View.GONE)
        LIST_ROWS.forEachIndexed { i, rowId ->
            val item = data.items.getOrNull(i)
            if (item == null) {
                views.setViewVisibility(rowId, View.GONE)
            } else {
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(rowId, item.text)
                // A row without its own link falls back to the list's link.
                val link = item.deepLink.ifBlank { data.deepLink }
                views.setOnClickPendingIntent(rowId, tapIntent(context, WidgetKind.LIST, link, i + 1))
            }
        }
        return views
    }

    private fun actions(context: Context, data: WidgetData.Actions): RemoteViews {
        val views = content(context, WidgetKind.ACTIONS, data.title)
        views.setOnClickPendingIntent(R.id.odc_widget_root, tapIntent(context, WidgetKind.ACTIONS, null, 0))
        ACTION_BUTTONS.forEachIndexed { i, buttonId ->
            val action = data.actions.getOrNull(i)
            if (action == null) {
                views.setViewVisibility(buttonId, View.GONE)
            } else {
                views.setViewVisibility(buttonId, View.VISIBLE)
                views.setTextViewText(buttonId, action.text)
                views.setOnClickPendingIntent(buttonId, tapIntent(context, WidgetKind.ACTIONS, action.deepLink, i + 1))
            }
        }
        return views
    }

    private fun setTextOrHide(views: RemoteViews, viewId: Int, text: String) {
        if (text.isBlank()) {
            views.setViewVisibility(viewId, View.GONE)
        } else {
            views.setViewVisibility(viewId, View.VISIBLE)
            views.setTextViewText(viewId, text)
        }
    }

    /**
     * Opens the host app's launcher activity with the deep link as an extra.
     * CLEAR_TOP + SINGLE_TOP reuse the running activity (onNewIntent) instead of
     * stacking a second copy, so no launchMode build action is normally needed.
     * Every view gets its own request code; FLAG_UPDATE_CURRENT makes sure the
     * extras are replaced when the data changes.
     */
    private fun tapIntent(context: Context, kind: WidgetKind, deepLink: String?, slot: Int): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        launch.putExtra(EXTRA_KIND, kind.name)
        if (!deepLink.isNullOrBlank()) launch.putExtra(EXTRA_DEEP_LINK, deepLink)

        val requestCode = kind.ordinal * 10 + slot
        return PendingIntent.getActivity(
            context,
            requestCode,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
