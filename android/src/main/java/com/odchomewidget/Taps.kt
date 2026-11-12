package com.odchomewidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * PendingIntents for widget taps. A tap starts the host app's launcher activity
 * with the deep link as an extra. CLEAR_TOP + SINGLE_TOP reuse the running
 * activity (onNewIntent) instead of stacking a second copy. Each tappable view
 * gets its own request code; FLAG_UPDATE_CURRENT replaces the extras when the
 * data changes.
 */
object Taps {
    const val EXTRA_DEEP_LINK = "odcWidgetDeepLink"
    const val EXTRA_KIND = "odcWidgetKind"

    private fun code(kind: WidgetKind, slot: Int) = kind.ordinal * 100 + slot

    private fun launch(context: Context): Intent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        return intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
    }

    fun open(context: Context, kind: WidgetKind, deepLink: String?, slot: Int): PendingIntent {
        val intent = launch(context).putExtra(EXTRA_KIND, kind.name)
        if (!deepLink.isNullOrBlank()) intent.putExtra(EXTRA_DEEP_LINK, deepLink)
        return PendingIntent.getActivity(
            context,
            code(kind, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
