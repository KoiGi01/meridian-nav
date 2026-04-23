package com.lidar.nav.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
        val size = (40 * d).toInt()
        return ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#CCFFFFFF"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            isClickable = true
            isFocusable = true
            setBackgroundColor(Color.parseColor("#66000000"))
            setOnClickListener { onClick() }
            layoutParams = LayoutParams(size, size).apply {
                bottomMargin = (8 * d).toInt()
            }
        }
    }
}
