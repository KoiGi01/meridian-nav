package com.lidar.nav.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class TurnInstructionOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val primaryCard: LinearLayout
    private val turnArrowView: TurnArrowView
    private val streetNameView: TextView
    private val distanceView: TextView

    private val secondaryCard: LinearLayout
    private val secondaryText: TextView

    var onTurnExecuted: (() -> Unit)? = null
    private var currentDistanceM: Float = Float.MAX_VALUE
    private var isVisible = false
    private var isPulsed = false

    init {
        visibility = View.GONE
        translationY = -dipToPx(200).toFloat()

        secondaryCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#80000000"))
            alpha = 0f
            setPadding(dipToPx(12), dipToPx(8), dipToPx(12), dipToPx(8))
        }
        addView(secondaryCard, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(40)).apply {
            topMargin = dipToPx(6)
            gravity = Gravity.TOP
        })

        secondaryText = TextView(context).apply {
            setTextColor(Color.parseColor("#66FFFFFF"))
            textSize = 10f
            typeface = monoTypeface
        }
        secondaryCard.addView(secondaryText)

        primaryCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E6000000"))
            setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(12))
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(primaryCard, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(80)))

        turnArrowView = TurnArrowView(context).also {
            primaryCard.addView(it, LinearLayout.LayoutParams(dipToPx(48), dipToPx(48)).apply {
                rightMargin = dipToPx(16)
            })
        }

        val textBlock = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        primaryCard.addView(textBlock, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        streetNameView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = monoTypeface
        }.also { textBlock.addView(it) }

        distanceView = TextView(context).apply {
            setTextColor(Color.parseColor("#CC6b0919"))
            textSize = 20f
            typeface = monoTypeface
        }.also { textBlock.addView(it) }
    }

    fun show(streetName: String, maneuverType: String, distanceM: Float, secondaryInstruction: String = "") {
        if (isVisible) return
        isVisible = true
        isPulsed = false
        currentDistanceM = distanceM

        streetNameView.text = streetName.uppercase()
        distanceView.text = formatDistance(distanceM)
        turnArrowView.setManeuver(maneuverType)

        if (secondaryInstruction.isNotBlank()) {
            secondaryText.text = "THEN: ${secondaryInstruction.uppercase()}"
            secondaryCard.alpha = 0.4f
        }

        visibility = View.VISIBLE
        translationY = -dipToPx(200).toFloat()
        animate()
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun updateDistance(distanceM: Float) {
        if (!isVisible) return
        currentDistanceM = distanceM
        distanceView.text = formatDistance(distanceM)

        if (distanceM <= 50f && !isPulsed) {
            isPulsed = true
            triggerWineRedPulse()
        }
    }

    private fun triggerWineRedPulse() {
        val overlay = View(context).apply {
            setBackgroundColor(Color.parseColor("#6b0919"))
            alpha = 0f
        }
        primaryCard.addView(overlay, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(overlay, "alpha", 0f, 0.6f).apply { duration = 200 },
                ObjectAnimator.ofFloat(overlay, "alpha", 0.6f, 0f).apply { duration = 300 }
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    primaryCard.removeView(overlay)
                    dismissWithWipe()
                }
            })
            start()
        }
    }

    private fun dismissWithWipe() {
        ObjectAnimator.ofFloat(this, "translationX", 0f, width.toFloat()).apply {
            duration = 350
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    translationX = 0f
                    isVisible = false
                    isPulsed = false
                    onTurnExecuted?.invoke()
                }
            })
            start()
        }
    }

    private fun formatDistance(distanceM: Float): String =
        if (distanceM >= 1000f) "${"%.1f".format(distanceM / 1000f)} KM"
        else "${distanceM.toInt()} M"

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}
