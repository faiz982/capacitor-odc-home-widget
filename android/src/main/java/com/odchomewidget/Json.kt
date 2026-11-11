package com.odchomewidget

import org.json.JSONArray
import org.json.JSONObject

// Case-insensitive JSON helpers: ODC's JSONSerialize keeps the attribute names
// as they are written in Studio (Title, IsOn, ...), so "title" must match "Title".

/** The actual key in this object that matches [name], ignoring case. */
internal fun JSONObject.keyFor(name: String): String? =
    if (has(name)) name else keys().asSequence().firstOrNull { it.equals(name, ignoreCase = true) }

/** The value for [name], or null when it is missing or JSON null. */
internal fun JSONObject.valueOf(name: String): Any? {
    val key = keyFor(name) ?: return null
    return if (isNull(key)) null else opt(key)
}

internal fun JSONObject.str(name: String): String = when (val v = valueOf(name)) {
    null, is JSONObject, is JSONArray -> ""
    else -> v.toString()
}

internal fun JSONObject.arr(name: String): JSONArray? = valueOf(name) as? JSONArray

internal fun JSONObject.obj(name: String): JSONObject? = valueOf(name) as? JSONObject

internal fun JSONObject.bool(name: String): Boolean = when (val v = valueOf(name)) {
    is Boolean -> v
    is Number -> v.toInt() != 0
    is String -> v.equals("true", ignoreCase = true) || v == "1"
    else -> false
}

internal fun JSONObject.num(name: String): Double? = when (val v = valueOf(name)) {
    is Number -> v.toDouble()
    is String -> v.trim().toDoubleOrNull()
    else -> null
}

/** The first [max] objects of the array; non-object entries are skipped. */
internal fun JSONArray?.objects(max: Int): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until minOf(length(), max)).mapNotNull { optJSONObject(it) }
}
