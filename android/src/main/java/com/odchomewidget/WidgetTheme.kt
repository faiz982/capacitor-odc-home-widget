package com.odchomewidget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import org.json.JSONObject

/** The colors a widget is drawn with. Every field can be set from JSON by its name. */
data class Palette(
    val background: Int,
    val onBackground: Int,
    val muted: Int,
    val accent: Int,
    val onAccent: Int,
    val tile: Int,
    val onTile: Int,
    val tileActive: Int,
    val onTileActive: Int,
    val positive: Int,
    val negative: Int,
)

class WidgetTheme(val light: Palette, val dark: Palette)

/**
 * Brand colors. Resolution order (later wins): built-in defaults, the app-wide
 * theme from setTheme(), then the widget JSON's own "theme". A "dark" object
 * inside a theme sets the dark-mode colors; without it, the same brand colors
 * are used in dark mode too.
 */
object Themes {
    private fun c(hex: Long) = hex.toInt()

    val LIGHT = Palette(
        background = c(0xFFFFFFFF), onBackground = c(0xFF1B1B1F), muted = c(0xFF5F6368),
        accent = c(0xFF1A73E8), onAccent = c(0xFFFFFFFF),
        tile = c(0xFFF1F3F4), onTile = c(0xFF1B1B1F),
        tileActive = c(0xFFD3E3FD), onTileActive = c(0xFF041E49),
        positive = c(0xFF188038), negative = c(0xFFD93025),
    )
    val DARK = Palette(
        background = c(0xFF1F1F1F), onBackground = c(0xFFE3E3E3), muted = c(0xFFA8ABAF),
        accent = c(0xFFA8C7FA), onAccent = c(0xFF062E6F),
        tile = c(0xFF2D2F31), onTile = c(0xFFE3E3E3),
        tileActive = c(0xFF0842A0), onTileActive = c(0xFFD3E3FD),
        positive = c(0xFF81C995), negative = c(0xFFF28B82),
    )
    val DEFAULT = WidgetTheme(LIGHT, DARK)

    private val KEYS = listOf(
        "background", "onBackground", "muted", "accent", "onAccent",
        "tile", "onTile", "tileActive", "onTileActive", "positive", "negative",
    )

    fun resolve(context: Context, widget: JSONObject?): WidgetTheme {
        var theme = DEFAULT
        WidgetStore.readTheme(context)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?.let { theme = overlay(theme, it) }
        widget?.obj("theme")?.let { theme = overlay(theme, it) }
        return theme
    }

    /** @throws IllegalArgumentException naming the first color that can't be parsed. */
    fun validate(theme: JSONObject) {
        listOfNotNull(theme, theme.obj("dark")).forEach { layer ->
            KEYS.forEach { key ->
                val value = layer.str(key)
                if (value.isNotBlank() && parse(value) == null) {
                    throw IllegalArgumentException("theme.$key \"$value\" is not a color like #1A73E8 or #FF1A73E8")
                }
            }
        }
    }

    private fun overlay(base: WidgetTheme, layer: JSONObject): WidgetTheme {
        val dark = layer.obj("dark")
        return WidgetTheme(
            light = apply(base.light, layer, null),
            dark = if (dark != null) apply(base.dark, dark, layer) else apply(base.dark, layer, null),
        )
    }

    private fun apply(p: Palette, layer: JSONObject, fallback: JSONObject?): Palette {
        fun pick(key: String, current: Int): Int =
            parse(layer.str(key)) ?: fallback?.let { parse(it.str(key)) } ?: current
        return Palette(
            background = pick("background", p.background),
            onBackground = pick("onBackground", p.onBackground),
            muted = pick("muted", p.muted),
            accent = pick("accent", p.accent),
            onAccent = pick("onAccent", p.onAccent),
            tile = pick("tile", p.tile),
            onTile = pick("onTile", p.onTile),
            tileActive = pick("tileActive", p.tileActive),
            onTileActive = pick("onTileActive", p.onTileActive),
            positive = pick("positive", p.positive),
            negative = pick("negative", p.negative),
        )
    }

    fun parse(value: String): Int? {
        val v = value.trim()
        if (v.isEmpty()) return null
        return runCatching { Color.parseColor(if (v.startsWith("#")) v else "#$v") }.getOrNull()
    }
}

/**
 * Applies palette colors to RemoteViews. On Android 12+ both the light and dark
 * color are sent, so the launcher switches them when dark mode changes; older
 * versions get the color for the current mode.
 */
class Painter(context: Context, private val views: RemoteViews, val theme: WidgetTheme) {
    private val night =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun tint(viewId: Int, role: (Palette) -> Int) = set(viewId, "setColorFilter", role)

    fun text(viewId: Int, role: (Palette) -> Int) = set(viewId, "setTextColor", role)

    fun progress(viewId: Int, role: (Palette) -> Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(
                viewId,
                "setProgressTintList",
                ColorStateList.valueOf(role(theme.light)),
                ColorStateList.valueOf(role(theme.dark)),
            )
        }
    }

    private fun set(viewId: Int, method: String, role: (Palette) -> Int) {
        val light = role(theme.light)
        val dark = role(theme.dark)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorInt(viewId, method, light, dark)
        } else {
            views.setInt(viewId, method, if (night) dark else light)
        }
    }
}
