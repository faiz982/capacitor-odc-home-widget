package com.odchomewidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

@CapacitorPlugin(name = "OdcHomeWidget")
class OdcHomeWidgetPlugin : Plugin() {

    companion object {
        const val EVENT_WIDGET_TAP = "widgetTap"
        const val EVENT_WIDGET_ACTION = "widgetAction"

        const val ERR_INVALID_KIND = "OS-PLUG-HWGT-0001"
        const val ERR_INVALID_DATA = "OS-PLUG-HWGT-0002"
        const val ERR_STORAGE = "OS-PLUG-HWGT-0003"
        const val ERR_PIN_NOT_SUPPORTED = "OS-PLUG-HWGT-0004"
        const val ERR_PIN_REFUSED = "OS-PLUG-HWGT-0005"
        const val ERR_UNEXPECTED = "OS-PLUG-HWGT-0006"

        /** One worker: saves happen in call order, and image downloads stay off the UI thread. */
        private val worker = Executors.newSingleThreadExecutor()

        @Volatile
        private var instance: WeakReference<OdcHomeWidgetPlugin>? = null

        /** Hands queued `widgetAction` events to the app when it is running. */
        fun deliverPendingEvents() {
            instance?.get()?.emitPendingEvents()
        }
    }

    // ---- Lifecycle and taps -------------------------------------------------

    override fun load() {
        super.load()
        instance = WeakReference(this)
        activity?.intent?.let { emitTap(it) }
        emitPendingEvents()
    }

    override fun handleOnResume() {
        super.handleOnResume()
        emitPendingEvents()
    }

    /** Button presses made on the home screen (also while the app was closed). */
    private fun emitPendingEvents() {
        WidgetStore.drainEvents(context).forEach { e ->
            val event = JSObject()
            event.put("type", e.optString("type"))
            event.put("id", e.optString("id"))
            event.put("key", e.optString("key"))
            event.put("value", e.opt("value"))
            event.put("ok", e.optBoolean("ok", true))
            event.put("error", e.optString("error"))
            notifyListeners(EVENT_WIDGET_ACTION, event, true)
        }
    }

    override fun handleOnNewIntent(intent: Intent) {
        super.handleOnNewIntent(intent)
        emitTap(intent)
    }

    private fun emitTap(intent: Intent) {
        val deepLink = intent.getStringExtra(Taps.EXTRA_DEEP_LINK) ?: return
        val kind = intent.getStringExtra(Taps.EXTRA_KIND) ?: ""
        // Consume the extra so an activity recreation doesn't fire the tap twice.
        intent.removeExtra(Taps.EXTRA_DEEP_LINK)
        val event = JSObject()
        event.put("kind", kind)
        event.put("deepLink", deepLink)
        // retainUntilConsumed = true: a tap that cold-starts the app is queued
        // until the ODC screen registers its listener.
        notifyListeners(EVENT_WIDGET_TAP, event, true)
    }

    // ---- Methods ----------------------------------------------------------

    @PluginMethod
    fun setWidgetData(call: PluginCall) {
        val kind = requireKind(call) ?: return
        val data = call.getString("data") ?: call.getObject("data")?.toString()
        if (data.isNullOrBlank()) {
            call.reject("data is required and must be a JSON string", ERR_INVALID_DATA)
            return
        }
        val json = try {
            WidgetValidator.validate(kind, data)
        } catch (e: IllegalArgumentException) {
            call.reject(e.message ?: "Invalid widget data", ERR_INVALID_DATA)
            return
        }
        worker.execute {
            try {
                // "values" in the JSON are starting values: keys that already have a value
                // (changed by a widget button, a refresh or setWidgetValues) keep it.
                val values = WidgetStore.updateValues(context) { v ->
                    json.obj("values")?.let { start -> start.keys().forEach { k -> if (v.keyFor(k) == null) v.put(k, start.opt(k)) } }
                }
                // Download images first, so the widget never shows a half-loaded state.
                ImageCache.prefetch(context, Values.resolve(json, values))
                if (!WidgetStore.save(context, kind, data)) {
                    call.reject("Could not save widget data", ERR_STORAGE)
                    return@execute
                }
                WidgetRenderer.refresh(context, kind)
                WidgetSync.schedule(context, json)
                call.resolve()
            } catch (e: Exception) {
                call.reject(e.message ?: "Unexpected error", ERR_UNEXPECTED, e)
            }
        }
    }

