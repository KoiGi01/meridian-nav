package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class IdleOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val wordmark: TextView

    init {
        setBackgroundColor(Color.TRANSPARENT)

        wordmark = TextView(context).apply {
            text = "LIDAR"
            setTextColor(Color.WHITE)
            textSize = 18f
            letterSpacing = 0.4f
            alpha = 0.45f
            typeface = try {
                ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular)
            } catch (e: Exception) {
                Typeface.MONOSPACE
            }
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                val density = resources.displayMetrics.density
                setMargins((24 * density).toInt(), (20 * density).toInt(), 0, 0)
            }
            layoutParams = params
        }
        addView(wordmark)
    }
}
