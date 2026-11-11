package com.odchomewidget

import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

/** The three widget templates. The name is what JavaScript passes as `kind`. */
enum class WidgetKind(val providerClass: Class<out AppWidgetProvider>) {
    COUNT(CountWidgetProvider::class.java),
    LIST(ListWidgetProvider::class.java),
    ACTIONS(ActionsWidgetProvider::class.java);

    fun componentName(context: Context) = ComponentName(context, providerClass)

    companion object {
        fun parse(value: String?): WidgetKind? {
            val v = value?.trim()?.uppercase() ?: return null
            return values().firstOrNull { it.name == v }
        }
    }
}
