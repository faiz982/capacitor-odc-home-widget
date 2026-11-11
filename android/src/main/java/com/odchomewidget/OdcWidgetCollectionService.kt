package com.odchomewidget

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.json.JSONObject

/** Supplies the rows of the scrolling templates: ARTICLES, GRID, MESSAGES and CHECKLIST. */
class OdcWidgetCollectionService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val kind = WidgetKind.parse(intent.getStringExtra(Taps.EXTRA_KIND)) ?: WidgetKind.ARTICLES
        return CollectionFactory(applicationContext, kind)
    }
}

class CollectionFactory(private val context: Context, private val kind: WidgetKind) :
    RemoteViewsService.RemoteViewsFactory {

    private var items: List<JSONObject> = emptyList()
    private var theme: WidgetTheme = Themes.DEFAULT

    override fun onCreate() = Unit

    /** Runs on a binder thread, so it may also fetch images the app didn't prefetch. */
    override fun onDataSetChanged() {
        val json = WidgetRenderer.readData(context, kind)
        theme = Themes.resolve(context, json)
        val field = WidgetValidator.requiredField(kind) ?: "items"
        items = json?.arr(field).objects(WidgetRenderer.MAX_COLLECTION_ITEMS)
        items.forEach { item ->
            val image = item.str("image")
            if (ImageCache.isImageSource(image)) runCatching { ImageCache.ensure(context, image) }
        }
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items.getOrNull(position) ?: JSONObject()
        return when (kind) {
            WidgetKind.GRID -> gridItem(item)
            WidgetKind.MESSAGES -> message(item)
            WidgetKind.CHECKLIST -> checkItem(item)
            else -> article(item)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    // ---- Rows ---------------------------------------------------------------

    private fun article(item: JSONObject): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.odc_w_item_article)
        val p = Painter(context, views, theme)
        val bitmap = ImageCache.load(context, item.str("image"))?.let { ImageCache.cropRound(it, 1f, 0.18f) }
        if (bitmap != null) views.setImageViewBitmap(R.id.odc_w_item_image, bitmap)
        Bind.show(views, R.id.odc_w_item_image, bitmap != null)
        Bind.text(views, R.id.odc_w_item_overline, item.str("overline"), p) { it.accent }
        Bind.text(views, R.id.odc_w_item_meta, item.str("meta"), p) { it.muted }
        Bind.text(views, R.id.odc_w_item_title, item.str("title"), p) { it.onBackground }
        Bind.text(views, R.id.odc_w_item_subtitle, item.str("subtitle"), p) { it.muted }
        views.setOnClickFillInIntent(R.id.odc_w_item_root, Taps.openFill(kind, item.str("deepLink")))
        return views
    }

    private fun gridItem(item: JSONObject): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.odc_w_item_grid)
        val p = Painter(context, views, theme)
        val bitmap = ImageCache.load(context, item.str("image"))?.let { ImageCache.cropRound(it, 16f / 10f, 0.12f) }
        if (bitmap != null) views.setImageViewBitmap(R.id.odc_w_item_image, bitmap)
        Bind.show(views, R.id.odc_w_item_image, bitmap != null)
        Bind.text(views, R.id.odc_w_item_title, item.str("title"), p) { it.onBackground }
        Bind.text(views, R.id.odc_w_item_subtitle, item.str("subtitle"), p) { it.muted }
        views.setOnClickFillInIntent(R.id.odc_w_item_root, Taps.openFill(kind, item.str("deepLink")))
        return views
    }

    private fun message(item: JSONObject): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.odc_w_item_message)
        val p = Painter(context, views, theme)
        val highlight = item.bool("highlight")
        val fg: (Palette) -> Int = if (highlight) Palette::onTileActive else Palette::onTile
        p.tint(R.id.odc_w_item_bg, if (highlight) Palette::tileActive else Palette::tile)
        Bind.text(views, R.id.odc_w_item_title, item.str("sender"), p, fg)
        Bind.text(views, R.id.odc_w_item_meta, item.str("time"), p, fg)
        Bind.text(views, R.id.odc_w_item_subtitle, item.str("text"), p, fg)
        views.setOnClickFillInIntent(R.id.odc_w_item_root, Taps.openFill(kind, item.str("deepLink")))
        return views
    }

    private fun checkItem(item: JSONObject): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.odc_w_item_check)
        val p = Painter(context, views, theme)
        val checked = item.bool("checked")
        WidgetIcons.bind(views, R.id.odc_w_item_check, if (checked) "check_circle" else "circle", p,
            if (checked) Palette::accent else Palette::muted)
        val text = item.str("text")
        val label: CharSequence = if (checked && text.isNotEmpty()) {
            SpannableString(text).apply { setSpan(StrikethroughSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        } else {
            text
        }
        Bind.text(views, R.id.odc_w_item_title, label, p, if (checked) Palette::muted else Palette::onBackground)
        WidgetIcons.bind(views, R.id.odc_w_item_icon, item.str("icon"), p) { it.muted }
        views.setOnClickFillInIntent(R.id.odc_w_item_root, Taps.toggleFill(item.str("id")))
        return views
    }
}
