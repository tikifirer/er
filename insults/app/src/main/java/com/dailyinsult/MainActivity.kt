package com.dailyinsult

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var dayLabel: TextView
    private lateinit var insultText: TextView
    private var shownDate: LocalDate = LocalDate.now()
    private var currentText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dayLabel = TextView(this).apply {
            textSize = 13f
            letterSpacing = 0.14f
            setTextColor(0xFFFFB4AB.toInt())
            gravity = Gravity.CENTER
        }
        insultText = TextView(this).apply {
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            setLineSpacing(0f, 1.2f)
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28.dp(), 36.dp(), 28.dp(), 36.dp())
            background = GradientDrawable().apply {
                setColor(0xFF2B1210.toInt())
                cornerRadius = 28.dp().toFloat()
            }
            addView(dayLabel)
            addView(insultText, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20.dp() })
        }

        val share = Button(this).apply {
            text = "Share it"
            isAllCaps = false
            setOnClickListener { share() }
        }
        val another = Button(this).apply {
            text = "Give me another"
            isAllCaps = false
            setOnClickListener { showRandom() }
        }
        val today = Button(this).apply {
            text = "Back to today's"
            isAllCaps = false
            setOnClickListener { showToday() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF120808.toInt())
            setPadding(20.dp(), 40.dp(), 20.dp(), 40.dp())
            addView(card)
            val buttons = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16.dp() }
            addView(share, buttons)
            addView(another, LinearLayout.LayoutParams(buttons))
            addView(today, LinearLayout.LayoutParams(buttons))
        }
        setContentView(root)
        showToday()
    }

    override fun onResume() {
        super.onResume()
        // If the app was left open past midnight, move on to the new day's insult.
        if (shownDate != LocalDate.now()) showToday()
    }

    private fun showToday() {
        shownDate = LocalDate.now()
        val (index, text) = Insults.today(this)
        dayLabel.text = "TODAY'S INSULT  ·  ${index + 1} OF 365"
        show(text)
    }

    private fun showRandom() {
        val list = Insults.all(this)
        val index = Random.nextInt(list.size)
        dayLabel.text = "A RANDOM ONE  ·  ${index + 1} OF 365"
        show(list[index])
    }

    private fun show(text: String) {
        currentText = text
        insultText.text = text
    }

    private fun share() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentText)
        }
        startActivity(Intent.createChooser(send, "Share this insult"))
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}
