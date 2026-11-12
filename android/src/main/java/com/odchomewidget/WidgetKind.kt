package com.odchomewidget

import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

/**
 * The widget kinds. There is one: WIDGET, whose whole look comes from the JSON
 * "layout" tree drawn by [LayoutRenderer]. The name is what JavaScript passes as `kind`.
 */
enum class WidgetKind(val providerClass: Class<out AppWidgetProvider>, val layout: Int) {
    WIDGET(CustomWidgetProvider::class.java, R.layout.odc_widget_custom);

    fun componentName(context: Context) = ComponentName(context, providerClass)

    companion object {
        /** `kind` is optional: blank means WIDGET. */
        fun parse(value: String?): WidgetKind? {
            val v = value?.trim()?.uppercase()
            if (v.isNullOrEmpty()) return WIDGET
            return values().firstOrNull { it.name == v }
        }

        val names: String get() = values().joinToString(", ") { it.name }
    }
}
