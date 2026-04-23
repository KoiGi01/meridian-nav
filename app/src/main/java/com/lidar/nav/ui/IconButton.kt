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

        background = CyberBracketDrawable(
            legLengthPx = 10 * density,
            strokeWidthPx = 1.5f * density,
            strokeColor = Color.parseColor("#00E5FF"),
            fillColor = Color.parseColor("#99000A14")
        )

        icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageTintList = ColorStateList.valueOf(Color.parseColor("#00E5FF"))
        }
        val iconSize = (28 * density).toInt()
        addView(icon, LayoutParams(iconSize, iconSize))

        label = TextView(context).apply {
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 9f
            typeface = mono
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
            visibility = android.view.View.GONE
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = (2 * density).toInt()
        })
    }

    fun setIconResource(resId: Int) = icon.setImageResource(resId)
    fun setLabel(text: String) {
        label.text = text
        label.visibility = if (text.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
