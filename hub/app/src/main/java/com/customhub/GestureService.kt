package com.customhub

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

/**
 * Draws a thin handle on one screen edge and turns taps, long presses and swipes on it into actions.
 * It only performs global actions (lock, screenshot, notifications...) and never reads the screen.
 */
class GestureService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: GestureService? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private lateinit var windows: WindowManager
    private lateinit var prefs: SharedPreferences
    private var handle: View? = null

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("g_")) main.post { rebuild() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windows = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Hub.prefs(this)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        rebuild()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(listener)
        removeHandle()
        return super.onUnbind(intent)
    }

    /** Runs an action after a short pause, so the screen it acts on isn't covered by our own window. */
    fun perform(actionId: Int, delayMs: Long = 0) {
        val global = Actions.globalAction(actionId) ?: return
        main.postDelayed({ performGlobalAction(global) }, delayMs)
    }

    private fun removeHandle() {
        handle?.let {
            try {
                windows.removeView(it)
            } catch (_: Exception) {
            }
        }
        handle = null
    }

    private fun rebuild() {
        removeHandle()
        if (!prefs.getBoolean(K.G_ENABLED, true)) return

        val metrics = resources.displayMetrics
        fun dp(v: Int) = (v * metrics.density).toInt()

        val thickness = dp(prefs.getInt(K.G_THICKNESS, 14))
        val length = (metrics.heightPixels * prefs.getInt(K.G_LENGTH, 30) / 100f).toInt()
        val onRight = prefs.getInt(K.G_EDGE, 1) == 1
        val position = prefs.getInt(K.G_POSITION, 50) / 100f

        val view = HandleView(
            if (prefs.getBoolean(K.G_VISIBLE, true)) prefs.getInt(K.G_OPACITY, 35) else 0,
            Themes.resolve(this).accent
        )
        val params = WindowManager.LayoutParams(
            thickness, length,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or (if (onRight) Gravity.END else Gravity.START)
            y = ((metrics.heightPixels - length) * position).toInt()
        }
        try {
            windows.addView(view, params)
            handle = view
        } catch (_: Exception) {
        }
    }

    private fun trigger(view: View, key: String, default: Int) {
        val id = prefs.getInt(key, default)
        if (id == Actions.NONE) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        perform(id)
    }

    private inner class HandleView(private val opacityPct: Int, tint: Int) : View(this@GestureService) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            alpha = (opacityPct * 255 / 100).coerceIn(0, 255)
        }
        private val swipeDistance = ViewConfiguration.get(this@GestureService).scaledTouchSlop * 4
        private var downX = 0f
        private var downY = 0f

        private val detector = GestureDetector(
            this@GestureService,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    trigger(this@HandleView, K.G_TAP, Actions.NONE)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    trigger(this@HandleView, K.G_DOUBLE, Actions.LOCK)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    trigger(this@HandleView, K.G_LONG, Actions.SCREENSHOT)
                }
            }
        )

        override fun onDraw(canvas: Canvas) {
            if (opacityPct <= 0) return
            val r = width / 2f
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), r, r, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            detector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    val dy = event.rawY - downY
                    val dx = event.rawX - downX
                    if (abs(dy) > swipeDistance && abs(dy) > abs(dx)) {
                        if (dy < 0) trigger(this, K.G_UP, Actions.HOME)
                        else trigger(this, K.G_DOWN, Actions.NOTIFICATIONS)
                    }
                }
            }
            return true
        }
    }
}
