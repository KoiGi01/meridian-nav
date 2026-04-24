package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class TripSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val etaValue: TextView
    private val distValue: TextView
    private val timeValue: TextView
    private val progressLine: View

    var onCancel: (() -> Unit)? = null

    init {
        visibility = View.GONE

        val sheetHeight = (72 * d).toInt()
        minimumHeight = sheetHeight

        // Rounded top corners
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#F0101418"))
            cornerRadii = floatArrayOf(
                20 * d, 20 * d,  // top-left
                20 * d, 20 * d,  // top-right
                0f, 0f,          // bottom-right
                0f, 0f           // bottom-left
            )
        }
        elevation = 12 * d

        // Thin cyan progress line at top of sheet
        progressLine = View(context).apply {
            setBackgroundColor(Color.parseColor("#00D4FF"))
        }
        addView(progressLine, LayoutParams(0, (2 * d).toInt().coerceAtLeast(2)).apply {
            gravity = Gravity.TOP or Gravity.START
        })

        // Main content row
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((24 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_VERTICAL
        })

        // Three data columns
        val etaCol = col("ETA", "--:--")
        val distCol = col("DIST", "--")
        val timeCol = col("TIME", "--")
        etaValue = etaCol.second
        distValue = distCol.second
        timeValue = timeCol.second

        row.addView(etaCol.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(dot())
        row.addView(distCol.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(dot())
        row.addView(timeCol.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Cancel button
        val cancelBtn = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1C2026"))
                setStroke((1 * d).toInt(), Color.parseColor("#30FFFFFF"))
            }
            setOnClickListener { onCancel?.invoke() }
        }
        val btnSize = (48 * d).toInt()
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#CCE8EDF0"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        cancelBtn.addView(icon, FrameLayout.LayoutParams((22 * d).toInt(), (22 * d).toInt()).apply {
            gravity = Gravity.CENTER
        })
        row.addView(cancelBtn, LinearLayout.LayoutParams(btnSize, btnSize).apply {
            leftMargin = (16 * d).toInt()
        })
    }

    fun update(distanceText: String, durationText: String, arrivalText: String, fraction: Float) {
        distValue.text = distanceText
        timeValue.text = durationText
        etaValue.text = arrivalText

        // Animate progress line width
        val parentWidth = (parent as? View)?.width?.toFloat() ?: return
        progressLine.animate()
            .translationX(0f)
            .setDuration(0).start()
        val targetW = (parentWidth * fraction.coerceIn(0f, 1f)).toInt().coerceAtLeast(0)
        progressLine.layoutParams = progressLine.layoutParams.apply { width = targetW }
        progressLine.requestLayout()
    }

    fun slideUp() {
        visibility = View.VISIBLE
        translationY = height.toFloat().coerceAtLeast(200f)
        animate().translationY(0f)
            .setDuration(400).setInterpolator(DecelerateInterpolator()).start()
    }

    fun slideDown() {
        animate().translationY(height.toFloat().coerceAtLeast(200f))
            .setDuration(300)
            .withEndAction { visibility = View.GONE }
            .start()
    }

    private fun col(label: String, initial: String): Pair<LinearLayout, TextView> {
        val c = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        c.addView(TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#55E8EDF0"))
            textSize = 9f
            typeface = mono
            letterSpacing = 0.2f
            gravity = Gravity.CENTER_HORIZONTAL
        })
        val v = TextView(context).apply {
            text = initial
            setTextColor(Color.parseColor("#E8EDF0"))
            textSize = 18f
            typeface = Typeface.create(mono, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        c.addView(v)
        return c to v
    }

    private fun dot() = View(context).apply {
        setBackgroundColor(Color.parseColor("#25E8EDF0"))
        layoutParams = LinearLayout.LayoutParams(
            (1 * d).toInt().coerceAtLeast(1),
            (28 * d).toInt()
        ).apply {
            leftMargin = (8 * d).toInt()
            rightMargin = (8 * d).toInt()
        }
    }
}
