package com.lidar.nav.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class IconButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val density = resources.displayMetrics.density
    private val icon: ImageView
    private val label: TextView

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true

        val bracket = BracketDrawable(
            legLengthPx = 8 * density,
            strokeWidthPx = 1.2f * density,
            strokeColor = Color.WHITE,
            fillColor = Color.parseColor("#CC000000")
        )
        background = RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#4D6b0919")),
            bracket,
            null
        )

        icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
        val iconSize = (22 * density).toInt()
        addView(icon, LayoutParams(iconSize, iconSize).apply {
            topMargin = (8 * density).toInt()
        })

        label = TextView(context).apply {
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 8f
            typeface = mono
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = (2 * density).toInt()
            bottomMargin = (6 * density).toInt()
        })
    }

    fun setIconResource(resId: Int) = icon.setImageResource(resId)
    fun setLabel(text: String) { label.text = text }
}
