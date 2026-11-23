package com.customhub

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Redraws an app's icon inside a shape of your choice, at a size and colour of your choice. */
object IconRenderer {

    val shapes = listOf("Circle", "Squircle", "Rounded", "Square", "Teardrop", "Hexagon")

    fun shapePath(shape: Int, size: Float): Path {
        val path = Path()
        val box = RectF(0f, 0f, size, size)
        when (shape) {
            0 -> path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
            1 -> {
                // A superellipse: rounder than a rounded square, squarer than a circle.
                val r = size / 2.0
                val steps = 120
                for (i in 0..steps) {
                    val t = i * 2.0 * PI / steps
                    val c = cos(t)
                    val s = sin(t)
                    val x = (r + r * sign(c) * abs(c).pow(0.5)).toFloat()
                    val y = (r + r * sign(s) * abs(s).pow(0.5)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            2 -> path.addRoundRect(box, size * 0.22f, size * 0.22f, Path.Direction.CW)
            3 -> path.addRoundRect(box, size * 0.04f, size * 0.04f, Path.Direction.CW)
            4 -> {
                val half = size / 2f
                val small = size * 0.08f
                path.addRoundRect(
                    box,
                    floatArrayOf(small, small, half, half, half, half, half, half),
                    Path.Direction.CW
                )
            }
            else -> {
                val r = size / 2f
                for (k in 0 until 6) {
                    val a = Math.toRadians(-90.0 + 60.0 * k)
                    val x = (r + r * cos(a)).toFloat()
                    val y = (r + r * sin(a)).toFloat()
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
        }
        return path
    }

    /**
     * @param sizePct how much of the shape the icon fills (100 = the icon's visible area fills it)
     * @param bgMode 0 theme accent, 1 custom colour, 2 keep the app's own background
     */
    fun render(
        icon: Drawable, shape: Int, sizePct: Int,
        bgMode: Int, bgColor: Int, accent: Int, border: Boolean,
        px: Int = 288
    ): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val size = px.toFloat()
        val path = shapePath(shape, size)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val fill = when (bgMode) {
            0 -> accent
            1 -> bgColor
            else -> null
        }
        if (fill != null) {
            paint.style = Paint.Style.FILL
            paint.color = fill
            canvas.drawPath(path, paint)
        }

        canvas.save()
        canvas.clipPath(path)
        val inner = size * sizePct / 100f
        if (icon is AdaptiveIconDrawable) {
            // The visible window of an adaptive icon is two thirds of the whole drawable.
            val side = inner * 1.5f
            val left = ((size - side) / 2f).toInt()
            val top = left
            val right = (left + side).toInt()
            val bottom = (top + side).toInt()
            if (bgMode == 2) {
                icon.background?.let {
                    it.setBounds(left, top, right, bottom)
                    it.draw(canvas)
                }
            }
            icon.foreground?.let {
                it.setBounds(left, top, right, bottom)
                it.draw(canvas)
            }
        } else {
            val left = ((size - inner) / 2f).toInt()
            val right = (left + inner).toInt()
            icon.setBounds(left, left, right, right)
            icon.draw(canvas)
        }
        canvas.restore()

        if (border) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.025f
            paint.color = 0x66FFFFFF
            canvas.drawPath(path, paint)
        }
        return bmp
    }
}
