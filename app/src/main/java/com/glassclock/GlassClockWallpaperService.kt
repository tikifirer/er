package com.glassclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.Typeface
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlassClockWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = GlassEngine()

    inner class GlassEngine : Engine() {

        private lateinit var prefs: SharedPreferences
        private var width = 0
        private var height = 0
        private var visible = false

        private var bgBitmap: Bitmap? = null
        private var maskBitmap: Bitmap? = null
        private var maskCanvas: Canvas? = null
        private val shader = RuntimeShader(GLASS_AGSL)
        private val paint = Paint()
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        private val prefListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == Prefs.BG_VERSION) bgBitmap = null
                draw()
            }

        private val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = draw()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs = Prefs.get(this@GlassClockWallpaperService)
            prefs.registerOnSharedPreferenceChangeListener(prefListener)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            registerReceiver(timeReceiver, filter)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
            try {
                unregisterReceiver(timeReceiver)
            } catch (_: IllegalArgumentException) {
            }
            super.onDestroy()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            if (w != width || h != height) {
                width = w
                height = h
                bgBitmap = null
                maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                maskCanvas = Canvas(maskBitmap!!)
            }
            draw()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) draw()
        }

        private fun draw() {
            if (!visible || width == 0 || height == 0) return
            val mask = maskBitmap ?: return
            val mc = maskCanvas ?: return

            val bg = bgBitmap ?: buildBackground().also { bgBitmap = it }
            drawClockMask(mc, mask)

            val tint = Prefs.TINTS[Prefs.tintColor(prefs)]
            shader.setInputShader(
                "bg", BitmapShader(bg, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            )
            shader.setInputShader(
                "mask", BitmapShader(mask, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            )
            shader.setFloatUniform("glass", if (Prefs.glass(prefs)) 1f else 0f)
            shader.setFloatUniform("blurR", Prefs.softness(prefs).toFloat())
            shader.setFloatUniform("refraction", Prefs.refraction(prefs).toFloat())
            shader.setFloatUniform(
                "tint",
                Color.red(tint) / 255f,
                Color.green(tint) / 255f,
                Color.blue(tint) / 255f,
                Prefs.tintAmount(prefs) / 100f
            )
            paint.shader = shader

            val holder = surfaceHolder
            try {
                val canvas = holder.lockHardwareCanvas() ?: return
                try {
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            } catch (_: Exception) {
                // Surface went away mid-draw; the next visibility change redraws.
            }
        }

        /** Draws the time (and date) in white on a transparent bitmap. The alpha is the glass shape. */
        private fun drawClockMask(canvas: Canvas, bitmap: Bitmap) {
            bitmap.eraseColor(Color.TRANSPARENT)

            val now = Date()
            val h24 = Prefs.hour24(this@GlassClockWallpaperService, prefs)
            val timeText = SimpleDateFormat(if (h24) "HH:mm" else "h:mm", Locale.getDefault())
                .format(now)

            val weight = Prefs.weight(prefs)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, weight, false)
            textPaint.textAlign = Paint.Align.CENTER

            // Scale the text so the time spans the chosen share of the screen width.
            textPaint.textSize = 100f
            val measured = textPaint.measureText(timeText)
            val target = width * Prefs.size(prefs) / 100f
            val timeSize = 100f * target / measured
            textPaint.textSize = timeSize

            val centerX = width / 2f
            val baseline = height * Prefs.position(prefs) / 100f + timeSize * 0.36f
            canvas.drawText(timeText, centerX, baseline, textPaint)

            if (Prefs.showDate(prefs)) {
                val dateText = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)
                textPaint.textSize = timeSize * 0.2f
                canvas.drawText(dateText, centerX, baseline + timeSize * 0.3f, textPaint)
            }
        }

        private fun buildBackground(): Bitmap {
            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)

            val custom = File(filesDir, "bg.jpg")
            val photo = if (custom.exists()) BitmapFactory.decodeFile(custom.path) else null
            if (photo != null) {
                val scale = maxOf(width / photo.width.toFloat(), height / photo.height.toFloat())
                val m = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(
                        (width - photo.width * scale) / 2f,
                        (height - photo.height * scale) / 2f
                    )
                }
                c.drawBitmap(photo, m, Paint(Paint.FILTER_BITMAP_FLAG))
                photo.recycle()
                return out
            }

            // Default backdrop: deep gradient with colourful glows, so the glass has something to bend.
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF1B0F3F.toInt(), 0xFF24185E.toInt(), 0xFF0B2A4A.toInt()),
                null, Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)

            fun glow(cx: Float, cy: Float, r: Float, color: Int) {
                p.shader = RadialGradient(
                    cx, cy, r, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP
                )
                c.drawCircle(cx, cy, r, p)
            }
            glow(width * 0.2f, height * 0.18f, width * 0.7f, 0xCCFF4FA3.toInt())
            glow(width * 0.85f, height * 0.38f, width * 0.65f, 0xCC38D5FF.toInt())
            glow(width * 0.3f, height * 0.75f, width * 0.8f, 0xCCFFA23A.toInt())
            return out
        }
    }

    companion object {
        /**
         * Thin liquid glass. Samples the clock-shape mask in a ring to get a soft height field,
         * bends the backdrop through the glyphs along its gradient, and adds a bright rim lit from
         * the top-left, a faint opposite rim, and a soft shadow halo outside the letters.
         */
        private const val GLASS_AGSL = """
            uniform shader bg;
            uniform shader mask;
            uniform float glass;
            uniform float blurR;
            uniform float refraction;
            uniform half4 tint;

            half4 main(float2 p) {
                half4 base = bg.eval(p);
                float a = mask.eval(p).a;

                if (glass < 0.5) {
                    half3 solid = mix(base.rgb, tint.rgb, 0.95);
                    return half4(mix(base.rgb, solid, a), 1.0);
                }

                float2 g = float2(0.0);
                float sum = a;
                for (int i = 0; i < 8; i++) {
                    float ang = float(i) * 0.785398;
                    float2 d = float2(cos(ang), sin(ang)) * blurR;
                    float s = mask.eval(p + d).a;
                    g += d * s;
                    sum += s;
                }
                g = g / (blurR * 8.0);
                float len = length(g);
                float2 n = g / (len + 0.0001);

                // Backdrop bent through the glass, with a hint of frost from three taps.
                float2 off = -g * refraction;
                half3 col = bg.eval(p + off).rgb * 0.5
                          + bg.eval(p + off * 1.3 + float2(1.5, 0.0)).rgb * 0.25
                          + bg.eval(p + off * 0.7 - float2(1.5, 0.0)).rgb * 0.25;
                col = mix(col, tint.rgb, tint.a);
                col += 0.05;

                float2 light = normalize(float2(-1.0, -1.0));
                float rim = smoothstep(0.0, 0.3, len);
                float hi = max(dot(-n, light), 0.0) * rim;
                float lo = max(dot(n, light), 0.0) * rim;
                col += half3(1.0) * (hi * 0.75 + lo * 0.25);

                half3 outc = base.rgb;
                float halo = (sum / 9.0) * (1.0 - a);
                outc *= 1.0 - 0.3 * halo;
                outc = mix(outc, col, a);
                return half4(outc, 1.0);
            }
        """
    }
}
