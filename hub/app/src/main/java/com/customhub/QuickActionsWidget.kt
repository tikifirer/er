package com.customhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A row of four buttons (lock, screenshot, notifications...) that you choose in the Widgets tab. */
class QuickActionsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = build(context)
        for (id in ids) manager.updateAppWidget(id, views)
    }

    companion object {
        private val buttons = intArrayOf(R.id.act1, R.id.act2, R.id.act3, R.id.act4)
        private val keys = arrayOf(K.W_ACT1, K.W_ACT2, K.W_ACT3, K.W_ACT4)
        private val defaults = intArrayOf(
            Actions.LOCK, Actions.SCREENSHOT, Actions.NOTIFICATIONS, Actions.QUICK_SETTINGS
        )

        fun glyph(action: Int): String = when (action) {
            Actions.LOCK -> "🔒"
            Actions.NOTIFICATIONS -> "🔔"
            Actions.QUICK_SETTINGS -> "⚙"
            Actions.HOME -> "⌂"
            Actions.BACK -> "◀"
            Actions.RECENTS -> "▭"
            Actions.SCREENSHOT -> "📷"
            Actions.POWER -> "⏻"
            else -> "·"
        }

        fun defaultFor(index: Int): Int = defaults[index]

        fun build(context: Context): RemoteViews {
            val p = Hub.prefs(context)
            val views = RemoteViews(context.packageName, R.layout.widget_actions)
            ClockWidget.applyBackground(context, views)
            val color = p.getInt(K.W_TEXT, 0xFFF2F2FF.toInt())

            buttons.forEachIndexed { i, viewId ->
                val action = p.getInt(keys[i], defaults[i]).coerceIn(0, Actions.POWER)
                views.setTextViewText(viewId, "${glyph(action)}\n${Actions.label(action)}")
                views.setTextColor(viewId, color)
                if (action != Actions.NONE) {
                    val intent = Intent(context, ActionActivity::class.java)
                        .putExtra(ActionActivity.EXTRA_ACTION, action)
                    views.setOnClickPendingIntent(
                        viewId,
                        PendingIntent.getActivity(
                            context, 100 + i * 20 + action, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
            return views
        }
    }
}
