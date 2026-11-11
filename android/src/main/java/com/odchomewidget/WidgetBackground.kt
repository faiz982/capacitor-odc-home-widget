package com.odchomewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-bleed background for the WIDGET kind, drawn into one bitmap at the
 * widget's own size:
 *
 * ```json
 * "background": {
 *   "gradient": ["#8BE04E", "#1BB8A9"], "angle": 135,
 *   "image": "https://...",              // optional, drawn under the gradient
 *   "decoration": "rings",               // rings | circles | waves | none
 *   "decorationColor": "#40FFFFFF"
 * }
 * ```
 * `angle` follows CSS: 0 = bottom to top, 90 = left to right, 180 = top to bottom.
 */
object WidgetBackground {
    val DECORATIONS = listOf("none", "rings", "circles", "waves")
    private const val MAX_SIDE_PX = 720f
    private const val CORNER_DP = 24f

    fun validate(bg: JSONObject) {
        val colors = bg.arr("gradient")
        if (bg.valueOf("gradient") != null && colors == null) {
            throw IllegalArgumentException("background.gradient must be an array of colors like [\"#8BE04E\", \"#1BB8A9\"]")
        }
        colors?.let { arr ->
            if (arr.length() > 6) throw IllegalArgumentException("background.gradient takes at most 6 colors")
            for (i in 0 until arr.length()) {
                if (Themes.parse(arr.optString(i)) == null) {
                    throw IllegalArgumentException("background.gradient[$i] \"${arr.optString(i)}\" is not a hex color")
                }
            }
        }
        val deco = bg.str("decoration").trim().lowercase()
        if (deco.isNotEmpty() && deco !in DECORATIONS) {
            throw IllegalArgumentException("background.decoration must be one of ${DECORATIONS.joinToString()}")
        }
        bg.str("decorationColor").takeIf { it.isNotBlank() }?.let {
            if (Themes.parse(it) == null) throw IllegalArgumentException("background.decorationColor \"$it\" is not a hex color")
        }
    }

    /** Draws the background for [widgetId], or returns null when [bg] draws nothing. */
    fun draw(context: Context, widgetId: Int, bg: JSONObject): Bitmap? {
        val colors = colors(bg.arr("gradient"))
        val image = ImageCache.load(context, bg.str("image"))
        val deco = bg.str("decoration").trim().lowercase()
        if (colors.isEmpty() && image == null && (deco.isEmpty() || deco == "none")) return null

        val (wDp, hDp) = sizeDp(context, widgetId)
        val scale = min(MAX_SIDE_PX / max(wDp, hDp), context.resources.displayMetrics.density)
        val w = (wDp * scale).toInt().coerceAtLeast(1)
        val h = (hDp * scale).toInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Round the corners ourselves so older launchers that don't clip still look right.
        val r = CORNER_DP * scale
        canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, Path.Direction.CW) })

        image?.let { canvas.drawBitmap(it, centerCrop(it, w, h), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG)) }
        when (colors.size) {
            0 -> {}
            1 -> canvas.drawColor(colors[0])
            else -> canvas.drawPaint(Paint().apply { shader = gradient(colors, bg.num("angle") ?: 135.0, w, h) })
        }

        val decoColor = Themes.parse(bg.str("decorationColor")) ?: Color.argb(64, 255, 255, 255)
        decorate(canvas, deco, decoColor, w.toFloat(), h.toFloat(), scale)
        return out
    }

    private fun colors(arr: JSONArray?): List<Int> {
        if (arr == null) return emptyList()
        return (0 until min(arr.length(), 6)).mapNotNull { Themes.parse(arr.optString(it)) }
    }

    /** Widget size in dp, portrait orientation (min width / max height), with a 4x2 fallback. */
    private fun sizeDp(context: Context, widgetId: Int): Pair<Float, Float> {
        val options = runCatching { AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId) }.getOrNull()
        val w = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
        val h = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0
        return Pair(if (w > 0) w.toFloat() else 320f, if (h > 0) h.toFloat() else 160f)
    }

    private fun gradient(colors: List<Int>, angleDeg: Double, w: Int, h: Int): LinearGradient {
        val a = Math.toRadians(angleDeg)
        val dx = sin(a).toFloat()
        val dy = -cos(a).toFloat()
        val half = (abs(w * dx) + abs(h * dy)) / 2f
        val cx = w / 2f
        val cy = h / 2f
        return LinearGradient(
            cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half,
            colors.toIntArray(), null, Shader.TileMode.CLAMP,
        )
    }

    private fun centerCrop(src: Bitmap, w: Int, h: Int): Rect {
        val target = w.toFloat() / h
        return if (src.width.toFloat() / src.height > target) {
            val cw = (src.height * target).toInt()
            val x = (src.width - cw) / 2
            Rect(x, 0, x + cw, src.height)
        } else {
            val ch = (src.width / target).toInt()
            val y = (src.height - ch) / 2
            Rect(0, y, src.width, y + ch)
        }
    }

    private fun decorate(canvas: Canvas, deco: String, color: Int, w: Float, h: Float, scale: Float) {
        when (deco) {
            // Concentric outlines growing from the top-right corner.
            "rings" -> {
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 10f * scale
                    this.color = color
                }
                val cx = w * 0.95f
                val cy = h * 0.05f
                val base = min(w, h) * 0.22f
                for (i in 0 until 5) canvas.drawCircle(cx, cy, base + i * 22f * scale, stroke)
                stroke.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, base * 0.55f, stroke)
            }
            // Soft overlapping discs.
            "circles" -> {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                val m = min(w, h)
                canvas.drawCircle(w * 0.92f, h * 0.08f, m * 0.55f, fill)
                canvas.drawCircle(w * 0.70f, h * 1.05f, m * 0.40f, fill)
            }
            // Two flowing bands across the lower half.
            "waves" -> {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                for ((top, amp) in listOf(0.60f to 0.10f, 0.75f to 0.08f)) {
                    val path = Path().apply {
                        moveTo(0f, h * top)
                        cubicTo(w * 0.3f, h * (top - amp * 2), w * 0.6f, h * (top + amp * 2), w, h * (top - amp))
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    canvas.drawPath(path, fill)
                }
            }
        }
    }
}
