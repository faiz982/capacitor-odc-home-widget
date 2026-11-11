package com.odchomewidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * PendingIntents for widget taps.
 *
 * Open taps start the host app's launcher activity with the deep link as an
 * extra. CLEAR_TOP + SINGLE_TOP reuse the running activity (onNewIntent) instead
 * of stacking a second copy. Toggle taps go to [WidgetActionReceiver] and never
 * open the app. Each view gets its own request code; FLAG_UPDATE_CURRENT
 * replaces the extras when the data changes.
 */
object Taps {
    const val EXTRA_DEEP_LINK = "odcWidgetDeepLink"
    const val EXTRA_KIND = "odcWidgetKind"
    const val EXTRA_ITEM_ID = "odcWidgetItemId"
    const val ACTION_TOGGLE = "com.odchomewidget.ACTION_TOGGLE"

    private const val TEMPLATE_SLOT = 99

    /** Collection templates are filled in per row, so they must be mutable on Android 12+. */
    private val MUTABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

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

    fun openTemplate(context: Context, kind: WidgetKind): PendingIntent = PendingIntent.getActivity(
        context,
        code(kind, TEMPLATE_SLOT),
        launch(context).putExtra(EXTRA_KIND, kind.name),
        PendingIntent.FLAG_UPDATE_CURRENT or MUTABLE,
    )

    fun openFill(kind: WidgetKind, deepLink: String): Intent {
        val intent = Intent().putExtra(EXTRA_KIND, kind.name)
        if (deepLink.isNotBlank()) intent.putExtra(EXTRA_DEEP_LINK, deepLink)
        return intent
    }

    fun toggle(context: Context, kind: WidgetKind, itemId: String, slot: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        code(kind, slot),
        toggleIntent(context, kind).putExtra(EXTRA_ITEM_ID, itemId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun toggleTemplate(context: Context, kind: WidgetKind): PendingIntent = PendingIntent.getBroadcast(
        context,
        code(kind, TEMPLATE_SLOT),
        toggleIntent(context, kind),
        PendingIntent.FLAG_UPDATE_CURRENT or MUTABLE,
    )

    fun toggleFill(itemId: String): Intent = Intent().putExtra(EXTRA_ITEM_ID, itemId)

    private fun toggleIntent(context: Context, kind: WidgetKind) =
        Intent(context, WidgetActionReceiver::class.java)
            .setAction(ACTION_TOGGLE)
            .putExtra(EXTRA_KIND, kind.name)
}
