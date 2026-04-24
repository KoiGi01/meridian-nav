package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class IdleOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val searchButton: LinearLayout

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    init {
        isMotionEventSplittingEnabled = false
        setBackgroundColor(Color.TRANSPARENT)

        val d = resources.displayMetrics.density

        // Wordmark — subtle, top-left
        addView(TextView(context).apply {
            text = "MERIDIAN"
            setTextColor(Color.WHITE)
            textSize = 14f
            letterSpacing = 0.5f
            alpha = 0.3f
            typeface = mono
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = (20 * d).toInt()
            topMargin = (20 * d).toInt()
        })

        // Google Maps-style search bar — bottom center
        searchButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            elevation = 8 * d
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0101418"))
                cornerRadius = 28 * d
                setStroke((1 * d).toInt(), Color.parseColor("#20FFFFFF"))
            }
            setPadding((20 * d).toInt(), (14 * d).toInt(), (20 * d).toInt(), (14 * d).toInt())
        }

        // Search icon (magnifier drawn inline via text glyph — no drawable needed)
        searchButton.addView(TextView(context).apply {
            text = "⌕"
            setTextColor(Color.parseColor("#00D4FF"))
            textSize = 20f
            typeface = mono
            setPadding(0, 0, (12 * d).toInt(), 0)
        })

        searchButton.addView(TextView(context).apply {
            text = "Where to?"
            setTextColor(Color.parseColor("#90E8EDF0"))
            textSize = 16f
            typeface = mono
            letterSpacing = 0.05f
        })

        addView(searchButton, LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            leftMargin = (24 * d).toInt()
            rightMargin = (24 * d).toInt()
            bottomMargin = (28 * d).toInt()
        })
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false
}
