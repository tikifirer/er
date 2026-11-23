package com.customhub

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private var tab = 0
    private lateinit var ui: Ui
    private lateinit var content: FrameLayout
    private val navItems = ArrayList<TextView>()

    private val tabs = listOf(
        "◐" to "Themes",
        "▣" to "Icons",
        "⌁" to "Gestures",
        "◷" to "Widgets"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getInt("tab", 0) ?: 0

        val prefs = Hub.prefs(this)
        val pal = Themes.resolve(this)
        ui = Ui(this, pal, prefs) { key -> onSettingChanged(key) }

        window.statusBarColor = pal.bg
        window.navigationBarColor = pal.surface
        if (!pal.dark) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        content = FrameLayout(this)

        val nav = LinearLayout(this).apply {
            setBackgroundColor(pal.surface)
            setPadding(0, ui.dp(6), 0, ui.dp(6))
        }
        tabs.forEachIndexed { index, (glyph, name) ->
            val item = TextView(this).apply {
                text = "$glyph\n$name"
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, ui.dp(6), 0, ui.dp(6))
                setOnClickListener { select(index) }
            }
            navItems.add(item)
            nav.addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pal.bg)
            addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(nav)
        })
        select(tab)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", tab)
    }

    override fun onResume() {
        super.onResume()
        // The gestures tab shows whether the service is on; refresh it after returning from Settings.
        if (tab == 2) select(2)
    }

    private fun select(index: Int) {
        tab = index
        navItems.forEachIndexed { i, item ->
            item.setTextColor(if (i == index) ui.pal.accent else ui.pal.muted)
        }
        val screen = when (index) {
            0 -> ThemeScreen.build(this, ui)
            1 -> IconsScreen.build(this, ui)
            2 -> GesturesScreen.build(this, ui)
            else -> WidgetsScreen.build(this, ui)
        }
        screen.setPadding(ui.dp(16), ui.dp(20), ui.dp(16), ui.dp(28))
        content.removeAllViews()
        content.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(screen)
        })
    }

    private fun onSettingChanged(key: String) {
        when {
            key.startsWith("t_") -> {
                ClockWidget.refreshAll(this)
                recreate()
            }
            key.startsWith("w_") -> ClockWidget.refreshAll(this)
        }
    }
}
