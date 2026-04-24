package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class TurnCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val arrow: TurnArrowView
    private val distanceLabel: TextView
    private val streetLabel: TextView

    private var isCardVisible = false
    private var currentDistanceM = Float.MAX_VALUE

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        visibility = View.GONE
        alpha = 0f

        val pad = (16 * d).toInt()
        setPadding(pad, (12 * d).toInt(), (20 * d).toInt(), (12 * d).toInt())

        background = GradientDrawable().apply {
            setColor(Color.parseColor("#E8101418"))
            cornerRadius = 14 * d
            setStroke((1 * d).toInt(), Color.parseColor("#28FFFFFF"))
        }
        elevation = 8 * d

        arrow = TurnArrowView(context)
        addView(arrow, LayoutParams(
            (40 * d).toInt(), (40 * d).toInt()
        ).apply { rightMargin = (14 * d).toInt() })

        val textBlock = LinearLayout(context).apply { orientation = VERTICAL }

        distanceLabel = TextView(context).apply {
            text = "--"
            setTextColor(Color.parseColor("#00D4FF"))
            textSize = 22f
            typeface = Typeface.create(mono, Typeface.BOLD)
            includeFontPadding = false
        }
        textBlock.addView(distanceLabel)

        streetLabel = TextView(context).apply {
            text = ""
            setTextColor(Color.parseColor("#CCE8EDF0"))
            textSize = 13f
            typeface = mono
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textBlock.addView(streetLabel)

        addView(textBlock)
    }

    fun showManeuver(street: String, maneuverType: String, distanceM: Float) {
        arrow.setManeuver(maneuverType)
        streetLabel.text = street
        distanceLabel.text = formatDistance(distanceM)
        currentDistanceM = distanceM

        // Tint arrow amber when close
        arrow.setUrgent(distanceM <= 500f)

        if (!isCardVisible) {
            isCardVisible = true
            visibility = View.VISIBLE
            translationX = -(width.toFloat().coerceAtLeast(300f))
            animate()
                .translationX(0f).alpha(1f)
                .setDuration(350).setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun updateDistance(distanceM: Float) {
        if (!isCardVisible) return
        currentDistanceM = distanceM
        distanceLabel.text = formatDistance(distanceM)
        arrow.setUrgent(distanceM <= 500f)
        distanceLabel.setTextColor(
            if (distanceM <= 500f) Color.parseColor("#FFB800")
            else Color.parseColor("#00D4FF")
        )
    }

    fun dismiss() {
        if (!isCardVisible) return
        isCardVisible = false
        animate()
            .translationX(-(width.toFloat().coerceAtLeast(300f))).alpha(0f)
            .setDuration(280)
            .withEndAction { visibility = View.GONE; translationX = 0f }
            .start()
    }

    fun reset() {
        isCardVisible = false
        visibility = View.GONE
        alpha = 0f
        translationX = 0f
    }

    private fun formatDistance(m: Float): String {
        val miles = m / 1609.344f
        return if (miles >= 0.1f) "%.1f mi".format(miles) else "${(m * 3.28084f).toInt()} ft"
    }
}
