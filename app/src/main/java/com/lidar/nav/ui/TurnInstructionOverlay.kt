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

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val card: LinearLayout
    private val turnArrow: TurnArrowView
    private val distanceView: TextView
    private val streetView: TextView
    private val headerView: TextView

    var onTurnExecuted: (() -> Unit)? = null
    private var currentDistanceM: Float = Float.MAX_VALUE
    private var isShown = false
    private var isPulsed = false

    private val density = resources.displayMetrics.density

    init {
        visibility = View.GONE
        alpha = 0f

        card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = CyberBracketDrawable(
                legLengthPx = 10 * density,
                strokeWidthPx = 1.5f * density,
                strokeColor = Color.parseColor("#00E5FF"),
                fillColor = Color.parseColor("#80001015")
            )
            setPadding(
                (18 * density).toInt(), (12 * density).toInt(),
                (22 * density).toInt(), (12 * density).toInt()
            )
        }
        addView(card, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        turnArrow = TurnArrowView(context).also {
            card.addView(it, LinearLayout.LayoutParams(
                (40 * density).toInt(), (40 * density).toInt()
            ).apply { rightMargin = (14 * density).toInt() })
        }

        val textBlock = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        card.addView(textBlock)

        headerView = TextView(context).apply {
            text = "NEXT_MANEUVER"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 9f
            typeface = mono
            letterSpacing = 0.25f
        }.also { textBlock.addView(it) }

        distanceView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.create(mono, Typeface.BOLD)
        }.also { textBlock.addView(it) }

        streetView = TextView(context).apply {
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 11f
            typeface = mono
            letterSpacing = 0.1f
        }.also { textBlock.addView(it) }
    }

    fun show(streetName: String, maneuverType: String, distanceM: Float) {
        isShown = true
        isPulsed = false
        currentDistanceM = distanceM

        streetView.text = streetName
        distanceView.text = formatDistance(distanceM)
        turnArrow.setManeuver(maneuverType)

        visibility = View.VISIBLE
        translationY = -dipToPx(40).toFloat()
        animate()
            .translationY(0f).alpha(1f)
            .setDuration(400).setInterpolator(DecelerateInterpolator()).start()
    }

    fun updateDistance(distanceM: Float) {
        if (!isShown) return
        currentDistanceM = distanceM
        distanceView.text = formatDistance(distanceM)
        if (distanceM <= 50f && !isPulsed) {
            isPulsed = true
            triggerWineRedPulse()
        }
    }

    private fun triggerWineRedPulse() {
        val flash = View(context).apply {
            setBackgroundColor(Color.parseColor("#6b0919"))
            alpha = 0f
        }
        card.addView(flash, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(flash, "alpha", 0f, 0.6f).apply { duration = 200 },
                ObjectAnimator.ofFloat(flash, "alpha", 0.6f, 0f).apply { duration = 300 }
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    card.removeView(flash)
                    dismissWithWipe()
                }
            })
            start()
        }
    }

    private fun dismissWithWipe() {
        ObjectAnimator.ofFloat(this, "translationX", 0f, -width.toFloat()).apply {
            duration = 350
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    translationX = 0f
                    alpha = 0f
                    isShown = false
                    isPulsed = false
                    onTurnExecuted?.invoke()
                }
            })
            start()
        }
    }

    private fun formatDistance(distanceM: Float): String {
        val miles = distanceM / 1609.344f
        return if (miles >= 0.1f) "%.1f mi".format(miles)
        else "${(distanceM * 3.28084f).toInt()} ft"
    }

    private fun dipToPx(dip: Int): Int = (dip * density).toInt()
}
