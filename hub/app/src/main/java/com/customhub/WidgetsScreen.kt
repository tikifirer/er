package com.customhub

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.Toast

object WidgetsScreen {

    private val fonts = arrayOf("sans-serif-thin", "sans-serif", "sans-serif-black", "serif")
    private val cornerDp = intArrayOf(0, 14, 28, 48)

    private val panelColors = intArrayOf(
        0xFF14142A.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF1B2A41.toInt(),
        0xFF3A1420.toInt(), 0xFF10261B.toInt(), 0xFFFFE4EE.toInt(), 0xFFB06CFF.toInt(),
        0xFF2AA5FF.toInt(), 0xFFFF7A45.toInt()
    )
    private val textColors = intArrayOf(
        0xFFF2F2FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF5A1B36.toInt(),
        0xFF7C6CFF.toInt(), 0xFF3DDC84.toInt(), 0xFFFFC53D.toInt(), 0xFFFF7A45.toInt()
    )

    fun build(activity: Activity, hostUi: Ui): LinearLayout {
        val prefs = hostUi.prefs
        val pal = hostUi.pal
        val preview = FrameLayout(activity)

        /** An in-app copy of the clock, so you can see changes without leaving this screen. */
        fun refreshPreview(ui: Ui) {
            val stacked = prefs.getInt(K.W_ARRANGE, 0) == 1
            val h24 = prefs.getBoolean(K.W_24H, android.text.format.DateFormat.is24HourFormat(activity))
            val color = prefs.getInt(K.W_TEXT, 0xFFF2F2FF.toInt())
            val size = prefs.getInt(K.W_SIZE, 56).toFloat()
            val face = Typeface.create(fonts[prefs.getInt(K.W_FONT, 1).coerceIn(0, 3)], Typeface.NORMAL)
            val alpha = prefs.getInt(K.W_BG_ALPHA, 85).coerceIn(0, 100) * 255 / 100
            val panel = (prefs.getInt(K.W_BG, 0xFF14142A.toInt()) and 0x00FFFFFF) or (alpha shl 24)

            fun clock(pattern: String, sp: Float) = TextClock(activity).apply {
                format12Hour = pattern
                format24Hour = pattern
                typeface = face
                setTextColor(color)
                textSize = sp
                includeFontPadding = false
                gravity = Gravity.CENTER
            }

            val column = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14))
                background = ui.roundRect(
                    panel, cornerDp[prefs.getInt(K.W_RADIUS, 2).coerceIn(0, 3)]
                )
                if (stacked) {
                    addView(clock(if (h24) "HH" else "h", size))
                    addView(clock("mm", size))
                } else {
                    addView(clock(if (h24) "HH:mm" else "h:mm", size))
                }
                if (prefs.getBoolean(K.W_SHOW_DATE, true)) {
                    addView(
                        clock(
                            ClockWidget.datePatterns[prefs.getInt(K.W_DATE_FMT, 0).coerceIn(0, 2)],
                            maxOf(12f, size * 0.28f)
                        )
                    )
                }
            }
            preview.removeAllViews()
            preview.addView(column, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        lateinit var ui: Ui
        ui = Ui(activity, pal, prefs) { key ->
            if (key.startsWith("w_")) ClockWidget.refreshAll(activity)
            refreshPreview(ui)
        }
        refreshPreview(ui)

        fun pin(provider: Class<*>, name: String) {
            val manager = AppWidgetManager.getInstance(activity)
            if (manager.isRequestPinAppWidgetSupported) {
                manager.requestPinAppWidget(ComponentName(activity, provider), null, null)
            } else {
                Toast.makeText(
                    activity,
                    "Press and hold your home screen, tap Widgets, and look for \"$name\"",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val actionNames = Actions.ids.map { Actions.label(it) }

        return ui.column().apply {
            addView(ui.title("Widgets & clock"))
            addView(ui.note("Design a clock for your home screen. Every change shows up on any clock you've already added."))

            addView(ui.section("Preview"))
            addView(preview)

            addView(ui.section("Clock style"))
            addView(ui.card {
                ui.chips(this, "Font", K.W_FONT, listOf("Thin", "Regular", "Bold", "Serif"), 1)
                ui.chips(this, "Layout", K.W_ARRANGE, listOf("Inline", "Stacked"), 0)
                ui.slider(this, "Clock size", K.W_SIZE, 28, 96, 56, " sp")
                ui.toggle(
                    this, "24-hour time", K.W_24H,
                    android.text.format.DateFormat.is24HourFormat(activity)
                )
                ui.toggle(this, "Show the date", K.W_SHOW_DATE, true)
                ui.chips(this, "Date format", K.W_DATE_FMT, listOf("Tue 24 Nov", "Tuesday", "24 November 2026"), 0)
            })

            addView(ui.section("Colours & shape"))
            addView(ui.card {
                ui.colorPicker(this, "Panel colour", K.W_BG, panelColors, 0xFF14142A.toInt())
                ui.slider(this, "Panel opacity", K.W_BG_ALPHA, 0, 100, 85, "%")
                ui.colorPicker(this, "Text colour", K.W_TEXT, textColors, 0xFFF2F2FF.toInt())
                ui.chips(this, "Corners", K.W_RADIUS, listOf("Square", "Soft", "Round", "Pill"), 2)
            })

            addView(ui.section("Quick actions widget"))
            addView(ui.note("Four buttons on your home screen. They share the colours above and need the gesture switch from the Gestures tab."))
            addView(ui.card {
                ui.chips(this, "Button 1", K.W_ACT1, actionNames, QuickActionsWidget.defaultFor(0))
                ui.chips(this, "Button 2", K.W_ACT2, actionNames, QuickActionsWidget.defaultFor(1))
                ui.chips(this, "Button 3", K.W_ACT3, actionNames, QuickActionsWidget.defaultFor(2))
                ui.chips(this, "Button 4", K.W_ACT4, actionNames, QuickActionsWidget.defaultFor(3))
            })

            addView(ui.section("Add to your home screen"))
            addView(ui.button("Add the clock") { pin(ClockWidget::class.java, "Hub clock") })
            addView(
                ui.button("Add quick actions", false) {
                    pin(QuickActionsWidget::class.java, "Hub quick actions")
                },
                ui.lp(top = 8)
            )
        }
    }
}
