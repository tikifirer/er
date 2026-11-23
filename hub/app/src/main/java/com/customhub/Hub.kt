package com.customhub

import android.content.Context
import android.content.SharedPreferences

/** Every settings key in one place. Prefixes matter: t_ = theme, g_ = gestures, w_ = widgets, i_ = icon maker. */
object K {
    const val T_MODE = "t_mode"            // 0 system, 1 light, 2 dark, 3 amoled
    const val T_ACCENT = "t_accent"
    const val T_RADIUS = "t_radius"        // dp
    const val T_CARD = "t_card"            // 0 solid, 1 glass, 2 outline

    const val G_ENABLED = "g_enabled"
    const val G_EDGE = "g_edge"            // 0 left, 1 right
    const val G_POSITION = "g_position"    // % down the screen
    const val G_LENGTH = "g_length"        // % of screen height
    const val G_THICKNESS = "g_thickness"  // dp
    const val G_VISIBLE = "g_visible"
    const val G_OPACITY = "g_opacity"      // %
    const val G_TAP = "g_tap"
    const val G_DOUBLE = "g_double"
    const val G_LONG = "g_long"
    const val G_UP = "g_up"
    const val G_DOWN = "g_down"

    const val W_FONT = "w_font"            // 0 thin, 1 regular, 2 bold, 3 serif
    const val W_ARRANGE = "w_arrange"      // 0 inline, 1 stacked
    const val W_24H = "w_24h"
    const val W_SHOW_DATE = "w_showdate"
    const val W_DATE_FMT = "w_datefmt"     // 0 "EEE d MMM", 1 "EEEE", 2 "d MMMM yyyy"
    const val W_BG = "w_bg"
    const val W_BG_ALPHA = "w_bgalpha"     // %
    const val W_TEXT = "w_text"
    const val W_RADIUS = "w_radius"        // 0..3
    const val W_SIZE = "w_size"            // sp
    const val W_ACT1 = "w_act1"            // quick-actions widget buttons (an action id each)
    const val W_ACT2 = "w_act2"
    const val W_ACT3 = "w_act3"
    const val W_ACT4 = "w_act4"

    const val I_SHAPE = "i_shape"
    const val I_SIZE = "i_size"            // % of the shape the icon fills
    const val I_BG_MODE = "i_bgmode"       // 0 theme accent, 1 custom colour, 2 transparent
    const val I_BG = "i_bg"
    const val I_BORDER = "i_border"
}

object Hub {
    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("customhub", Context.MODE_PRIVATE)
}
