package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

/**
 * Tactical speed readout. Not a bubble — a hairline-framed rectangular slab
 * with a left tick-column indicator and right-aligned large numerals.
 *
 * Displays:
 *   top-tag:  "LIMIT nn ──── VEL · KMH"
 *   body:     tick column + large speed + "● NOMINAL" / "● OVER"
 *
 * Over the limit → numerals switch to amber, status dot + label to amber.
 */
class SpeedBubble @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }
    private val monoBold: Typeface? by lazy { Typeface.create(mono, Typeface.BOLD) }

    private val fg       = Color.parseColor("#E8EDF0")
    private val fgDim    = Color.parseColor("#8CE8EDF0")
    private val fgGhost  = Color.parseColor("#52E8EDF0")
    private val hairHot  = Color.parseColor("#8CE8EDF0")
    private val hairDim  = Color.parseColor("#1AE8EDF0")
    private val wineLine = Color.parseColor("#8A1226")
    private val amber    = Color.parseColor("#FFB300")
    private val bgPanel  = Color.parseColor("#C7000000")

    private val speedText: TextView
    private val statusText: TextView
    private val statusDot: View
    private val limitTag: TextView
    private val tickColumn: LinearLayout
    private val tickViews: List<View>
    private var limit: Int = 80

    init {
        orientation = VERTICAL
        gravity = Gravity.END
        setBackgroundColor(Color.TRANSPARENT)

        // Top tag strip
        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        limitTag = tag("LIMIT $limit", fgGhost)
        topRow.addView(limitTag)
        topRow.addView(View(context).apply { setBackgroundColor(hairDim) },
            LayoutParams((24 * d).toInt(), (1 * d).toInt().coerceAtLeast(1)).apply {
                leftMargin = (10 * d).toInt(); rightMargin = (10 * d).toInt()
            })
        topRow.addView(tag("VEL · KMH", fgDim))
        addView(topRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = (4 * d).toInt()
        })

        // Main panel
        val panel = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = FramedPanelDrawable(bgPanel, hairHot, hairHot, d)
            setPadding((16 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
            minimumWidth = (150 * d).toInt()
        }

        // Left tick column
        tickColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.START
        }
        tickViews = (0..4).map {
            View(context).apply {
                setBackgroundColor(hairDim)
                layoutParams = LayoutParams((6 * d).toInt(), (1 * d).toInt().coerceAtLeast(1)).apply {
                    bottomMargin = (6 * d).toInt()
                }
            }.also { tickColumn.addView(it) }
        }
        panel.addView(tickColumn, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = (14 * d).toInt() })

        // Right numbers column
        val right = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.END
        }
        speedText = TextView(context).apply {
            text = "--"
            setTextColor(fg)
            textSize = 52f
            typeface = monoBold
            letterSpacing = -0.02f
            includeFontPadding = false
            gravity = Gravity.END
        }
        right.addView(speedText)

        val statusRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        statusDot = View(context).apply { setBackgroundColor(wineLine) }
        statusRow.addView(statusDot, LayoutParams((5 * d).toInt(), (5 * d).toInt()).apply {
            rightMargin = (8 * d).toInt()
        })
        statusText = tag("NOMINAL", fgDim)
        statusRow.addView(statusText)
        right.addView(statusRow, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (2 * d).toInt() })

        panel.addView(right, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ))

        addView(panel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun setLimit(kmh: Int) {
        limit = kmh
        limitTag.text = "LIMIT $kmh"
    }

    fun update(kmh: Int?) {
        if (kmh == null) {
            speedText.text = "--"
            speedText.setTextColor(fg)
            statusDot.setBackgroundColor(wineLine)
            statusText.setTextColor(fgDim)
            statusText.text = "NOMINAL"
            tickViews.forEachIndexed { i, v ->
                v.setBackgroundColor(if (i == 1) fg else hairDim)
            }
            return
        }
        val over = kmh > limit
        val color = if (over) amber else fg
        speedText.text = kmh.toString().padStart(2, '0')
        speedText.setTextColor(color)
        statusDot.setBackgroundColor(if (over) amber else wineLine)
        statusText.setTextColor(if (over) amber else fgDim)
        statusText.text = if (over) "OVER" else "NOMINAL"
        val activeIdx = if (over) 0 else 1
        tickViews.forEachIndexed { i, v ->
            v.setBackgroundColor(if (i == activeIdx) color else hairDim)
        }
    }

    private fun tag(text: String, color: Int, size: Float = 9f): TextView = TextView(context).apply {
        this.text = text
        setTextColor(color)
        textSize = size
        typeface = mono
        letterSpacing = 0.18f
        includeFontPadding = false
    }
}
