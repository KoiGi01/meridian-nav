package com.lidar.nav.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

/**
 * Right-edge vertical stack of square tactical map-control buttons.
 *
 * Each button: framed panel (hairline border + 4 L-brackets), 52dp square,
 * monochrome icon in phosphor white. A short 3-letter tag (ZM+, ZM-, CTR)
 * sits to the left of each button.
 */
class MapControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val fg      = Color.parseColor("#E8EDF0")
    private val fgGhost = Color.parseColor("#52E8EDF0")
    private val hairHot = Color.parseColor("#8CE8EDF0")
    private val bgPanel = Color.parseColor("#C7000000")

    var onRecenter: (() -> Unit)? = null
    var onZoomIn: (() -> Unit)? = null
    var onZoomOut: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.END
        addView(row("ZM+", R.drawable.ic_zoom_in) { onZoomIn?.invoke() })
        addView(row("ZM-", R.drawable.ic_zoom_out) { onZoomOut?.invoke() })
        addView(row("CTR", R.drawable.ic_recenter) { onRecenter?.invoke() })
    }

    private fun row(label: String, iconRes: Int, onClick: () -> Unit): LinearLayout {
        val size = (52 * d).toInt()
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        container.addView(TextView(context).apply {
            text = label
            setTextColor(fgGhost)
            textSize = 9f
            typeface = mono
            letterSpacing = 0.18f
            includeFontPadding = false
            gravity = Gravity.END
        }, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = (10 * d).toInt() })

        val btn = FrameLayout(context).apply {
            background = FramedPanelDrawable(bgPanel, hairHot, hairHot, d)
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        btn.addView(ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(fg)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }, FrameLayout.LayoutParams((22 * d).toInt(), (22 * d).toInt()).apply {
            gravity = Gravity.CENTER
        })
        container.addView(btn, LayoutParams(size, size).apply {
            bottomMargin = (10 * d).toInt()
        })
        return container
    }
}
