package com.odchomewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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

    /** Sets text in a fixed color (used over images). */
    fun fixedText(views: RemoteViews, viewId: Int, value: String, color: Int) {
        if (value.isBlank()) {
            views.setViewVisibility(viewId, View.GONE)
            return
        }
        views.setViewVisibility(viewId, View.VISIBLE)
        views.setTextViewText(viewId, value)
        views.setTextColor(viewId, color)
    }
}

/** Turns saved JSON into RemoteViews for every kind. */
object WidgetRenderer {
    const val MAX_COLLECTION_ITEMS = 25

    private class Tile(val root: Int, val bg: Int, val icon: Int, val label: Int, val state: Int)
    private class InfoRow(val root: Int, val icon: Int, val label: Int, val sublabel: Int, val value: Int, val subvalue: Int)
    private class Stat(val root: Int, val ring: Int, val icon: Int, val value: Int, val label: Int)

    private val LIST_ROWS = intArrayOf(R.id.odc_w_row1, R.id.odc_w_row2, R.id.odc_w_row3)

    private val TILES = listOf(
        Tile(R.id.odc_w_tile1, R.id.odc_w_tile1_bg, R.id.odc_w_tile1_icon, R.id.odc_w_tile1_label, R.id.odc_w_tile1_state),
        Tile(R.id.odc_w_tile2, R.id.odc_w_tile2_bg, R.id.odc_w_tile2_icon, R.id.odc_w_tile2_label, R.id.odc_w_tile2_state),
        Tile(R.id.odc_w_tile3, R.id.odc_w_tile3_bg, R.id.odc_w_tile3_icon, R.id.odc_w_tile3_label, R.id.odc_w_tile3_state),
        Tile(R.id.odc_w_tile4, R.id.odc_w_tile4_bg, R.id.odc_w_tile4_icon, R.id.odc_w_tile4_label, R.id.odc_w_tile4_state),
        Tile(R.id.odc_w_tile5, R.id.odc_w_tile5_bg, R.id.odc_w_tile5_icon, R.id.odc_w_tile5_label, R.id.odc_w_tile5_state),
        Tile(R.id.odc_w_tile6, R.id.odc_w_tile6_bg, R.id.odc_w_tile6_icon, R.id.odc_w_tile6_label, R.id.odc_w_tile6_state),
    )

    private val INFO_ROWS = listOf(
        InfoRow(R.id.odc_w_irow1, R.id.odc_w_irow1_icon, R.id.odc_w_irow1_label, R.id.odc_w_irow1_sublabel, R.id.odc_w_irow1_value, R.id.odc_w_irow1_subvalue),
        InfoRow(R.id.odc_w_irow2, R.id.odc_w_irow2_icon, R.id.odc_w_irow2_label, R.id.odc_w_irow2_sublabel, R.id.odc_w_irow2_value, R.id.odc_w_irow2_subvalue),
        InfoRow(R.id.odc_w_irow3, R.id.odc_w_irow3_icon, R.id.odc_w_irow3_label, R.id.odc_w_irow3_sublabel, R.id.odc_w_irow3_value, R.id.odc_w_irow3_subvalue),
        InfoRow(R.id.odc_w_irow4, R.id.odc_w_irow4_icon, R.id.odc_w_irow4_label, R.id.odc_w_irow4_sublabel, R.id.odc_w_irow4_value, R.id.odc_w_irow4_subvalue),
        InfoRow(R.id.odc_w_irow5, R.id.odc_w_irow5_icon, R.id.odc_w_irow5_label, R.id.odc_w_irow5_sublabel, R.id.odc_w_irow5_value, R.id.odc_w_irow5_subvalue),
    )

