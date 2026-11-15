package com.dailycompliment

import android.content.Context
import java.time.LocalDate

object Compliments {
    private var cache: List<String>? = null

    fun all(context: Context): List<String> =
        cache ?: context.applicationContext.assets.open("compliments.txt")
            .bufferedReader()
            .useLines { lines -> lines.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
            .also { cache = it }

    /** Position of the given date in the list: day 1 of the year is the first compliment. */
    fun indexFor(date: LocalDate, size: Int): Int = (date.dayOfYear - 1) % size

    fun today(context: Context): Pair<Int, String> {
        val list = all(context)
        val i = indexFor(LocalDate.now(), list.size)
        return i to list[i]
    }
}
