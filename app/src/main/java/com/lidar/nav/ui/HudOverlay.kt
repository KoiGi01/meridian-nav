package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.lidar.nav.R

class HudOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val turnCard: TurnInstructionOverlay
    val topMetrics: TopBarMetricsView
    val progressPill: TripProgressPillView
    val compass: CompassView

    private val leftStack: LinearLayout

    val recenterButton: IconButton
    val zoomInButton: IconButton
    val zoomOutButton: IconButton
    val voiceButton: IconButton
    val searchButton: IconButton

    var onRecenter: (() -> Unit)? = null
    var onZoomIn: (() -> Unit)? = null
    var onZoomOut: (() -> Unit)? = null
    var onVoice: (() -> Unit)? = null
    var onSettings: (() -> Unit)? = null
    var onSearch: (() -> Unit)? = null
    var onCancelRoute: (() -> Unit)? = null

    private val density = resources.displayMetrics.density

    init {
        isMotionEventSplittingEnabled = false
        setBackgroundColor(Color.TRANSPARENT)
        val margin = (20 * density).toInt()
        val btnSize = (72 * density).toInt()

        // 1. Left Stack (vertical column of utilities)
        leftStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        addView(leftStack, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            leftMargin = margin
        })

        recenterButton = iconBtn(R.drawable.ic_recenter, "") { onRecenter?.invoke() }
        zoomInButton = iconBtn(R.drawable.ic_zoom_in, "") { onZoomIn?.invoke() }
        zoomOutButton = iconBtn(R.drawable.ic_zoom_out, "") { onZoomOut?.invoke() }
        voiceButton = iconBtn(R.drawable.ic_voice, "") { onVoice?.invoke() }
        searchButton = iconBtn(R.drawable.ic_search, "") { onSearch?.invoke() }

        listOf(recenterButton, zoomInButton, zoomOutButton, voiceButton, searchButton)
            .forEach { btn ->
                leftStack.addView(btn, LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    bottomMargin = (12 * density).toInt()
                })
            }

        // 2. Top Center (Speed / Metrics)
        topMetrics = TopBarMetricsView(context).also {
            addView(it, LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = margin
            })
        }

        // 3. Top Right (Turn Instruction)
        turnCard = TurnInstructionOverlay(context).also {
            addView(it, LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                rightMargin = margin
                topMargin = margin
            })
        }

        // 4. Bottom Center (Trip Progress Pill)
        progressPill = TripProgressPillView(context).apply {
            onCancel = { onCancelRoute?.invoke() }
        }
        addView(progressPill, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (12 * density).toInt()
            width = (600 * density).toInt()
        })

        // 5. Bottom Right (Compass)
        compass = CompassView(context)
        addView(compass, LayoutParams(
            (120 * density).toInt(), (120 * density).toInt()
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            rightMargin = (20 * density).toInt()
            bottomMargin = (20 * density).toInt()
        })
    }

    private fun iconBtn(iconRes: Int, labelText: String, onClick: () -> Unit) = IconButton(context).apply {
        setIconResource(iconRes)
        setLabel(labelText)
        setOnClickListener { onClick() }
    }

    fun update(
        distanceText: String,
        durationText: String,
        arrivalText: String,
        progressFraction: Float,
        speedMph: Int?,
        speedLimitMph: Int?
    ) {
        progressPill.update(distanceText, durationText, arrivalText, progressFraction)
        // Convert MPH to KMH approx for the HUD (mocked representation)
        topMetrics.updateSpeed((speedMph?.toFloat()?.times(1.609f))?.toInt())
    }

    fun show() {
        animate().alpha(1f).setDuration(400).start()
        visibility = View.VISIBLE
    }

    fun hide() {
        animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start()
    }
}
