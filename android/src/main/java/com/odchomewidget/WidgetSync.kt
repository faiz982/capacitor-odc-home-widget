package com.odchomewidget

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Background work that keeps the widget live while the app is closed:
 *
 * ```json
 * "refresh": { "url": "https://...", "minutes": 30, "headers": { "Authorization": "Bearer {{token}}" }, "key": "weather" }
 * ```
 * The response (JSON) is merged into the values (under "key" when given), then the
 * widget redraws. Android runs periodic work at most every 15 minutes, and may
 * delay it to save battery. `_updatedAt` (epoch ms) and `_updatedTime` ("HH:mm")
 * are set after each successful refresh; `_error` holds the last failure.
 */
object WidgetSync {
    const val JOB_REFRESH = "refresh"
    const val JOB_HTTP = "http"
    const val JOB_REDRAW = "redraw"
    private const val PERIODIC = "odc_widget_refresh"
    private const val MIN_MINUTES = 15L
    private const val MAX_BYTES = 1024 * 1024

    fun validate(refresh: JSONObject) {
        val url = refresh.str("url")
        if (url.isBlank()) throw IllegalArgumentException("refresh needs a \"url\"")
        if (!Values.hasPlaceholder(url) && !url.startsWith("https://", true) && !url.startsWith("http://", true)) {
            throw IllegalArgumentException("refresh.url must start with https://")
        }
    }

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Starts, updates or stops the periodic refresh to match the saved widget JSON. */
    fun schedule(context: Context, widget: JSONObject?) {
        val wm = WorkManager.getInstance(context)
        val refresh = widget?.obj("refresh")
        if (refresh == null || refresh.str("url").isBlank()) {
            wm.cancelUniqueWork(PERIODIC)
            return
        }
        val minutes = (refresh.num("minutes")?.toLong() ?: 30L).coerceAtLeast(MIN_MINUTES)
        val request = PeriodicWorkRequest.Builder(WidgetWorker::class.java, minutes, TimeUnit.MINUTES)
            .setConstraints(network)
            .setInputData(Data.Builder().putString("job", JOB_REFRESH).build())
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        enqueue(context, JOB_REFRESH, null)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }

    fun enqueue(context: Context, job: String, action: JSONObject?) {
        val builder = OneTimeWorkRequest.Builder(WidgetWorker::class.java)
            .setInputData(Data.Builder().putString("job", job).putString("action", action?.toString()).build())
        if (job != JOB_REDRAW) builder.setConstraints(network)
        val name = if (job == JOB_HTTP) "odc_widget_http_${System.nanoTime()}" else "odc_widget_$job"
        WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, builder.build())
    }

    /** Runs a job now (on the calling background thread). */
    fun run(context: Context, job: String, action: JSONObject?) {
        when (job) {
            JOB_REFRESH -> refreshNow(context)
            JOB_HTTP -> action?.let { httpAction(context, it) }
        }
        redraw(context)
    }

    /** Fetches images the current values point at, then redraws. */
    fun redraw(context: Context) {
        WidgetRenderer.readData(context, WidgetKind.WIDGET)?.let { raw ->
            runCatching { ImageCache.prefetch(context, Values.resolve(raw, WidgetStore.values(context))) }
        }
        WidgetRenderer.refresh(context, WidgetKind.WIDGET)
    }

    private fun refreshNow(context: Context) {
        val raw = WidgetRenderer.readData(context, WidgetKind.WIDGET) ?: return
        val refresh = raw.obj("refresh")?.let { Values.resolve(it, WidgetStore.values(context)) } ?: return
        try {
            val body = request(refresh.str("url"), refresh.str("method").ifBlank { "GET" }, refresh.obj("headers"), null)
            store(context, body, refresh.str("key"))
            WidgetStore.updateValues(context) {
                val now = System.currentTimeMillis()
                it.put("_updatedAt", now)
                it.put("_updatedTime", SimpleDateFormat("HH:mm", Locale.getDefault()).format(now))
                it.remove("_error")
            }
        } catch (e: Exception) {
            WidgetStore.updateValues(context) { it.put("_error", e.message ?: "Refresh failed") }
        }
    }

    private fun httpAction(context: Context, action: JSONObject) {
        try {
            val body = when (val b = action.valueOf("body")) {
                null -> null
                is JSONObject, is JSONArray -> b.toString()
                else -> b.toString()
            }
            val response = request(action.str("url"), action.str("method").ifBlank { "POST" }, action.obj("headers"), body)
            val merge = action.valueOf("merge")?.let { Values.truthy(it) } ?: true
            if (merge) store(context, response, action.str("key"))
            WidgetActions.report(context, action, parse(response))
        } catch (e: Exception) {
            WidgetActions.report(context, action, null, ok = false, error = e.message ?: "Request failed")
        }
    }

    private fun parse(body: String): Any? =
        if (body.isBlank()) null else runCatching { JSONTokener(body).nextValue() }.getOrNull() ?: body

    /** Merges a JSON response into the values; non-objects (or any response with [key]) go under [key] / "data". */
    private fun store(context: Context, body: String, key: String) {
        val value = parse(body) ?: return
        WidgetStore.updateValues(context) { values ->
            when {
                key.isNotBlank() -> values.put(key, value)
                value is JSONObject -> Values.merge(values, value)
                else -> values.put("data", value)
            }
        }
    }

    private fun request(url: String, method: String, headers: JSONObject?, body: String?): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.requestMethod = method.uppercase()
        conn.setRequestProperty("Accept", "application/json")
        headers?.keys()?.forEach { conn.setRequestProperty(it, headers.optString(it)) }
        try {
            if (body != null && conn.requestMethod != "GET") {
                conn.doOutput = true
                if (headers?.keyFor("Content-Type") == null) conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) throw IOException("Response larger than 1 MB")
                    out.write(buffer, 0, n)
                }
                out.toString("UTF-8")
            }
        } finally {
            conn.disconnect()
        }
    }
}

class WidgetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val job = inputData.getString("job") ?: WidgetSync.JOB_REDRAW
        val action = inputData.getString("action")?.let { runCatching { JSONObject(it) }.getOrNull() }
        WidgetSync.run(applicationContext, job, action)
        return Result.success()
    }
}