    private val STATS = listOf(
        Stat(R.id.odc_w_stat1, R.id.odc_w_stat1_ring, R.id.odc_w_stat1_icon, R.id.odc_w_stat1_value, R.id.odc_w_stat1_label),
        Stat(R.id.odc_w_stat2, R.id.odc_w_stat2_ring, R.id.odc_w_stat2_icon, R.id.odc_w_stat2_value, R.id.odc_w_stat2_label),
        Stat(R.id.odc_w_stat3, R.id.odc_w_stat3_ring, R.id.odc_w_stat3_icon, R.id.odc_w_stat3_value, R.id.odc_w_stat3_label),
        Stat(R.id.odc_w_stat4, R.id.odc_w_stat4_ring, R.id.odc_w_stat4_icon, R.id.odc_w_stat4_value, R.id.odc_w_stat4_label),
    )

    // ---- Entry points -------------------------------------------------------

    /** Redraws every widget of [kind] currently on the home screen. */
    fun refresh(context: Context, kind: WidgetKind) {
        val manager = AppWidgetManager.getInstance(context)
        update(context, manager, kind, manager.getAppWidgetIds(kind.componentName(context)))
    }

    fun update(context: Context, manager: AppWidgetManager, kind: WidgetKind, ids: IntArray) {
        if (ids.isEmpty()) return
        ids.forEach { manager.updateAppWidget(it, build(context, kind, it)) }
        if (kind.collection) manager.notifyAppWidgetViewDataChanged(ids, R.id.odc_w_list)
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

        when (kind) {
            WidgetKind.COUNT -> count(context, views, p, json)
            WidgetKind.LIST -> list(context, views, p, json)
            WidgetKind.ACTIONS -> actions(context, views, p, json)
            WidgetKind.INFO -> info(context, views, p, json)
            WidgetKind.STATS -> stats(context, views, p, json)
            WidgetKind.HERO -> hero(context, views, p, json)
            WidgetKind.TOGGLES -> toggles(context, views, p, json)
            WidgetKind.WIDGET -> LayoutRenderer.render(context, views, R.id.odc_widget_content, json, p.theme)
            WidgetKind.ARTICLES, WidgetKind.GRID, WidgetKind.MESSAGES, WidgetKind.CHECKLIST ->
                collection(context, views, p, kind, json, widgetId)
        }
        return views
    }

    // ---- Header -------------------------------------------------------------

    /** Icon in an accent circle, title, and up to 2 icon buttons with their own deep links. */
    private fun header(context: Context, views: RemoteViews, p: Painter, kind: WidgetKind, json: JSONObject, onImage: Boolean = false) {
        val title = json.str("title")
        val icon = json.str("icon")
        val buttons = json.arr("buttons").objects(2)
        if (title.isBlank() && WidgetIcons.res(icon) == null && buttons.isEmpty()) {
            Bind.show(views, R.id.odc_w_header, false)
            return
        }
        Bind.show(views, R.id.odc_w_header, true)
        views.setTextViewText(R.id.odc_w_header_title, title)
        if (onImage) views.setTextColor(R.id.odc_w_header_title, Color.WHITE) else p.text(R.id.odc_w_header_title) { it.onBackground }

        val hasIcon = WidgetIcons.bind(views, R.id.odc_w_header_icon, icon, p) { it.onAccent }
        Bind.show(views, R.id.odc_w_header_icon_wrap, hasIcon)
        if (hasIcon) p.tint(R.id.odc_w_header_icon_bg) { it.accent }

        listOf(R.id.odc_w_header_btn1, R.id.odc_w_header_btn2).forEachIndexed { i, viewId ->
            val button = buttons.getOrNull(i)
            if (button != null && WidgetIcons.bind(views, viewId, button.str("icon"), p) { it.onBackground }) {
                if (onImage) views.setInt(viewId, "setColorFilter", Color.WHITE)
                views.setOnClickPendingIntent(viewId, Taps.open(context, kind, button.str("deepLink"), 90 + i))
            } else {
                Bind.show(views, viewId, false)
            }
        }
    }

    // ---- Templates ----------------------------------------------------------

    private fun count(context: Context, views: RemoteViews, p: Painter, json: JSONObject) {
        header(context, views, p, WidgetKind.COUNT, json)
        Bind.text(views, R.id.odc_w_value, json.str("value"), p) { it.accent }
        Bind.text(views, R.id.odc_w_subtitle, json.str("subtitle"), p) { it.muted }
    }

