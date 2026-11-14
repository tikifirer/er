package com.glassclock

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.get(this)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 28.dp(), 20.dp(), 40.dp())
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF121212.toInt())
            isFillViewport = true
            addView(content)
        }
        setContentView(scroll)

        title("Glass Clock")
        note("A thin liquid glass clock for your lock screen. Pick \"Lock screen\" when Android asks where to apply it.")

        button("Set as wallpaper") { openWallpaperPicker() }

        header("Style")
        toggle("Liquid glass", Prefs.GLASS, true)
        slider("Thickness (font weight)", Prefs.WEIGHT, 100, 700, 250)
        slider("Edge softness", Prefs.SOFTNESS, 2, 20, 6)
        slider("Refraction", Prefs.REFRACTION, 0, 100, 40)
        slider("Tint amount", Prefs.TINT_AMOUNT, 0, 60, 12)
        swatches()

        header("Layout")
        slider("Clock size", Prefs.SIZE, 40, 100, 78)
        slider("Vertical position", Prefs.POSITION, 10, 70, 30)
        toggle("Show date", Prefs.SHOW_DATE, true)
        toggle(
            "24-hour time", Prefs.HOUR24,
            android.text.format.DateFormat.is24HourFormat(this)
        )

        header("Background")
        note("Glass bends whatever is behind it, so a colourful photo looks best.")
        button("Choose background image") { pickImage() }
        button("Use default background") {
            File(filesDir, "bg.jpg").delete()
            bumpBackground()
        }
    }

    // ---- actions -------------------------------------------------------------------------

    private fun openWallpaperPicker() {
        try {
            val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            i.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, GlassClockWallpaperService::class.java)
            )
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun pickImage() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(i, REQ_IMAGE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != REQ_IMAGE || resultCode != RESULT_OK || uri == null) return

        Thread {
            val ok = try {
                saveBackground(uri)
                true
            } catch (e: Exception) {
                false
            }
            runOnUiThread {
                if (ok) bumpBackground()
                else Toast.makeText(this, "Couldn't load that image", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun saveBackground(uri: android.net.Uri) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2600) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IllegalStateException("decode failed")

        val orientation = contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees != 0f) {
            val m = Matrix().apply { postRotate(degrees) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }

        FileOutputStream(File(filesDir, "bg.jpg")).use {
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, it)
        }
        bmp.recycle()
    }

    /** Any change to this key makes the running wallpaper reload its background. */
    private fun bumpBackground() {
        prefs.edit().putLong(Prefs.BG_VERSION, System.currentTimeMillis()).apply()
    }

    // ---- UI helpers ----------------------------------------------------------------------

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    private fun title(text: String) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, 300, false)
        })
    }

    private fun header(text: String) {
        content.addView(TextView(this).apply {
            this.text = text.uppercase()
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(0xFF9DB7FF.toInt())
            setPadding(0, 28.dp(), 0, 6.dp())
        })
    }

    private fun note(text: String) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFFB0B0B0.toInt())
            setPadding(0, 6.dp(), 0, 10.dp())
        })
    }

    private fun button(text: String, onClick: () -> Unit) {
        content.addView(Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun toggle(label: String, key: String, default: Boolean) {
        content.addView(Switch(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean(key, default)
            setPadding(0, 10.dp(), 0, 10.dp())
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        })
    }

    private fun slider(label: String, key: String, min: Int, max: Int, default: Int) {
        val valueText = TextView(this).apply {
            setTextColor(0xFFB0B0B0.toInt())
            textSize = 14f
        }
        val labelText = TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 16f
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(labelText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(valueText)
        }
        val current = prefs.getInt(key, default).coerceIn(min, max)
        valueText.text = current.toString()

        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = current - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    valueText.text = (value + min).toString()
                    if (fromUser) prefs.edit().putInt(key, value + min).apply()
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10.dp(), 0, 4.dp())
            addView(row)
            addView(bar)
        })
    }

    private fun swatches() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12.dp(), 0, 4.dp())
        }
        val selected = Prefs.tintColor(prefs)
        val dots = mutableListOf<View>()
        Prefs.TINTS.forEachIndexed { index, color ->
            val dot = View(this).apply {
                background = swatchDrawable(color, index == selected)
                setOnClickListener {
                    prefs.edit().putInt(Prefs.TINT_COLOR, index).apply()
                    dots.forEachIndexed { i, v -> v.background = swatchDrawable(Prefs.TINTS[i], i == index) }
                }
            }
            dots.add(dot)
            row.addView(dot, LinearLayout.LayoutParams(44.dp(), 44.dp()).apply {
                rightMargin = 12.dp()
            })
        }
        content.addView(TextView(this).apply {
            text = "Tint colour"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 10.dp(), 0, 0)
        })
        content.addView(row)
    }

    private fun swatchDrawable(color: Int, selected: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(if (selected) 3.dp() else 1.dp(), if (selected) Color.WHITE else 0x55FFFFFF)
    }

    companion object {
        private const val REQ_IMAGE = 1
    }
}
