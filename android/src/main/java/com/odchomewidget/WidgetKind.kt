package com.odchomewidget

import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

/**
 * The widget templates. The name is what JavaScript passes as `kind`.
 * [collection] templates scroll and are filled by [OdcWidgetCollectionService].
 */
enum class WidgetKind(
    val providerClass: Class<out AppWidgetProvider>,
    val layout: Int,
    val collection: Boolean = false,
) {
    COUNT(CountWidgetProvider::class.java, R.layout.odc_widget_count),
    LIST(ListWidgetProvider::class.java, R.layout.odc_widget_list),
    ACTIONS(ActionsWidgetProvider::class.java, R.layout.odc_widget_actions),
    INFO(InfoWidgetProvider::class.java, R.layout.odc_widget_info),
    STATS(StatsWidgetProvider::class.java, R.layout.odc_widget_stats),
    HERO(HeroWidgetProvider::class.java, R.layout.odc_widget_hero),
    ARTICLES(ArticlesWidgetProvider::class.java, R.layout.odc_widget_articles, collection = true),
    GRID(GridWidgetProvider::class.java, R.layout.odc_widget_grid, collection = true),
    MESSAGES(MessagesWidgetProvider::class.java, R.layout.odc_widget_messages, collection = true),
    TOGGLES(TogglesWidgetProvider::class.java, R.layout.odc_widget_toggles),
    CHECKLIST(ChecklistWidgetProvider::class.java, R.layout.odc_widget_checklist, collection = true),

    /** Fully custom: the JSON "layout" tree is drawn by [LayoutRenderer]. */
    WIDGET(CustomWidgetProvider::class.java, R.layout.odc_widget_custom);

    fun componentName(context: Context) = ComponentName(context, providerClass)

    companion object {
        fun parse(value: String?): WidgetKind? {
            val v = value?.trim()?.uppercase() ?: return null
            return values().firstOrNull { it.name == v }
        }

        val names: String get() = values().joinToString(", ") { it.name }
    }
}
