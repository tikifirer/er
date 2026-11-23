package com.customhub

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/** The colours the hub's own screens use, worked out from the saved theme settings. */
data class Palette(
    val dark: Boolean,
    val bg: Int,
    val surface: Int,
    val surfaceHi: Int,
    val text: Int,
    val muted: Int,
    val accent: Int,
    val onAccent: Int,
    val radius: Int,
    val card: Int
)

class Preset(
    val name: String,
    val mode: Int,
    val accent: Int,
    val radius: Int,
    val card: Int,
    val widgetBg: Int,
    val widgetAlpha: Int,
    val widgetText: Int,
    val widgetFont: Int,
    val widgetArrange: Int
)

object Themes {
    const val DEFAULT_ACCENT = 0xFF7C6CFF.toInt()

    val presets = listOf(
        Preset("Midnight", 2, 0xFF7C6CFF.toInt(), 20, 0, 0xFF14142A.toInt(), 85, 0xFFF2F2FF.toInt(), 1, 0),
        Preset("Sakura", 1, 0xFFE8548F.toInt(), 28, 0, 0xFFFFE4EE.toInt(), 90, 0xFF5A1B36.toInt(), 0, 1),
        Preset("Ocean", 2, 0xFF2AA5FF.toInt(), 16, 1, 0xFF0B2A44.toInt(), 70, 0xFFE6F6FF.toInt(), 0, 0),
        Preset("Forest", 2, 0xFF3DDC84.toInt(), 12, 0, 0xFF10261B.toInt(), 85, 0xFFD8FFE8.toInt(), 3, 0),
        Preset("Sunset", 2, 0xFFFF7A45.toInt(), 24, 1, 0xFF3A1420.toInt(), 75, 0xFFFFE6D8.toInt(), 2, 1),
        Preset("Mono", 3, 0xFFFFFFFF.toInt(), 4, 2, 0xFF000000.toInt(), 80, 0xFFFFFFFF.toInt(), 0, 1),
        Preset("Mint glass", 1, 0xFF14B8A6.toInt(), 30, 1, 0xFFFFFFFF.toInt(), 40, 0xFF0B3B36.toInt(), 0, 0),
        Preset("Aurora", 2, 0xFFB06CFF.toInt(), 32, 1, 0xFF1A1033.toInt(), 65, 0xFFF3E8FF.toInt(), 1, 1)
    )

    val accents = intArrayOf(
        0xFF7C6CFF.toInt(), 0xFFE8548F.toInt(), 0xFFFF7A45.toInt(), 0xFFFFC53D.toInt(),
        0xFF3DDC84.toInt(), 0xFF14B8A6.toInt(), 0xFF2AA5FF.toInt(), 0xFFB06CFF.toInt(),
        0xFFFF4D4D.toInt(), 0xFFFFFFFF.toInt()
    )

    fun isLight(color: Int): Boolean {
        val l = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)
        return l / 255.0 > 0.62
    }

    fun resolve(context: Context): Palette {
        val p = Hub.prefs(context)
        val mode = p.getInt(K.T_MODE, 0)
        val systemDark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val dark = when (mode) {
            1 -> false
            2, 3 -> true
            else -> systemDark
        }
        val amoled = mode == 3
        val accent = p.getInt(K.T_ACCENT, DEFAULT_ACCENT)
        return if (dark) {
            Palette(
                true,
                if (amoled) 0xFF000000.toInt() else 0xFF0F0F14.toInt(),
                if (amoled) 0xFF0D0D10.toInt() else 0xFF1B1B24.toInt(),
                0xFF2A2A38.toInt(), 0xFFF2F2F7.toInt(), 0xFFA0A0B2.toInt(),
                accent, if (isLight(accent)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                p.getInt(K.T_RADIUS, 20), p.getInt(K.T_CARD, 0)
            )
        } else {
            Palette(
                false, 0xFFF5F5FA.toInt(), 0xFFFFFFFF.toInt(), 0xFFE9E9F2.toInt(),
                0xFF14141C.toInt(), 0xFF6A6A7C.toInt(),
                accent, if (isLight(accent)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                p.getInt(K.T_RADIUS, 20), p.getInt(K.T_CARD, 0)
            )
        }
    }

    /** Applies a preset to both the hub's look and the clock widget style in one go. */
    fun apply(context: Context, preset: Preset) {
        Hub.prefs(context).edit()
            .putInt(K.T_MODE, preset.mode)
            .putInt(K.T_ACCENT, preset.accent)
            .putInt(K.T_RADIUS, preset.radius)
            .putInt(K.T_CARD, preset.card)
            .putInt(K.W_BG, preset.widgetBg)
            .putInt(K.W_BG_ALPHA, preset.widgetAlpha)
            .putInt(K.W_TEXT, preset.widgetText)
            .putInt(K.W_FONT, preset.widgetFont)
            .putInt(K.W_ARRANGE, preset.widgetArrange)
            .apply()
    }
}
