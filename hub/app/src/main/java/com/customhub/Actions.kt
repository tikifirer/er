package com.customhub

import android.accessibilityservice.AccessibilityService

/** Things an edge gesture or a widget button can do. The id is what gets stored. */
object Actions {
    const val NONE = 0
    const val LOCK = 1
    const val NOTIFICATIONS = 2
    const val QUICK_SETTINGS = 3
    const val HOME = 4
    const val BACK = 5
    const val RECENTS = 6
    const val SCREENSHOT = 7
    const val POWER = 8

    val ids = listOf(NONE, LOCK, NOTIFICATIONS, QUICK_SETTINGS, HOME, BACK, RECENTS, SCREENSHOT, POWER)

    fun label(id: Int): String = when (id) {
        LOCK -> "Lock screen"
        NOTIFICATIONS -> "Notifications"
        QUICK_SETTINGS -> "Quick settings"
        HOME -> "Home"
        BACK -> "Back"
        RECENTS -> "Recent apps"
        SCREENSHOT -> "Screenshot"
        POWER -> "Power menu"
        else -> "Do nothing"
    }

    fun globalAction(id: Int): Int? = when (id) {
        LOCK -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
        NOTIFICATIONS -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
        QUICK_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
        HOME -> AccessibilityService.GLOBAL_ACTION_HOME
        BACK -> AccessibilityService.GLOBAL_ACTION_BACK
        RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
        SCREENSHOT -> AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
        POWER -> AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
        else -> null
    }
}