    /** Merges values into the live values (for {{placeholders}}) and redraws. */
    @PluginMethod
    fun setWidgetValues(call: PluginCall) {
        val data = call.getString("data") ?: call.getObject("data")?.toString()
        val values = try {
            JSONObject(data ?: "")
        } catch (e: JSONException) {
            call.reject("data must be a JSON object string like {\"temp\":21}", ERR_INVALID_DATA)
            return
        }
        worker.execute {
            try {
                WidgetStore.updateValues(context) { Values.merge(it, values) }
                WidgetSync.redraw(context)
                call.resolve()
            } catch (e: Exception) {
                call.reject(e.message ?: "Unexpected error", ERR_UNEXPECTED, e)
            }
        }
    }

    /** The live values, including ones changed by widget buttons and background refreshes. */
    @PluginMethod
    fun getWidgetValues(call: PluginCall) {
        guarded(call) {
            call.resolve(JSObject().put("data", WidgetStore.values(context).toString()))
        }
    }

    /** Runs the widget's "refresh" request now and redraws. */
    @PluginMethod
    fun refreshNow(call: PluginCall) {
        worker.execute {
            try {
                WidgetSync.run(context, WidgetSync.JOB_REFRESH, null)
                call.resolve(JSObject().put("data", WidgetStore.values(context).toString()))
            } catch (e: Exception) {
                call.reject(e.message ?: "Unexpected error", ERR_UNEXPECTED, e)
            }
        }
    }

    @PluginMethod
    fun setTheme(call: PluginCall) {
        val data = call.getString("data") ?: call.getObject("data")?.toString()
        val theme = if (data.isNullOrBlank()) {
            null
        } else {
            try {
                JSONObject(data).also { Themes.validate(it) }
            } catch (e: JSONException) {
                call.reject("data must be a JSON object string: ${e.message}", ERR_INVALID_DATA)
                return
            } catch (e: IllegalArgumentException) {
                call.reject(e.message ?: "Invalid theme", ERR_INVALID_DATA)
                return
            }
        }
        worker.execute {
            try {
                WidgetStore.saveTheme(context, theme?.toString())
                WidgetKind.values().forEach { WidgetRenderer.refresh(context, it) }
                call.resolve()
            } catch (e: Exception) {
                call.reject(e.message ?: "Unexpected error", ERR_UNEXPECTED, e)
            }
        }
    }

    @PluginMethod
    fun refresh(call: PluginCall) {
        val kind = requireKind(call) ?: return
        guarded(call) {
            WidgetRenderer.refresh(context, kind)
            call.resolve()
        }
    }

    @PluginMethod
    fun clear(call: PluginCall) {
        val kind = requireKind(call) ?: return
        guarded(call) {
            WidgetStore.clear(context, kind)
            WidgetSync.cancel(context)
            WidgetRenderer.refresh(context, kind)
            call.resolve()
        }
    }

    @PluginMethod
    fun isPinSupported(call: PluginCall) {
        call.resolve(JSObject().put("supported", pinSupported()))
    }

    @PluginMethod
    fun requestPin(call: PluginCall) {
        val kind = requireKind(call) ?: return
        if (!pinSupported() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            call.reject("This launcher does not support pinning widgets from the app", ERR_PIN_NOT_SUPPORTED)
            return
        }
        guarded(call) {
            val ok = AppWidgetManager.getInstance(context)
                .requestPinAppWidget(kind.componentName(context), null, null)
            if (ok) call.resolve() else call.reject("The launcher refused the pin request", ERR_PIN_REFUSED)
        }
    }

    @PluginMethod
    fun getInstalledCount(call: PluginCall) {
        val kind = requireKind(call) ?: return
        guarded(call) {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(kind.componentName(context))
            call.resolve(JSObject().put("count", ids.size))
        }
    }

    // ---- Helpers ----------------------------------------------------------

    private fun pinSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    private fun requireKind(call: PluginCall): WidgetKind? {
        val raw = call.getString("kind")
        val kind = WidgetKind.parse(raw)
        if (kind == null) {
            call.reject("kind must be one of ${WidgetKind.names} (got \"${raw ?: ""}\")", ERR_INVALID_KIND)
        }
        return kind
    }

    private inline fun guarded(call: PluginCall, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            call.reject(e.message ?: "Unexpected error", ERR_UNEXPECTED, e)
        }
    }
}