    private fun list(context: Context, views: RemoteViews, p: Painter, json: JSONObject) {
        header(context, views, p, WidgetKind.LIST, json)
        val items = json.arr("items").objects(LIST_ROWS.size)
        Bind.show(views, R.id.odc_w_none, items.isEmpty())
        p.text(R.id.odc_w_none) { it.muted }
        LIST_ROWS.forEachIndexed { i, rowId ->
            val item = items.getOrNull(i)
            if (item == null) {
                Bind.show(views, rowId, false)
            } else {
                Bind.text(views, rowId, item.str("text"), p) { it.onBackground }
                val link = item.str("deepLink").ifBlank { json.str("deepLink") }
                views.setOnClickPendingIntent(rowId, Taps.open(context, WidgetKind.LIST, link, i + 1))
            }
        }
    }

    private fun actions(context: Context, views: RemoteViews, p: Painter, json: JSONObject) {
        header(context, views, p, WidgetKind.ACTIONS, json)
        views.setOnClickPendingIntent(R.id.odc_widget_root, Taps.open(context, WidgetKind.ACTIONS, null, 0))
        val actions = json.arr("actions").objects(3)
        TILES.take(3).forEachIndexed { i, t ->
            val action = actions.getOrNull(i)
            if (action == null) {
                Bind.show(views, t.root, false)
                return@forEachIndexed
            }
            Bind.show(views, t.root, true)
            p.tint(t.bg) { it.tileActive }
            WidgetIcons.bind(views, t.icon, action.str("icon"), p) { it.onTileActive }
            Bind.text(views, t.label, action.str("label"), p) { it.onTileActive }
            Bind.show(views, t.state, false)
            views.setOnClickPendingIntent(t.root, Taps.open(context, WidgetKind.ACTIONS, action.str("deepLink"), i + 1))
        }
    }

    private fun toggles(context: Context, views: RemoteViews, p: Painter, json: JSONObject) {
        header(context, views, p, WidgetKind.TOGGLES, json)
        val tiles = json.arr("tiles").objects(TILES.size)
        TILES.forEachIndexed { i, t ->
            val tile = tiles.getOrNull(i)
            if (tile == null) {
                Bind.show(views, t.root, false)
                return@forEachIndexed
            }
            val on = tile.bool("on")
            val fg: (Palette) -> Int = if (on) Palette::onTileActive else Palette::onTile
            Bind.show(views, t.root, true)
            p.tint(t.bg, if (on) Palette::tileActive else Palette::tile)
            WidgetIcons.bind(views, t.icon, tile.str("icon"), p, fg)
            Bind.text(views, t.label, tile.str("label"), p, fg)
            val state = if (on) tile.str("onText").ifBlank { "On" } else tile.str("offText").ifBlank { "Off" }
            Bind.text(views, t.state, state, p, fg)
            views.setOnClickPendingIntent(t.root, Taps.toggle(context, WidgetKind.TOGGLES, tile.str("id"), i + 1))
        }
        Bind.show(views, R.id.odc_w_tilerow1, tiles.isNotEmpty())
        Bind.show(views, R.id.odc_w_tilerow2, tiles.size > 2)
        Bind.show(views, R.id.odc_w_tilerow3, tiles.size > 4)
    }

