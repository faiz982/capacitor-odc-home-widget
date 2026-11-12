package com.odchomewidget

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Live values behind `{{placeholders}}`.
 *
 * Values come from (later wins per key): the widget JSON's "values", data fetched
 * by "refresh" or an "http" action, and buttons that change state on the home
 * screen ("set", "toggle", "add", "cycle"). Any string in the widget JSON may use
 * `{{key}}`, `{{a.b.0.c}}` (dot path into objects and arrays) and a filter:
 * `{{temp|round}}`. A string that is exactly one placeholder keeps the value's
 * type, so `"value": "{{progress}}"` stays a number.
 */
object Values {
    private val PLACEHOLDER = Regex("""\{\{\s*([^}|]+?)\s*(?:\|\s*([A-Za-z0-9]+)\s*)?\}\}""")
    val FILTERS = listOf("round", "floor", "ceil", "fixed1", "fixed2", "upper", "lower", "time", "date")

    fun hasPlaceholder(s: String) = s.contains("{{")

    /** A deep copy of [json] with every placeholder filled from [values]. */
    fun resolve(json: JSONObject, values: JSONObject): JSONObject {
        val out = JSONObject()
        json.keys().forEach { key -> out.put(key, resolveAny(json.opt(key), values)) }
        return out
    }

    private fun resolveAny(v: Any?, values: JSONObject): Any? = when (v) {
        is JSONObject -> resolve(v, values)
        is JSONArray -> JSONArray().also { arr -> for (i in 0 until v.length()) arr.put(resolveAny(v.opt(i), values)) }
        is String -> resolveString(v, values)
        else -> v
    }

    fun resolveString(s: String, values: JSONObject): Any {
        if (!hasPlaceholder(s)) return s
        val whole = PLACEHOLDER.matchEntire(s.trim())
        if (whole != null) {
            val value = lookup(values, whole.groupValues[1])
            val filter = whole.groupValues[2]
            if (filter.isEmpty() && (value is Number || value is Boolean)) return value
            return format(value, filter)
        }
        return PLACEHOLDER.replace(s) { m -> format(lookup(values, m.groupValues[1]), m.groupValues[2]) }
    }

    /** Walks a dot path ("current.temperature_2m", "items.0.name"); missing parts give null. */
    fun lookup(values: JSONObject, path: String): Any? {
        var node: Any? = values
        for (part in path.trim().split('.')) {
            node = when (node) {
                is JSONObject -> node.valueOf(part)
                is JSONArray -> part.toIntOrNull()?.let { if (it in 0 until node.length()) node.opt(it) else null }
                else -> null
            }
            if (node == null || node == JSONObject.NULL) return null
        }
        return node
    }

    private fun format(value: Any?, filter: String): String {
        if (value == null || value == JSONObject.NULL) return ""
        val n = (value as? Number)?.toDouble() ?: value.toString().trim().toDoubleOrNull()
        return when (filter.lowercase()) {
            "round" -> n?.roundToLong()?.toString() ?: value.toString()
            "floor" -> n?.let { floor(it).toLong().toString() } ?: value.toString()
            "ceil" -> n?.let { ceil(it).toLong().toString() } ?: value.toString()
            "fixed1" -> n?.let { String.format(Locale.US, "%.1f", it) } ?: value.toString()
            "fixed2" -> n?.let { String.format(Locale.US, "%.2f", it) } ?: value.toString()
            "upper" -> value.toString().uppercase()
            "lower" -> value.toString().lowercase()
            "time" -> Dates.parse(value)?.let { fmt("HH:mm", it) } ?: value.toString()
            "date" -> Dates.parse(value)?.let { fmt("d MMM", it) } ?: value.toString()
            else -> if (value is Double && value == floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()
        }
    }

    private fun fmt(pattern: String, ms: Long) = SimpleDateFormat(pattern, Locale.getDefault()).format(ms)

    /**
     * `visible` after placeholders are filled: a boolean, a number (0 = hidden),
     * or a string condition: "a == b", "a != b", "a > b", "a >= b", "a < b", "a <= b",
     * or a single value that is hidden when empty, "false", "0" or "no".
     */
    fun truthy(value: Any?): Boolean = when (value) {
        null, JSONObject.NULL -> true
        is Boolean -> value
        is Number -> value.toDouble() != 0.0
        is String -> condition(value)
        else -> true
    }

    private val OPS = listOf("==", "!=", ">=", "<=", ">", "<")

    private fun condition(raw: String): Boolean {
        val s = raw.trim()
        for (op in OPS) {
            val i = s.indexOf(op)
            if (i < 0) continue
            val a = s.substring(0, i).trim().trim('"', '\'')
            val b = s.substring(i + op.length).trim().trim('"', '\'')
            val na = a.toDoubleOrNull()
            val nb = b.toDoubleOrNull()
            val cmp = if (na != null && nb != null) na.compareTo(nb) else a.compareTo(b, ignoreCase = true)
            return when (op) {
                "==" -> cmp == 0
                "!=" -> cmp != 0
                ">=" -> cmp >= 0
                "<=" -> cmp <= 0
                ">" -> cmp > 0
                else -> cmp < 0
            }
        }
        return s.isNotEmpty() && s.lowercase() !in setOf("false", "0", "no", "null")
    }

    /** Copies every key of [from] into [into] (objects are merged one level deep only by replacement). */
    fun merge(into: JSONObject, from: JSONObject): JSONObject {
        from.keys().forEach { into.put(it, from.opt(it)) }
        return into
    }
}

/** Epoch milliseconds from a number, digits, or an ISO-8601 date / date-time (as ODC JSONSerialize writes it). */
object Dates {
    private val PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mmXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
    )

    fun parse(value: Any?): Long? {
        when (value) {
            null, JSONObject.NULL -> return null
            is Number -> return value.toLong()
        }
        val s = value.toString().trim()
        if (s.isEmpty()) return null
        s.toLongOrNull()?.let { return it }
        // "Z" means UTC; SimpleDateFormat's XXX accepts it on Android.
        for (p in PATTERNS) {
            val f = SimpleDateFormat(p, Locale.US).apply { isLenient = false }
            if (!p.contains("XXX")) f.timeZone = TimeZone.getDefault()
            runCatching { return f.parse(s)!!.time }
        }
        return null
    }
}
