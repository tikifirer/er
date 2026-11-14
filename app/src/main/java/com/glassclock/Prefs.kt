package com.glassclock

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat

object Prefs {
    const val FILE = "glassclock"

    const val GLASS = "glass"
    const val WEIGHT = "weight"
    const val SOFTNESS = "softness"
    const val REFRACTION = "refraction"
    const val TINT_COLOR = "tintColor"
    const val TINT_AMOUNT = "tintAmount"
    const val SIZE = "size"
    const val POSITION = "position"
    const val HOUR24 = "hour24"
    const val SHOW_DATE = "showDate"
    const val BG_VERSION = "bgVersion"

    val TINTS = intArrayOf(
        0xFFFFFFFF.toInt(), // white
        0xFFBFE6FF.toInt(), // ice blue
        0xFFFFC2DC.toInt(), // pink
        0xFFFFD79A.toInt(), // amber
        0xFFBFFFD9.toInt()  // mint
    )

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun glass(p: SharedPreferences) = p.getBoolean(GLASS, true)
    fun weight(p: SharedPreferences) = p.getInt(WEIGHT, 250)
    fun softness(p: SharedPreferences) = p.getInt(SOFTNESS, 6)
    fun refraction(p: SharedPreferences) = p.getInt(REFRACTION, 40)
    fun tintColor(p: SharedPreferences) = p.getInt(TINT_COLOR, 0).coerceIn(0, TINTS.size - 1)
    fun tintAmount(p: SharedPreferences) = p.getInt(TINT_AMOUNT, 12)
    fun size(p: SharedPreferences) = p.getInt(SIZE, 78)
    fun position(p: SharedPreferences) = p.getInt(POSITION, 30)
    fun showDate(p: SharedPreferences) = p.getBoolean(SHOW_DATE, true)
    fun hour24(context: Context, p: SharedPreferences) =
        p.getBoolean(HOUR24, DateFormat.is24HourFormat(context))
}
