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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        WidgetRenderer.update(context, appWidgetManager, kind, intArrayOf(appWidgetId))
    }
}

class CountWidgetProvider : BaseWidgetProvider(WidgetKind.COUNT)
class ListWidgetProvider : BaseWidgetProvider(WidgetKind.LIST)
class ActionsWidgetProvider : BaseWidgetProvider(WidgetKind.ACTIONS)
class InfoWidgetProvider : BaseWidgetProvider(WidgetKind.INFO)
class StatsWidgetProvider : BaseWidgetProvider(WidgetKind.STATS)
class HeroWidgetProvider : BaseWidgetProvider(WidgetKind.HERO)
class ArticlesWidgetProvider : BaseWidgetProvider(WidgetKind.ARTICLES)
class GridWidgetProvider : BaseWidgetProvider(WidgetKind.GRID)
class MessagesWidgetProvider : BaseWidgetProvider(WidgetKind.MESSAGES)
class TogglesWidgetProvider : BaseWidgetProvider(WidgetKind.TOGGLES)
class ChecklistWidgetProvider : BaseWidgetProvider(WidgetKind.CHECKLIST)
