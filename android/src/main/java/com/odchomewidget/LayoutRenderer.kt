package com.odchomewidget

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject

/**
 * Draws the WIDGET kind: the app sends a tree of elements as JSON and this turns
 * it into native views by nesting small RemoteViews (RemoteViews.addView).
 *
 * Element types: column, row, box, text, icon, image, progress, spacer, button.
 * Common fields: type, children, padding (dp, or [start, top, end, bottom]),
 * weight (> 0 fills the remaining space), visible, deepLink.
 */
object LayoutRenderer {
    const val MAX_ELEMENTS = 80
    const val MAX_DEPTH = 8
    private const val MAX_CLICK_SLOTS = 90
    val TYPES = listOf("column", "row", "box", "text", "icon", "image", "progress", "spacer", "button")

    private enum class Parent { VERTICAL, HORIZONTAL }

    private class Ctx(val context: Context, val theme: WidgetTheme) {
        var slot = 1
        val density = context.resources.displayMetrics.density
        fun px(dp: Double) = (dp * density).toInt()
    }

    private val ROLES: Map<String, (Palette) -> Int> = mapOf(
        "background" to Palette::background,
        "onbackground" to Palette::onBackground,
        "muted" to Palette::muted,
        "accent" to Palette::accent,
        "onaccent" to Palette::onAccent,
        "tile" to Palette::tile,
        "ontile" to Palette::onTile,
        "tileactive" to Palette::tileActive,
        "ontileactive" to Palette::onTileActive,
        "positive" to Palette::positive,
        "negative" to Palette::negative,
    )

    // ---- Validation -------------------------------------------------------

    /** @throws IllegalArgumentException naming the first problem in the tree. */
    fun validate(layout: JSONObject) {
        validateNode(layout, "layout", 0, intArrayOf(0))
    }

    private fun validateNode(node: JSONObject, path: String, depth: Int, count: IntArray) {
        val type = node.str("type").trim().lowercase()
        if (type !in TYPES) {
            throw IllegalArgumentException("$path.type \"${node.str("type")}\" must be one of ${TYPES.joinToString()}")
        }
        if (depth > MAX_DEPTH) throw IllegalArgumentException("$path is nested deeper than $MAX_DEPTH levels")
        count[0] += 1
        if (count[0] > MAX_ELEMENTS) throw IllegalArgumentException("layout has more than $MAX_ELEMENTS elements")
        listOf("color", "background").forEach { key ->
            val value = node.str(key).trim()
            if (value.isNotEmpty() && ROLES[value.lowercase()] == null && Themes.parse(value) == null) {
                throw IllegalArgumentException("$path.$key \"$value\" must be a theme color name or a hex color")
            }
        }
        val children = node.valueOf("children") ?: return
        if (children !is JSONArray) throw IllegalArgumentException("$path.children must be an array")
        if (type !in setOf("column", "row", "box")) {
            throw IllegalArgumentException("$path is a $type; only column, row and box can have children")
        }
        children.objects(MAX_ELEMENTS).forEachIndexed { i, child -> validateNode(child, "$path.children[$i]", depth + 1, count) }
    }

    // ---- Rendering ----------------------------------------------------------

    /** Adds the rendered tree into [containerId] of [root]. */
    fun render(context: Context, root: RemoteViews, containerId: Int, json: JSONObject, theme: WidgetTheme) {
        val layout = json.obj("layout") ?: return
        root.addView(containerId, element(Ctx(context, theme), layout, Parent.VERTICAL, fill = true))
    }

