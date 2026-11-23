package com.customhub

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast

object IconsScreen {

    private class AppItem(val label: String, val component: ComponentName, val icon: Drawable)

    fun build(activity: Activity, hostUi: Ui): LinearLayout {
        val prefs = hostUi.prefs
        val pal = hostUi.pal

        var selected: AppItem? = null
        val preview = ImageView(activity)
        val labelField = EditText(activity)

        fun renderCurrent(): android.graphics.Bitmap? {
            val app = selected ?: return null
            return IconRenderer.render(
                app.icon.constantState?.newDrawable()?.mutate() ?: app.icon,
                prefs.getInt(K.I_SHAPE, 1),
                prefs.getInt(K.I_SIZE, 85),
                prefs.getInt(K.I_BG_MODE, 0),
                prefs.getInt(K.I_BG, 0xFF7C6CFF.toInt()),
                pal.accent,
                prefs.getBoolean(K.I_BORDER, false)
            )
        }

        fun refreshPreview() {
            val bmp = renderCurrent()
            if (bmp != null) preview.setImageDrawable(BitmapDrawable(activity.resources, bmp))
        }

        // Style controls repaint the preview instead of touching the theme or widgets.
        val ui = Ui(activity, pal, prefs) { refreshPreview() }

        val apps = ArrayList<AppItem>()
        val listHolder = ui.column()

        fun select(app: AppItem) {
            selected = app
            labelField.setText(app.label)
            refreshPreview()
        }

        fun showApps(filter: String) {
            listHolder.removeAllViews()
            val query = filter.trim().lowercase()
            apps.filter { query.isEmpty() || it.label.lowercase().contains(query) }.forEach { app ->
                listHolder.addView(LinearLayout(activity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(ui.dp(6), ui.dp(8), ui.dp(6), ui.dp(8))
                    addView(ImageView(activity).apply { setImageDrawable(app.icon) },
                        LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)))
                    addView(ui.text(app.label, 15f, pal.text).apply { setPadding(ui.dp(14), 0, 0, 0) })
                    setOnClickListener { select(app) }
                })
            }
        }

        // Load the launchable apps off the main thread, then fill the list.
        Thread {
            val pm = activity.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val found = pm.queryIntentActivities(intent, 0).map {
                AppItem(
                    it.loadLabel(pm).toString(),
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                    it.loadIcon(pm)
                )
            }.sortedBy { it.label.lowercase() }
            activity.runOnUiThread {
                apps.clear()
                apps.addAll(found)
                if (selected == null && apps.isNotEmpty()) select(apps[0])
                showApps("")
            }
        }.start()

        fun pin() {
            val app = selected
            val bmp = renderCurrent()
            if (app == null || bmp == null) {
                Toast.makeText(activity, "Pick an app first", Toast.LENGTH_SHORT).show()
                return
            }
            val manager = activity.getSystemService(ShortcutManager::class.java)
            if (manager == null || !manager.isRequestPinShortcutSupported) {
                Toast.makeText(
                    activity, "Your home screen doesn't support adding shortcuts", Toast.LENGTH_LONG
                ).show()
                return
            }
            val label = labelField.text.toString().ifBlank { app.label }
            val launch = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val info = ShortcutInfo.Builder(activity, "hub-${System.currentTimeMillis()}")
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(Icon.createWithBitmap(bmp))
                .setIntent(launch)
                .build()
            manager.requestPinShortcut(info, null)
        }

        return ui.column().apply {
            addView(ui.title("Icon shapes & sizes"))
            addView(ui.note(
                "Restyle any app's icon and add it to your home screen as a shortcut. " +
                    "The original icon stays where it is, so hide or remove it in your launcher if you like."
            ))

            // ---- preview + label ----
            addView(ui.section("Preview"))
            addView(ui.card {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(preview, LinearLayout.LayoutParams(ui.dp(112), ui.dp(112)))
                labelField.apply {
                    hint = "Label"
                    setTextColor(pal.text)
                    setHintTextColor(pal.muted)
                    gravity = Gravity.CENTER
                    setSingleLine()
                    background = ui.roundRect(pal.surfaceHi, 14)
                    setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
                }
                addView(labelField, ui.lp(top = 12))
                addView(ui.button("Add to home screen") { pin() }, ui.lp(top = 12))
            })

            // ---- style ----
            addView(ui.section("Style"))
            addView(ui.card {
                ui.chips(this, "Shape", K.I_SHAPE, IconRenderer.shapes, 1)
                ui.slider(this, "Icon size", K.I_SIZE, 50, 130, 85, "%")
                ui.chips(this, "Background", K.I_BG_MODE, listOf("Theme accent", "Custom colour", "Keep original"), 0)
                ui.colorPicker(this, "Custom background colour", K.I_BG, Themes.accents, 0xFF7C6CFF.toInt())
                ui.toggle(this, "Soft border", K.I_BORDER, false)
            })

            // ---- app list ----
            addView(ui.section("Choose an app"))
            val search = EditText(activity).apply {
                hint = "Search apps"
                setTextColor(pal.text)
                setHintTextColor(pal.muted)
                setSingleLine()
                background = ui.roundRect(pal.surfaceHi, 14)
                setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) = showApps(s?.toString() ?: "")
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                })
            }
            addView(search, ui.lp(bottom = 8))
            addView(listHolder)
        }
    }
}
