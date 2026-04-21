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
    val speedPair: SpeedPairView
    val progressPill: TripProgressPillView
    val searchFab: IconButton
    private val rightStack: LinearLayout

    val recenterButton: IconButton
    val zoomInButton: IconButton
    val zoomOutButton: IconButton
    val voiceButton: IconButton
    val settingsButton: IconButton

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
        val btnSize = (60 * density).toInt()

        turnCard = TurnInstructionOverlay(context).also {
            addView(it, LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = margin
                topMargin = margin
            })
        }

        speedPair = SpeedPairView(context).also {
            addView(it, LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = margin
            })
        }

        rightStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        addView(rightStack, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            rightMargin = margin
        })

        recenterButton = iconBtn(R.drawable.ic_recenter, "RCTR") { onRecenter?.invoke() }
        zoomInButton = iconBtn(R.drawable.ic_zoom_in, "ZM+") { onZoomIn?.invoke() }
        zoomOutButton = iconBtn(R.drawable.ic_zoom_out, "ZM-") { onZoomOut?.invoke() }
        voiceButton = iconBtn(R.drawable.ic_voice, "VOX") { onVoice?.invoke() }
        settingsButton = iconBtn(R.drawable.ic_settings, "CFG") { onSettings?.invoke() }

        listOf(recenterButton, zoomInButton, zoomOutButton, voiceButton, settingsButton)
            .forEach { btn ->
                rightStack.addView(btn, LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    bottomMargin = (8 * density).toInt()
                })
            }

        searchFab = IconButton(context).apply {
            setIconResource(R.drawable.ic_search)
            setLabel("SRCH")
            setOnClickListener { onSearch?.invoke() }
        }
        addView(searchFab, LayoutParams(btnSize, btnSize).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            leftMargin = margin
            bottomMargin = margin
        })

        progressPill = TripProgressPillView(context).apply {
            onCancel = { onCancelRoute?.invoke() }
        }
        addView(progressPill, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = margin
            width = (480 * density).toInt()
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
        speedPair.update(speedMph, speedLimitMph)
    }

    fun show() {
        animate().alpha(1f).setDuration(400).start()
        visibility = View.VISIBLE
    }

    fun hide() {
        animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start()
    }
}