    private fun element(c: Ctx, node: JSONObject, parent: Parent, fill: Boolean = false): RemoteViews {
        val type = node.str("type").trim().lowercase()
        val flexibleSpacer = type == "spacer" && node.num("size") == null
        val weighted = fill || flexibleSpacer || (node.num("weight") ?: 0.0) > 0.0
        val variant = when (parent) {
            Parent.VERTICAL -> if (weighted) "vw" else "v"
            Parent.HORIZONTAL -> if (weighted) "hw" else "h"
        }
        val views = RemoteViews(c.context.packageName, ElementLayouts.get(type, variant))
        val p = Painter(c.context, views, c.theme)
        val id = R.id.odc_w_el

        when (type) {
            "column", "row" -> {
                node.str("background").takeIf { it.isNotBlank() }?.let { p.background(id, color(it, Palette::background)) }
                val vertical = type == "column"
                if (hasAlignment(node)) {
                    views.setInt(id, "setGravity", gravity(node, Gravity.START, if (vertical) Gravity.TOP else Gravity.CENTER_VERTICAL))
                }
                padding(c, views, id, node, 0.0)
                addChildren(c, views, id, node, if (vertical) Parent.VERTICAL else Parent.HORIZONTAL)
            }
            "box" -> {
                p.tint(R.id.odc_w_el_bg, color(node.str("background"), Palette::tile))
                if (hasAlignment(node)) views.setInt(R.id.odc_w_el_inner, "setGravity", gravity(node, Gravity.START, Gravity.TOP))
                padding(c, views, R.id.odc_w_el_inner, node, 12.0)
                addChildren(c, views, R.id.odc_w_el_inner, node, Parent.VERTICAL)
            }
            "text" -> {
                text(views, id, node)
                p.text(id, color(node.str("color"), Palette::onBackground))
                node.num("size")?.let { views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, it.toFloat()) }
                node.num("maxLines")?.let { views.setInt(id, "setMaxLines", it.toInt().coerceAtLeast(1)) }
                if (hasAlignment(node)) views.setInt(id, "setGravity", gravity(node, Gravity.START, Gravity.CENTER_VERTICAL))
                padding(c, views, id, node, 0.0)
            }
            "icon" -> {
                WidgetIcons.bind(views, id, node.str("name"), p, color(node.str("color"), Palette::onBackground))
                node.num("size")?.let { size(c, views, id, it, it) }
            }
            "image" -> {
                val w = node.num("width")
                val h = node.num("height")
                val aspect = if (w != null && h != null && h > 0) (w / h).toFloat() else if (parent == Parent.HORIZONTAL) 1f else 16f / 9f
                val radius = (node.num("radius") ?: 0.12).toFloat().coerceIn(0f, 0.5f)
                val bitmap = ImageCache.load(c.context, node.str("src"))?.let { ImageCache.cropRound(it, aspect, radius) }
                if (bitmap == null) {
                    views.setViewVisibility(id, View.GONE)
                } else {
                    views.setImageViewBitmap(id, bitmap)
                    size(c, views, id, w, h)
                }
            }
            "progress" -> {
                val value = (node.num("value") ?: 0.0).coerceIn(0.0, 100.0).toInt()
                views.setProgressBar(R.id.odc_w_el_ring, 100, value, false)
                p.progress(R.id.odc_w_el_ring, color(node.str("color"), Palette::accent))
                WidgetIcons.bind(views, R.id.odc_w_el_icon, node.str("icon"), p, color(node.str("color"), Palette::onBackground))
                Bind.text(views, R.id.odc_w_el_text, node.str("text"), p, Palette::onBackground)
                node.num("size")?.let { size(c, views, id, it, it) }
            }
            "spacer" -> {
                node.num("size")?.let { s ->
                    if (parent == Parent.VERTICAL) size(c, views, id, null, s) else size(c, views, id, s, null)
                }
            }
            "button" -> {
                p.tint(R.id.odc_w_el_bg, color(node.str("background"), Palette::accent))
                val fg = color(node.str("color"), Palette::onAccent)
                WidgetIcons.bind(views, R.id.odc_w_el_icon, node.str("icon"), p, fg)
                text(views, R.id.odc_w_el_text, node)
                p.text(R.id.odc_w_el_text, fg)
                if (node.str("text").isBlank()) views.setViewVisibility(R.id.odc_w_el_text, View.GONE)
                node.valueOf("padding")?.let { padding(c, views, R.id.odc_w_el_inner, node, 0.0) }
            }
        }

