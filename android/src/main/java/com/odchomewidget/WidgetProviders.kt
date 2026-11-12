package com.odchomewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/**
 * Providers never run OutSystems code: they only draw the last JSON saved in
 * [WidgetStore]. Updates come from the app (setWidgetData / refresh / setTheme),
 * from the system when a widget is added or resized, and after a reboot.
 */
abstract class BaseWidgetProvider(private val kind: WidgetKind) : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetRenderer.update(context, appWidgetManager, kind, appWidgetIds)
    }

    // Redraw on resize: the background bitmap is drawn at the widget's size.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        WidgetRenderer.update(context, appWidgetManager, kind, intArrayOf(appWidgetId))
    }
}

class CustomWidgetProvider : BaseWidgetProvider(WidgetKind.WIDGET)
