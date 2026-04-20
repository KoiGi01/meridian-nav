package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class IdleOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val wordmark: TextView
    val searchButton: TextView

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        val density = resources.displayMetrics.density

        wordmark = TextView(context).apply {
            text = "MERIDIAN"
            setTextColor(Color.WHITE)
            textSize = 18f
            letterSpacing = 0.4f
            alpha = 0.45f
            typeface = mono
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins((24 * density).toInt(), (20 * density).toInt(), 0, 0)
            }
        }
        addView(wordmark)

        searchButton = TextView(context).apply {
            text = "◢ SET DESTINATION"
            setTextColor(Color.WHITE)
            textSize = 12f
            letterSpacing = 0.3f
            typeface = mono
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000"))
                setStroke((1 * density).toInt(), Color.parseColor("#6b0919"))
                cornerRadius = 0f
            }
            setPadding(
                (20 * density).toInt(), (12 * density).toInt(),
                (20 * density).toInt(), (12 * density).toInt()
            )
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (32 * density).toInt()
            }
        }
        addView(searchButton)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false
}