        if (node.valueOf("visible") != null && !node.bool("visible")) views.setViewVisibility(id, View.GONE)
        val link = node.str("deepLink")
        if (link.isNotBlank() && c.slot < MAX_CLICK_SLOTS) {
            views.setOnClickPendingIntent(id, Taps.open(c.context, WidgetKind.WIDGET, link, c.slot++))
        }
        return views
    }

    private fun addChildren(c: Ctx, views: RemoteViews, containerId: Int, node: JSONObject, parent: Parent) {
        val gap = node.num("gap")
        node.arr("children").objects(MAX_ELEMENTS).forEachIndexed { i, child ->
            if (i > 0 && gap != null && gap > 0) {
                val spacer = JSONObject().put("type", "spacer").put("size", gap)
                views.addView(containerId, element(c, spacer, parent))
            }
            views.addView(containerId, element(c, child, parent))
        }
    }

    // ---- Helpers ------------------------------------------------------------

    private fun color(value: String, default: (Palette) -> Int): (Palette) -> Int {
        val v = value.trim()
        if (v.isEmpty()) return default
        ROLES[v.lowercase()]?.let { return it }
        val hex = Themes.parse(v) ?: return default
        return { _ -> hex }
    }

    /** Text with optional bold / strike, set as spans so it works on every Android version. */
    private fun text(views: RemoteViews, viewId: Int, node: JSONObject) {
        val raw = node.str("text")
        val bold = node.bool("bold")
        val strike = node.bool("strike")
        val value: CharSequence = if ((bold || strike) && raw.isNotEmpty()) {
            SpannableString(raw).apply {
                if (bold) setSpan(StyleSpan(Typeface.BOLD), 0, raw.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (strike) setSpan(StrikethroughSpan(), 0, raw.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            raw
        }
        views.setTextViewText(viewId, value)
    }

    /** Gravity is only set when the JSON asks for it; the layout XML defaults apply otherwise. */
    private fun hasAlignment(node: JSONObject) = node.str("align").isNotBlank() || node.str("valign").isNotBlank()

    private fun gravity(node: JSONObject, defaultH: Int, defaultV: Int): Int {
        val h = when (node.str("align").trim().lowercase()) {
            "center" -> Gravity.CENTER_HORIZONTAL
            "end", "right" -> Gravity.END
            "start", "left" -> Gravity.START
            else -> defaultH
        }
        val v = when (node.str("valign").trim().lowercase()) {
            "center", "middle" -> Gravity.CENTER_VERTICAL
            "bottom", "end" -> Gravity.BOTTOM
            "top", "start" -> Gravity.TOP
            else -> defaultV
        }
        return h or v
    }

    /** padding: a number (all sides) or [start, top, end, bottom], in dp. */
    private fun padding(c: Ctx, views: RemoteViews, viewId: Int, node: JSONObject, defaultDp: Double) {
        val value = node.valueOf("padding")
        val sides: List<Double> = when (value) {
            is Number -> List(4) { value.toDouble() }
            is String -> value.trim().toDoubleOrNull()?.let { d -> List(4) { d } } ?: List(4) { defaultDp }
            is JSONArray -> List(4) { i -> value.optDouble(i, defaultDp) }
            else -> if (defaultDp == 0.0) return else List(4) { defaultDp }
        }
        views.setViewPadding(viewId, c.px(sides[0]), c.px(sides[1]), c.px(sides[2]), c.px(sides[3]))
    }

    /** Exact sizes need Android 12+; older versions keep the variant's default size. */
    private fun size(c: Ctx, views: RemoteViews, viewId: Int, widthDp: Double?, heightDp: Double?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        widthDp?.let { views.setViewLayoutWidth(viewId, it.toFloat(), TypedValue.COMPLEX_UNIT_DIP) }
        heightDp?.let { views.setViewLayoutHeight(viewId, it.toFloat(), TypedValue.COMPLEX_UNIT_DIP) }
    }
}
