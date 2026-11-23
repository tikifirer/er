package com.customhub

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast

object GesturesScreen {

    fun build(activity: Activity, ui: Ui): LinearLayout {
        val pal = ui.pal
        val on = GestureService.instance != null
        val names = Actions.ids.map { Actions.label(it) }

        fun open(intent: Intent) {
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(activity, "Couldn't open that screen", Toast.LENGTH_SHORT).show()
            }
        }

        return ui.column().apply {
            addView(ui.title("Gestures"))
            addView(ui.note(
                "A thin handle on the edge of your screen. Tap it, double tap it, hold it or swipe it " +
                    "to lock the phone, take a screenshot, open notifications and more."
            ))

            // ---- service status and set-up ----
            addView(ui.section("Set-up"))
            addView(ui.card {
                addView(ui.text(
                    if (on) "● Gestures are on" else "○ Gestures are off",
                    17f, if (on) pal.accent else pal.muted, true
                ))
                addView(ui.note(
                    "Android needs the Accessibility switch for this. Custom Hub only presses " +
                        "buttons like Lock and Screenshot for you. It cannot read your screen."
                ))
                addView(ui.text("1. Allow restricted settings", 14f, pal.text, true), ui.lp(top = 6))
                addView(ui.note(
                    "Because this app was installed from a file, Android blocks the switch until you " +
                        "open App info, tap the ⋮ menu at the top right, and choose \"Allow restricted settings\"."
                ))
                addView(ui.button("Open App info", false) {
                    open(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${activity.packageName}"))
                    )
                })
                addView(ui.text("2. Turn it on", 14f, pal.text, true), ui.lp(top = 14))
                addView(ui.note("Open Accessibility, find Custom Hub (it may be under Installed apps), and switch it on."))
                addView(ui.button("Open Accessibility settings") {
                    open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                })
            })

            // ---- handle ----
            addView(ui.section("Edge handle"))
            addView(ui.card {
                ui.toggle(this, "Show the handle", K.G_ENABLED, true)
                ui.chips(this, "Edge", K.G_EDGE, listOf("Left", "Right"), 1)
                ui.slider(this, "Height on screen", K.G_POSITION, 0, 100, 50, "%")
                ui.slider(this, "Handle length", K.G_LENGTH, 10, 70, 30, "%")
                ui.slider(this, "Handle width (touch area)", K.G_THICKNESS, 6, 40, 14, " dp")
                ui.toggle(this, "Draw a visible line", K.G_VISIBLE, true, "Off keeps it invisible but still touchable")
                ui.slider(this, "Line opacity", K.G_OPACITY, 5, 100, 35, "%")
            })

            // ---- actions ----
            addView(ui.section("What each gesture does"))
            addView(ui.card {
                ui.chips(this, "Tap", K.G_TAP, names, Actions.NONE)
                ui.chips(this, "Double tap", K.G_DOUBLE, names, Actions.LOCK)
                ui.chips(this, "Long press", K.G_LONG, names, Actions.SCREENSHOT)
                ui.chips(this, "Swipe up", K.G_UP, names, Actions.HOME)
                ui.chips(this, "Swipe down", K.G_DOWN, names, Actions.NOTIFICATIONS)
            })
        }
    }
}
