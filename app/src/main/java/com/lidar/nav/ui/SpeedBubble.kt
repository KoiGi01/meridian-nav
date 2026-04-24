package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class SpeedBubble @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val speedText: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER

        val size = (80 * d).toInt()
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E8101418"))
            setStroke((1 * d).toInt(), Color.parseColor("#20FFFFFF"))
        }
        elevation = 6 * d
        minimumWidth = size
        minimumHeight = size

        val padV = (8 * d).toInt()
        val padH = (12 * d).toInt()
        setPadding(padH, padV, padH, padV)

        speedText = TextView(context).apply {
            text = "--"
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.create(mono, Typeface.BOLD)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        addView(speedText)

        addView(TextView(context).apply {
            text = "KM/H"
            setTextColor(Color.parseColor("#60E8EDF0"))
            textSize = 9f
            typeface = mono
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
        })
    }

    fun update(kmh: Int?) {
        speedText.text = kmh?.toString() ?: "--"
        speedText.setTextColor(
            if (kmh != null && kmh > 0) Color.parseColor("#00D4FF") else Color.WHITE
        )
    }
}
