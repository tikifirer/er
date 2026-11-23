package com.customhub

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast

object ThemeScreen {

    fun build(activity: Activity, ui: Ui): LinearLayout {
        val pal = ui.pal
        return ui.column().apply {
            addView(ui.title("Themes & colours"))
            addView(ui.note("Restyle the hub itself, plus the clock widget, with one tap or by hand."))

            // ---- presets ----
            addView(ui.section("Preset themes"))
            val row = LinearLayout(activity).apply { setPadding(0, 0, 0, ui.dp(4)) }
            Themes.presets.forEach { preset ->
                val light = preset.mode == 1
                val tile = ui.column().apply {
                    gravity = Gravity.CENTER
                    setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(12))
                    background = ui.roundRect(
                        if (light) 0xFFF5F5FA.toInt() else 0xFF15151C.toInt(),
                        minOf(preset.radius, 24), preset.accent, 2
                    )
                    addView(View(activity).apply {
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(preset.accent)
                        }
                    }, LinearLayout.LayoutParams(ui.dp(30), ui.dp(30)))
                    addView(
                        ui.text(preset.name, 13f, if (light) 0xFF14141C.toInt() else 0xFFF2F2F7.toInt(), true)
                            .apply { setPadding(0, ui.dp(8), 0, 0) }
                    )
                    setOnClickListener {
                        Themes.apply(activity, preset)
                        ui.onChange("t_preset")
                    }
                }
                row.addView(tile, LinearLayout.LayoutParams(ui.dp(104), ViewGroupWrap).apply {
                    rightMargin = ui.dp(10)
                })
            }
            addView(HorizontalScrollView(activity).apply {
                isHorizontalScrollBarEnabled = false
                addView(row)
            })

            // ---- mode and colour ----
            addView(ui.section("Look"))
            addView(ui.card {
                ui.chips(this, "Mode", K.T_MODE, listOf("System", "Light", "Dark", "AMOLED"), 0)
                ui.colorPicker(this, "Accent colour", K.T_ACCENT, Themes.accents, Themes.DEFAULT_ACCENT)
            })

            addView(ui.section("Cards"))
            addView(ui.card {
                ui.chips(this, "Style", K.T_CARD, listOf("Solid", "Glass", "Outline"), 0)
                ui.slider(this, "Corner roundness", K.T_RADIUS, 0, 40, 20, " dp")
            })

            // ---- system shortcuts ----
            addView(ui.section("Android's own settings"))
            addView(ui.note(
                "Android only lets an app restyle itself, its widgets and its shortcuts. " +
                    "System-wide dark mode, fonts and wallpaper colours live in Settings, so here are shortcuts."
            ))
            val shortcuts = listOf(
                "Display, dark mode and font size" to Settings.ACTION_DISPLAY_SETTINGS,
                "Wallpaper" to Intent.ACTION_SET_WALLPAPER,
                "Sounds and vibration" to Settings.ACTION_SOUND_SETTINGS,
                "Default apps" to Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
            )
            shortcuts.forEach { (label, action) ->
                addView(ui.button(label, false) {
                    try {
                        val intent = Intent(action)
                        activity.startActivity(
                            if (action == Intent.ACTION_SET_WALLPAPER) Intent.createChooser(intent, "Wallpaper")
                            else intent
                        )
                    } catch (e: Exception) {
                        Toast.makeText(activity, "That screen isn't available on this phone", Toast.LENGTH_SHORT).show()
                    }
                }, ui.lp(top = 8))
            }
        }
    }

    private const val ViewGroupWrap = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
}
