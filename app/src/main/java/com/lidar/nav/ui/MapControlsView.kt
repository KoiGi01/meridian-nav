package com.lidar.nav.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import com.lidar.nav.R

class MapControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    var onRecenter: (() -> Unit)? = null
    var onZoomIn: (() -> Unit)? = null
    var onZoomOut: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        addView(btn(R.drawable.ic_recenter) { onRecenter?.invoke() })
        addView(btn(R.drawable.ic_zoom_in) { onZoomIn?.invoke() })
        addView(btn(R.drawable.ic_zoom_out) { onZoomOut?.invoke() })
    }

    private fun btn(iconRes: Int, onClick: () -> Unit): ImageView {
        val size = (44 * d).toInt()
        return ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#CCE8EDF0"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            isClickable = true
            isFocusable = true
            elevation = 4 * d
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC101418"))
                setStroke((1 * d).toInt(), Color.parseColor("#22FFFFFF"))
            }
            setOnClickListener { onClick() }
            layoutParams = LayoutParams(size, size).apply {
                bottomMargin = (10 * d).toInt()
            }
        }
    }
}
