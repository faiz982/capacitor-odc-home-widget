package com.odchomewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject

/** Shared helpers for setting text and visibility on RemoteViews. */
internal object Bind {
    fun show(views: RemoteViews, viewId: Int, visible: Boolean) =
        views.setViewVisibility(viewId, if (visible) View.VISIBLE else View.GONE)

    /** Sets themed text, or hides the view when [value] is blank. */
    fun text(views: RemoteViews, viewId: Int, value: CharSequence, painter: Painter, role: (Palette) -> Int) {
        if (value.isBlank()) {
            views.setViewVisibility(viewId, View.GONE)
            return
        }
        views.setViewVisibility(viewId, View.VISIBLE)
        views.setTextViewText(viewId, value)
        painter.text(viewId, role)
    }
}

/** Turns the saved JSON into RemoteViews. */
object WidgetRenderer {

    /** Redraws every widget of [kind] currently on the home screen. */
    fun refresh(context: Context, kind: WidgetKind) {
        val manager = AppWidgetManager.getInstance(context)
        update(context, manager, kind, manager.getAppWidgetIds(kind.componentName(context)))
    }

    fun update(context: Context, manager: AppWidgetManager, kind: WidgetKind, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, build(context, kind, it)) }
    }

    /** The saved data for [kind], or null when none is saved or it no longer validates. */
    fun readData(context: Context, kind: WidgetKind): JSONObject? =
        WidgetStore.read(context, kind)?.let { runCatching { WidgetValidator.validate(kind, it) }.getOrNull() }

    fun build(context: Context, kind: WidgetKind, widgetId: Int): RemoteViews {
        val json = readData(context, kind)
        val views = RemoteViews(context.packageName, kind.layout)
        val p = Painter(context, views, Themes.resolve(context, json))
        p.tint(R.id.odc_w_bg) { it.background }

        if (json == null) {
            Bind.show(views, R.id.odc_widget_empty, true)
            Bind.show(views, R.id.odc_widget_content, false)
            p.text(R.id.odc_widget_empty) { it.muted }
            views.setOnClickPendingIntent(R.id.odc_widget_root, Taps.open(context, kind, null, 0))
            return views
        }

        Bind.show(views, R.id.odc_widget_empty, false)
        Bind.show(views, R.id.odc_widget_content, true)
        views.setOnClickPendingIntent(R.id.odc_widget_root, Taps.open(context, kind, json.str("deepLink"), 0))

        json.obj("background")?.let { bg ->
            runCatching { WidgetBackground.draw(context, widgetId, bg) }.getOrNull()?.let { bitmap ->
                p.tint(R.id.odc_w_bg) { Color.TRANSPARENT }
                views.setImageViewBitmap(R.id.odc_w_bg, bitmap)
            }
        }
        LayoutRenderer.render(context, views, R.id.odc_widget_content, json, p.theme)
        return views
    }
}
