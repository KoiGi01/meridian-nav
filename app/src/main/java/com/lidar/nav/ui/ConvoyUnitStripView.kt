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
 * Horizontal convoy unit card strip.
 *
 * Shows one framed slab per unit. Each unit's border/dot uses its assigned color.
 * Self (UNIT·01) shows "● YOU" instead of a distance. A MI/KM toggle sits at the far right.
 *
 * All values are hardcoded stubs for UI mockup — no live data.
 */
class ConvoyUnitStripView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density
    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }
    private val monoBold: Typeface? by lazy { Typeface.create(mono, Typeface.BOLD) }

    private val fg     = Color.parseColor("#E8EDF0")
    private val fgDim  = Color.parseColor("#8CE8EDF0")
    private val bgPanel = Color.parseColor("#C7000000")

    val unitColors = listOf(
        Color.parseColor("#8A1226"), // UNIT·01 — wine red (self)
        Color.parseColor("#4A9EFF"), // UNIT·02 — cobalt blue
        Color.parseColor("#FFB300"), // UNIT·03 — amber
        Color.parseColor("#A855F7"), // UNIT·04 — purple
        Color.parseColor("#00D4AA"), // UNIT·05 — teal
    )

    private data class StubUnit(val callsign: String, val distMi: Float, val isSelf: Boolean)

    private val stubUnits = listOf(
        StubUnit("UNIT·01", 0f, true),
        StubUnit("UNIT·02", 1.4f, false),
        StubUnit("UNIT·03", 2.1f, false),
    )

    private var useMetric = false
    private val distLabels = mutableListOf<TextView>()
    private val stubMiles = listOf(1.4f, 2.1f)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        stubUnits.forEachIndexed { i, unit ->
            addView(buildCard(i, unit))
        }

        addView(View(context), LayoutParams(0, 1, 1f))
        addView(buildToggle())
    }

    private fun buildCard(index: Int, unit: StubUnit): LinearLayout {
        val color = unitColors[index]
        val card = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = FramedPanelDrawable(bgPanel, color, color, d)
            setPadding((12 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
        }

        // Colored dot
        card.addView(View(context).apply {
            setBackgroundColor(color)
        }, LayoutParams((8 * d).toInt(), (8 * d).toInt()).apply {
            rightMargin = (9 * d).toInt()
        })

        // Callsign
        card.addView(TextView(context).apply {
            text = unit.callsign
            setTextColor(fg)
            textSize = 9f
            typeface = mono
            letterSpacing = 0.12f
            includeFontPadding = false
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            rightMargin = (10 * d).toInt()
        })

        // Distance / YOU label
        val distLabel = TextView(context).apply {
            text = if (unit.isSelf) "● YOU" else formatDist(unit.distMi)
            setTextColor(if (unit.isSelf) color else fgDim)
            textSize = 10f
            typeface = monoBold
            includeFontPadding = false
        }
        if (!unit.isSelf) distLabels.add(distLabel)
        card.addView(distLabel)

        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            addView(card, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                rightMargin = (10 * d).toInt()
            }
        }
    }

    private fun buildToggle(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val miLabel = tag("MI", fg)
        val sep     = tag("·", fgDim)
        val kmLabel = tag("KM", fgDim)

        row.addView(miLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            rightMargin = (5 * d).toInt()
        })
        row.addView(sep, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            rightMargin = (5 * d).toInt()
        })
        row.addView(kmLabel)

        var miActive = true
        fun toggle() {
            miActive = !miActive
            useMetric = !miActive
            miLabel.setTextColor(if (miActive) fg else fgDim)
            kmLabel.setTextColor(if (!miActive) fg else fgDim)
            distLabels.forEachIndexed { i, tv ->
                tv.text = formatDist(stubMiles.getOrElse(i) { 0f })
            }
        }

        miLabel.isClickable = true; miLabel.isFocusable = true
        kmLabel.isClickable = true; kmLabel.isFocusable = true
        miLabel.setOnClickListener { toggle() }
        kmLabel.setOnClickListener { toggle() }
        return row
    }

    private fun formatDist(miles: Float): String =
        if (useMetric) "%.1f km".format(miles * 1.609f)
        else "%.1f mi".format(miles)

    private fun tag(text: String, color: Int): TextView = TextView(context).apply {
        this.text = text
        setTextColor(color)
        textSize = 9f
        typeface = mono
        letterSpacing = 0.18f
        includeFontPadding = false
    }
}
