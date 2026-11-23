package com.customhub

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * Small toolkit for building the hub's screens in code. Every control saves straight to
 * settings and then calls [onChange] with the key, so the host can refresh widgets or the theme.
 * Keys starting with "t_" (theme) are reported when a slider is let go, not while it moves.
 */
class Ui(
    val ctx: Context,
    val pal: Palette,
    val prefs: SharedPreferences,
    val onChange: (String) -> Unit
) {
    fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    private val full = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    fun lp(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(full, wrap).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    fun roundRect(color: Int, radiusDp: Int, strokeColor: Int = 0, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    // ---- text ---------------------------------------------------------------------------

    fun text(value: String, sizeSp: Float, color: Int, bold: Boolean = false) = TextView(ctx).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    fun title(value: String) = text(value, 26f, pal.text, true).apply { setPadding(0, dp(4), 0, dp(2)) }

    fun section(value: String) = text(value.uppercase(), 12f, pal.accent, true).apply {
        letterSpacing = 0.12f
        setPadding(dp(4), dp(22), 0, dp(8))
    }

    fun note(value: String) = text(value, 13f, pal.muted).apply { setPadding(dp(4), dp(2), dp(4), dp(8)) }

    // ---- containers ---------------------------------------------------------------------

    fun column(): LinearLayout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

    /** A rounded panel that follows the chosen card style (solid, glass or outline). */
    fun card(build: LinearLayout.() -> Unit): LinearLayout = column().apply {
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = when (pal.card) {
            1 -> roundRect(
                (pal.surface and 0x00FFFFFF) or 0xB0000000.toInt(), pal.radius,
                if (pal.dark) 0x33FFFFFF else 0x22000000, 1
            )
            2 -> roundRect(Color.TRANSPARENT, pal.radius, pal.muted, 1)
            else -> roundRect(pal.surface, pal.radius)
        }
        build()
    }

    fun spacer(h: Int) = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(full, dp(h)) }

    // ---- buttons ------------------------------------------------------------------------

    fun button(label: String, primary: Boolean = true, onClick: () -> Unit) = TextView(ctx).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) pal.onAccent else pal.text)
        background = roundRect(
            if (primary) pal.accent else pal.surfaceHi, minOf(pal.radius, 28)
        )
        setPadding(dp(16), dp(13), dp(16), dp(13))
        setOnClickListener { onClick() }
    }

    // ---- controls -----------------------------------------------------------------------

    fun slider(
        parent: LinearLayout, label: String, key: String,
        min: Int, max: Int, def: Int, unit: String = ""
    ) {
        val current = prefs.getInt(key, def).coerceIn(min, max)
        val value = text("$current$unit", 14f, pal.muted)
        val head = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text(label, 15f, pal.text), LinearLayout.LayoutParams(0, wrap, 1f))
            addView(value)
        }
        val deferred = key.startsWith("t_")
        val bar = SeekBar(ctx).apply {
            this.max = max - min
            progress = current - min
            progressTintList = ColorStateList.valueOf(pal.accent)
            thumbTintList = ColorStateList.valueOf(pal.accent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, v: Int, fromUser: Boolean) {
                    value.text = "${v + min}$unit"
                    if (!fromUser) return
                    prefs.edit().putInt(key, v + min).apply()
                    if (!deferred) onChange(key)
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    if (deferred) onChange(key)
                }
            })
        }
        parent.addView(column().apply {
            setPadding(0, dp(8), 0, dp(2))
            addView(head)
            addView(bar)
        })
    }

    fun toggle(parent: LinearLayout, label: String, key: String, def: Boolean, sub: String? = null) {
        val sw = Switch(ctx).apply {
            isChecked = prefs.getBoolean(key, def)
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(pal.accent, pal.muted))
            trackTintList = ColorStateList(
                states, intArrayOf((pal.accent and 0x00FFFFFF) or 0x66000000, pal.surfaceHi)
            )
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
                onChange(key)
            }
        }
        val texts = column().apply {
            addView(text(label, 15f, pal.text))
            if (sub != null) addView(text(sub, 12f, pal.muted))
        }
        parent.addView(LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(texts, LinearLayout.LayoutParams(0, wrap, 1f))
            addView(sw)
        })
    }

    /** A scrolling row of pill-shaped choices. The saved value is the option's position. */
    fun chips(parent: LinearLayout, label: String, key: String, options: List<String>, def: Int) {
        val pills = ArrayList<TextView>()
        val row = LinearLayout(ctx).apply { setPadding(0, dp(6), 0, dp(4)) }
        fun paint(selected: Int) {
            pills.forEachIndexed { i, pill ->
                val on = i == selected
                pill.setTextColor(if (on) pal.onAccent else pal.text)
                pill.background = roundRect(if (on) pal.accent else pal.surfaceHi, 40)
            }
        }
        options.forEachIndexed { i, name ->
            val pill = text(name, 14f, pal.text).apply {
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener {
                    prefs.edit().putInt(key, i).apply()
                    paint(i)
                    onChange(key)
                }
            }
            pills.add(pill)
            row.addView(pill, LinearLayout.LayoutParams(wrap, wrap).apply { rightMargin = dp(8) })
        }
        paint(prefs.getInt(key, def).coerceIn(0, options.size - 1))
        parent.addView(column().apply {
            setPadding(0, dp(8), 0, dp(2))
            addView(text(label, 15f, pal.text))
            addView(HorizontalScrollView(ctx).apply {
                isHorizontalScrollBarEnabled = false
                addView(row)
            })
        })
    }

    /** Swatches plus a custom picker (hue, saturation, brightness). Saves the colour as an int. */
    fun colorPicker(parent: LinearLayout, label: String, key: String, swatches: IntArray, def: Int) {
        val deferred = key.startsWith("t_")
        var current = prefs.getInt(key, def)
        val dots = ArrayList<View>()
        val custom = column().apply { visibility = View.GONE }
        val hsv = FloatArray(3).also { Color.colorToHSV(current, it) }
        val preview = View(ctx)

        fun dotBg(color: Int, selected: Boolean) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(if (selected) dp(3) else dp(1), if (selected) pal.text else 0x55888888)
        }

        fun refreshDots() = dots.forEachIndexed { i, v -> v.background = dotBg(swatches[i], swatches[i] == current) }

        fun save(color: Int, notify: Boolean) {
            current = color
            prefs.edit().putInt(key, color).apply()
            preview.background = dotBg(color, false)
            refreshDots()
            if (notify) onChange(key)
        }

        val row = LinearLayout(ctx).apply { setPadding(0, dp(8), 0, dp(4)) }
        swatches.forEach { color ->
            val dot = View(ctx).apply {
                setOnClickListener {
                    Color.colorToHSV(color, hsv)
                    save(color, true)
                }
            }
            dots.add(dot)
            row.addView(dot, LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(10) })
        }
        refreshDots()

        fun hsvBar(name: String, max: Int, index: Int, scale: Float) {
            val bar = SeekBar(ctx).apply {
                this.max = max
                progress = (hsv[index] * scale).toInt().coerceIn(0, max)
                progressTintList = ColorStateList.valueOf(pal.accent)
                thumbTintList = ColorStateList.valueOf(pal.accent)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, v: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        hsv[index] = v / scale
                        save(Color.HSVToColor(hsv), !deferred)
                    }

                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {
                        if (deferred) onChange(key)
                    }
                })
            }
            custom.addView(text(name, 12f, pal.muted))
            custom.addView(bar)
        }
        hsvBar("Hue", 360, 0, 1f)
        hsvBar("Saturation", 100, 1, 100f)
        hsvBar("Brightness", 100, 2, 100f)

        val toggleCustom = text("Custom colour", 14f, pal.accent, true).apply {
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener {
                custom.visibility = if (custom.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
        preview.background = dotBg(current, false)
        val head = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text(label, 15f, pal.text), LinearLayout.LayoutParams(0, wrap, 1f))
            addView(preview, LinearLayout.LayoutParams(dp(22), dp(22)))
        }
        parent.addView(column().apply {
            setPadding(0, dp(8), 0, dp(2))
            addView(head)
            addView(HorizontalScrollView(ctx).apply {
                isHorizontalScrollBarEnabled = false
                addView(row)
            })
            addView(toggleCustom)
            addView(custom)
        })
    }
}
