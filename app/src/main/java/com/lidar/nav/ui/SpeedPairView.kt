package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class SpeedPairView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val density = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val spdValue: TextView
    private val limValue: TextView
    private val spdColumn: LinearLayout

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = (20 * density).toInt()
        val padV = (12 * density).toInt()
        setPadding(padH, padV, padH, padV)
        
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#E6000000"))
            setStroke((2 * density).toInt(), Color.parseColor("#FF0033")) // Vivid Red glow
            cornerRadius = 0f
        }

        spdColumn = column("SPD").also { addView(it) }
        spdValue = value().also { spdColumn.addView(it) }

        addView(divider(), LayoutParams((1 * density).toInt(), (36 * density).toInt()).apply {
            leftMargin = (14 * density).toInt()
            rightMargin = (14 * density).toInt()
        })

        val limColumn = column("LIM").also { addView(it) }
        limValue = value().also { limColumn.addView(it) }
    }

    private fun column(headerText: String) = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(TextView(context).apply {
            text = headerText
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 9f
            typeface = mono
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
        })
    }

    private fun value() = TextView(context).apply {
        text = "--"
        setTextColor(Color.WHITE)
        textSize = 48f
        typeface = Typeface.create(mono, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(0, (2 * density).toInt(), 0, 0)
    }

    private fun divider() = View(context).apply {
        setBackgroundColor(Color.parseColor("#4DFFFFFF"))
    }

    fun update(speedMph: Int?, limitMph: Int?) {
        spdValue.text = speedMph?.toString() ?: "--"
        limValue.text = limitMph?.toString() ?: "--"
        val over = speedMph != null && limitMph != null && speedMph > limitMph
        spdValue.setTextColor(if (over) Color.parseColor("#FF6b0919") else Color.WHITE)
        (background as? android.graphics.drawable.GradientDrawable)?.setStroke(
            (2 * density).toInt(),
            if (over) Color.WHITE else Color.parseColor("#FF0033")
        )
    }
}
