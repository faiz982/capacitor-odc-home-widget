package com.odchomewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Widget images. Sources can be https URLs or data URIs (data:image/png;base64,...,
 * handy for ODC Binary Data). The plugin downloads them when the app sends data,
 * scales them down and keeps them on disk, so widgets never touch the network.
 */
object ImageCache {
    private const val DIR = "odc_widget_images"
    private const val MAX_PX = 512
    private const val MAX_BYTES = 8 * 1024 * 1024
    private const val MAX_FILES = 200
    private val IMAGE_KEYS = setOf("image", "thumbnail", "avatar", "src")

    fun isImageSource(src: String): Boolean =
        src.startsWith("https://", ignoreCase = true) ||
            src.startsWith("http://", ignoreCase = true) ||
            src.startsWith("data:image", ignoreCase = true)

    /** Fetches every image the widget JSON refers to. Blocking: call off the main thread. */
    fun prefetch(context: Context, json: JSONObject) {
        val sources = mutableSetOf<String>()
        collect(json, sources)
        sources.forEach { runCatching { ensure(context, it) } }
        prune(context)
    }

    /** Makes sure [src] is cached. Blocking for URLs. */
    fun ensure(context: Context, src: String): Boolean {
        val file = fileFor(context, src)
        if (file.exists()) return true
        val bytes = if (src.startsWith("data:", ignoreCase = true)) {
            Base64.decode(src.substringAfter(','), Base64.DEFAULT)
        } else {
            download(src)
        }
        val bitmap = decodeScaled(bytes) ?: return false
        val tmp = File(file.path + ".tmp")
        FileOutputStream(tmp).use { out ->
            if (bitmap.hasAlpha()) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        }
        return tmp.renameTo(file)
    }

    /**
     * Returns the cached bitmap, or null. Data URIs are decoded on the spot; URLs
     * that were not prefetched are skipped here because this may run on the main thread.
     */
    fun load(context: Context, src: String?): Bitmap? {
        if (src.isNullOrBlank() || !isImageSource(src)) return null
        val file = fileFor(context, src)
        if (!file.exists() && src.startsWith("data:", ignoreCase = true)) {
            runCatching { ensure(context, src) }
        }
        return if (file.exists()) BitmapFactory.decodeFile(file.path) else null
    }

    /** Center-crops to [aspect] (width / height) and rounds the corners. */
    fun cropRound(src: Bitmap, aspect: Float, radiusFraction: Float): Bitmap {
        var w = src.width
        var h = src.height
        if (w.toFloat() / h > aspect) w = (h * aspect).toInt().coerceAtLeast(1) else h = (w / aspect).toInt().coerceAtLeast(1)
        val dx = (src.width - w) / 2f
        val dy = (src.height - h) / 2f
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(Matrix().apply { setTranslate(-dx, -dy) })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        val r = minOf(w, h) * radiusFraction
        Canvas(out).drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, paint)
        return out
    }

    private fun collect(node: Any?, out: MutableSet<String>) {
        when (node) {
            is JSONObject -> node.keys().forEach { key ->
                val value = node.opt(key)
                if (value is String && key.lowercase() in IMAGE_KEYS && isImageSource(value)) {
                    out += value
                } else {
                    collect(value, out)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) collect(node.opt(i), out)
        }
    }

    private fun download(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.instanceFollowRedirects = true
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $url")
            return conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) throw IOException("Image larger than 8 MB: $url")
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_PX && bounds.outHeight / (sample * 2) >= MAX_PX) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        val scale = MAX_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun dir(context: Context) = File(context.applicationContext.filesDir, DIR).apply { mkdirs() }

    private fun fileFor(context: Context, src: String) = File(dir(context), sha1(src) + ".img")

    private fun prune(context: Context) {
        val files = dir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_FILES).forEach { it.delete() }
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
