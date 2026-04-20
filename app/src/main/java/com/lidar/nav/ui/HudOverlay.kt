package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class HudOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    val streetNameView: TextView
    val compassView: CompassView
    val speedView: TextView
    val speedLimitView: TextView
    val etaView: TextView
    val distanceView: TextView
    val progressBar: View

    init {
        setBackgroundColor(Color.TRANSPARENT)

        progressBar = View(context).apply {
            setBackgroundColor(Color.parseColor("#6b0919"))
            alpha = 0.9f
        }
        addView(progressBar, LayoutParams(0, dipToPx(2)))

        val topPanel = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        addView(topPanel, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(40)).apply {
            gravity = Gravity.TOP
        })

        streetNameView = makeLabel(12f, Gravity.START or Gravity.CENTER_VERTICAL).also {
            topPanel.addView(it, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                leftMargin = dipToPx(16)
            })
        }

        compassView = CompassView(context).also {
            topPanel.addView(it, FrameLayout.LayoutParams(dipToPx(40), dipToPx(40)).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                rightMargin = dipToPx(8)
            })
        }

        val bottomPanel = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        addView(bottomPanel, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(64)).apply {
            gravity = Gravity.BOTTOM
        })

        val speedBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        bottomPanel.addView(speedBlock, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = dipToPx(16)
        })

        speedView = TextView(context).apply {
            text = "0"
            setTextColor(Color.WHITE)
            textSize = 32f
            typeface = monoTypeface
        }.also { speedBlock.addView(it) }

        speedLimitView = makeLabel(10f).also { speedBlock.addView(it) }

        val etaBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        bottomPanel.addView(etaBlock, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = dipToPx(16)
        })

        etaView = makeLabel(12f).also { etaBlock.addView(it) }
        distanceView = makeLabel(10f).also { etaBlock.addView(it) }
    }

    private fun makeLabel(sizeSp: Float, gravity: Int = Gravity.START): TextView =
        TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = sizeSp
            typeface = monoTypeface
            this.gravity = gravity
            alpha = 0.9f
        }

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()

    fun updateProgress(fraction: Float) {
        post {
            val parentWidth = (parent as? FrameLayout)?.width ?: width
            progressBar.layoutParams = (progressBar.layoutParams as LayoutParams).apply {
                width = (parentWidth * fraction.coerceIn(0f, 1f)).toInt()
            }
            progressBar.requestLayout()
        }
    }

    fun update(
        streetName: String,
        speedKmh: Int,
        speedLimit: Int?,
        etaText: String,
        distanceText: String,
        progressFraction: Float,
        bearingDegrees: Float
    ) {
        streetNameView.text = streetName.uppercase()
        speedView.text = speedKmh.toString()
        speedLimitView.text = speedLimit?.let { "LIMIT $it" } ?: ""
        etaView.text = etaText
        distanceView.text = distanceText
        compassView.bearing = bearingDegrees
        updateProgress(progressFraction)
    }

    fun show() {
        animate().alpha(1f).setDuration(400).start()
        visibility = View.VISIBLE
    }

    fun hide() {
        animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start()
    }
}
