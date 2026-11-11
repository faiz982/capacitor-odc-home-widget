package com.odchomewidget

import android.view.View
import android.widget.RemoteViews

/** Built-in Material icons that JSON can refer to by name, e.g. "icon": "search". */
object WidgetIcons {
    private val icons = mapOf(
        "search" to R.drawable.odc_w_ic_search,
        "refresh" to R.drawable.odc_w_ic_refresh,
        "add" to R.drawable.odc_w_ic_add,
        "check" to R.drawable.odc_w_ic_check,
        "check_circle" to R.drawable.odc_w_ic_check_circle,
        "circle" to R.drawable.odc_w_ic_circle,
        "chevron_left" to R.drawable.odc_w_ic_chevron_left,
        "chevron_right" to R.drawable.odc_w_ic_chevron_right,
        "arrow_up" to R.drawable.odc_w_ic_arrow_up,
        "arrow_down" to R.drawable.odc_w_ic_arrow_down,
        "favorite" to R.drawable.odc_w_ic_favorite,
        "star" to R.drawable.odc_w_ic_star,
        "home" to R.drawable.odc_w_ic_home,
        "play" to R.drawable.odc_w_ic_play,
        "camera" to R.drawable.odc_w_ic_camera,
        "upload" to R.drawable.odc_w_ic_upload,
        "menu" to R.drawable.odc_w_ic_menu,
        "lightbulb" to R.drawable.odc_w_ic_lightbulb,
        "tv" to R.drawable.odc_w_ic_tv,
        "sun" to R.drawable.odc_w_ic_sun,
        "cloud" to R.drawable.odc_w_ic_cloud,
        "rain" to R.drawable.odc_w_ic_rain,
        "calendar" to R.drawable.odc_w_ic_calendar,
        "location" to R.drawable.odc_w_ic_location,
        "reply" to R.drawable.odc_w_ic_reply,
        "chat" to R.drawable.odc_w_ic_chat,
        "bell" to R.drawable.odc_w_ic_bell,
        "bolt" to R.drawable.odc_w_ic_bolt,
        "trending_up" to R.drawable.odc_w_ic_trending_up,
        "info" to R.drawable.odc_w_ic_info,
    )

    fun res(name: String?): Int? =
        name?.trim()?.lowercase()?.replace('-', '_')?.replace(' ', '_')?.let { icons[it] }

    /** Shows the named icon tinted with [role], or hides the view when the name is unknown. */
    fun bind(views: RemoteViews, viewId: Int, name: String?, painter: Painter, role: (Palette) -> Int): Boolean {
        val res = res(name)
        if (res == null) {
            views.setViewVisibility(viewId, View.GONE)
            return false
        }
        views.setViewVisibility(viewId, View.VISIBLE)
        views.setImageViewResource(viewId, res)
        painter.tint(viewId, role)
        return true
    }
}