    private fun info(context: Context, views: RemoteViews, p: Painter, json: JSONObject) {
        header(context, views, p, WidgetKind.INFO, json)
        Bind.text(views, R.id.odc_w_headline, json.str("headline"), p) { it.onBackground }
        val rows = json.arr("rows").objects(INFO_ROWS.size)
        INFO_ROWS.forEachIndexed { i, r ->
            val row = rows.getOrNull(i)
            if (row == null) {
                Bind.show(views, r.root, false)
                return@forEachIndexed
            }
            Bind.show(views, r.root, true)
            val trend = row.str("trend").trim().lowercase()
            val trendColor: (Palette) -> Int = when (trend) {
                "up" -> Palette::positive
                "down" -> Palette::negative
                else -> Palette::onBackground
            }
            val icon = row.str("icon").ifBlank {
                when (trend) {
                    "up" -> "arrow_up"
                    "down" -> "arrow_down"
                    else -> ""
                }
            }
            WidgetIcons.bind(views, r.icon, icon, p, if (trend == "up" || trend == "down") trendColor else Palette::muted)
            Bind.text(views, r.label, row.str("label"), p) { it.onBackground }
            Bind.text(views, r.sublabel, row.str("sublabel"), p) { it.onBackground }
            Bind.text(views, r.value, row.str("value"), p, trendColor)
            Bind.text(views, r.subvalue, row.str("subvalue"), p, trendColor)
            val link = row.str("deepLink")
            if (link.isNotBlank()) views.setOnClickPendingIntent(r.root, Taps.open(context, WidgetKind.INFO, link, i + 1))
        }
    }

    private fun stats(context: Context, views: RemoteViews, p: Painter, json: JSONObject) {
        header(context, views, p, WidgetKind.STATS, json)
        val tiles = json.arr("tiles").objects(STATS.size)
        STATS.forEachIndexed { i, s ->
            val tile = tiles.getOrNull(i)
            if (tile == null) {
                Bind.show(views, s.root, false)
                return@forEachIndexed
            }
            Bind.show(views, s.root, true)
            val progress = (tile.num("progress") ?: 0.0).coerceIn(0.0, 100.0).toInt()
            views.setProgressBar(s.ring, 100, progress, false)
            p.progress(s.ring) { it.accent }
            WidgetIcons.bind(views, s.icon, tile.str("icon"), p) { it.onBackground }
            Bind.text(views, s.value, tile.str("value"), p) { it.onBackground }
            Bind.text(views, s.label, tile.str("label"), p) { it.muted }
            val link = tile.str("deepLink")
            if (link.isNotBlank()) views.setOnClickPendingIntent(s.root, Taps.open(context, WidgetKind.STATS, link, i + 1))
        }
    }

    private fun hero(context: Context, views: RemoteViews, p: Painter, json: JSONObject) {
        val bitmap = ImageCache.load(context, json.str("image"))
        val onImage = bitmap != null
        if (bitmap != null) views.setImageViewBitmap(R.id.odc_w_hero_image, bitmap)
        Bind.show(views, R.id.odc_w_hero_image, onImage)
        Bind.show(views, R.id.odc_w_hero_scrim, onImage)
        header(context, views, p, WidgetKind.HERO, json, onImage)
        if (onImage) {
            Bind.fixedText(views, R.id.odc_w_overline, json.str("overline"), Color.WHITE)
            Bind.fixedText(views, R.id.odc_w_title, json.str("headline"), Color.WHITE)
            Bind.fixedText(views, R.id.odc_w_subtitle, json.str("subtitle"), 0xDDFFFFFF.toInt())
        } else {
            Bind.text(views, R.id.odc_w_overline, json.str("overline"), p) { it.accent }
            Bind.text(views, R.id.odc_w_title, json.str("headline"), p) { it.onBackground }
            Bind.text(views, R.id.odc_w_subtitle, json.str("subtitle"), p) { it.muted }
        }
    }

    private fun collection(context: Context, views: RemoteViews, p: Painter, kind: WidgetKind, json: JSONObject, widgetId: Int) {
        header(context, views, p, kind, json)
        val adapter = Intent(context, OdcWidgetCollectionService::class.java)
            .putExtra(Taps.EXTRA_KIND, kind.name)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        // A unique data URI stops Android from reusing one factory for every widget.
        adapter.data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME))
        @Suppress("DEPRECATION")
        views.setRemoteAdapter(R.id.odc_w_list, adapter)
        views.setEmptyView(R.id.odc_w_list, R.id.odc_w_none)
        p.text(R.id.odc_w_none) { it.muted }
        val template = if (kind == WidgetKind.CHECKLIST) Taps.toggleTemplate(context, kind) else Taps.openTemplate(context, kind)
        views.setPendingIntentTemplate(R.id.odc_w_list, template)
    }
}
