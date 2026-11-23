package com.customhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews

/** The home screen clock. Its look comes from the Widgets tab, so every copy matches. */
class ClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = build(context)
        for (id in ids) manager.updateAppWidget(id, views)
    }

    companion object {
        private val layouts = arrayOf(
            intArrayOf(
                R.layout.widget_clock_inline_thin, R.layout.widget_clock_inline_regular,
                R.layout.widget_clock_inline_black, R.layout.widget_clock_inline_serif
            ),
            intArrayOf(
                R.layout.widget_clock_stacked_thin, R.layout.widget_clock_stacked_regular,
                R.layout.widget_clock_stacked_black, R.layout.widget_clock_stacked_serif
            )
        )

        val cornerDrawables = intArrayOf(
            R.drawable.wbg_r0, R.drawable.wbg_r1, R.drawable.wbg_r2, R.drawable.wbg_r3
        )

        val datePatterns = arrayOf("EEE d MMM", "EEEE", "d MMMM yyyy")

        /** Redraws every clock and quick-actions widget on the home screen with the current settings. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val clockIds = manager.getAppWidgetIds(ComponentName(context, ClockWidget::class.java))
            if (clockIds.isNotEmpty()) {
                val views = build(context)
                for (id in clockIds) manager.updateAppWidget(id, views)
            }
            val actionIds = manager.getAppWidgetIds(ComponentName(context, QuickActionsWidget::class.java))
            if (actionIds.isNotEmpty()) {
                val views = QuickActionsWidget.build(context)
                for (id in actionIds) manager.updateAppWidget(id, views)
            }
        }

        /** The tinted, rounded panel behind a widget. Shared by both widgets. */
        fun applyBackground(context: Context, views: RemoteViews) {
            val p = Hub.prefs(context)
            views.setImageViewResource(
                R.id.bg, cornerDrawables[p.getInt(K.W_RADIUS, 2).coerceIn(0, 3)]
            )
            views.setInt(R.id.bg, "setColorFilter", p.getInt(K.W_BG, 0xFF14142A.toInt()))
            views.setInt(R.id.bg, "setImageAlpha", p.getInt(K.W_BG_ALPHA, 85).coerceIn(0, 100) * 255 / 100)
        }

        fun build(context: Context): RemoteViews {
            val p = Hub.prefs(context)
            val font = p.getInt(K.W_FONT, 1).coerceIn(0, 3)
            val stacked = p.getInt(K.W_ARRANGE, 0) == 1
            val views = RemoteViews(context.packageName, layouts[if (stacked) 1 else 0][font])

            val h24 = p.getBoolean(K.W_24H, DateFormat.is24HourFormat(context))
            val color = p.getInt(K.W_TEXT, 0xFFF2F2FF.toInt())
            val size = p.getInt(K.W_SIZE, 56).toFloat()

            applyBackground(context, views)

            fun style(id: Int, pattern: String, sp: Float) {
                // Both formats are set so our 12/24-hour choice wins over the phone's setting.
                views.setCharSequence(id, "setFormat12Hour", pattern)
                views.setCharSequence(id, "setFormat24Hour", pattern)
                views.setTextColor(id, color)
                views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp)
            }

            if (stacked) {
                style(R.id.time_h, if (h24) "HH" else "h", size)
                style(R.id.time_m, "mm", size)
            } else {
                style(R.id.time, if (h24) "HH:mm" else "h:mm", size)
            }

            if (p.getBoolean(K.W_SHOW_DATE, true)) {
                views.setViewVisibility(R.id.date, View.VISIBLE)
                style(
                    R.id.date,
                    datePatterns[p.getInt(K.W_DATE_FMT, 0).coerceIn(0, datePatterns.size - 1)],
                    maxOf(12f, size * 0.28f)
                )
            } else {
                views.setViewVisibility(R.id.date, View.GONE)
            }

            // Tapping the clock opens the hub.
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.root, open)
            return views
        }
    }
}
