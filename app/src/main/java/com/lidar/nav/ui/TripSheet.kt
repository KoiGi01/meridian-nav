package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

/**
 * Bottom trip-strip. Full-width, flat — not a rounded sheet.
 *
 * Composition (top → bottom):
 *   Progress rail:  "ROUTE · ACTIVE"  [──────●──────]   nn%
 *   Data panel:     ETA 14:07 │ DIST 4.2 mi │ T-REM 12 min     TGT·01 EMBARCADERO  [×ABORT]
 *
 * The progress rail uses 11 tick marks (major @ 0/50/100%), a wine-red filled
 * segment up to the current fraction, a phosphor-white diamond position marker,
 * and a vertical cursor line.
 */
class TripSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

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
    private val bgPanel  = Color.parseColor("#E5000000")

    private val etaValue: TextView
    private val distValue: TextView
    private val timeValue: TextView
    private val pctLabel: TextView
    private val progressBar: ProgressRailView

    var onCancel: (() -> Unit)? = null

    init {
        visibility = View.GONE
        setPadding((28 * d).toInt(), 0, (28 * d).toInt(), (22 * d).toInt())

        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        // Progress rail row
        val railRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        railRow.addView(tag("ROUTE · ACTIVE", fgDim))
        progressBar = ProgressRailView(context, d, hairDim, wineLine, fg).also {
            railRow.addView(it, LinearLayout.LayoutParams(0, (18 * d).toInt(), 1f).apply {
                leftMargin = (16 * d).toInt(); rightMargin = (16 * d).toInt()
            })
        }
        pctLabel = tag("0%", fgGhost)
        railRow.addView(pctLabel)
        column.addView(railRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (10 * d).toInt() })

        // Data panel
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = FramedPanelDrawable(bgPanel, hairHot, hairHot, d)
            setPadding((22 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
        }

        val etaPair = valueCell("ETA", "--:--")
        etaValue = etaPair.second
        panel.addView(etaPair.first)
        panel.addView(divider())

        val distPair = valueCell("DIST", "--")
        distValue = distPair.second
        panel.addView(distPair.first)
        panel.addView(divider())

        val timePair = valueCell("T-REM", "--")
        timeValue = timePair.second
        panel.addView(timePair.first)

        panel.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))

        // Target destination callout
        val targetCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        targetCol.addView(tag("TGT · 01", fgGhost).apply { gravity = Gravity.END })
        targetCol.addView(TextView(context).apply {
            text = "EMBARCADERO / PIER 39"
            setTextColor(fg)
            textSize = 12f
            typeface = mono
            letterSpacing = 0.06f
            gravity = Gravity.END
        })
        panel.addView(targetCol, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = (16 * d).toInt() })

        // Abort button — wine-red framed slab
        val abort = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = FramedPanelDrawable(
                bg = Color.TRANSPARENT,
                borderColor = wineLine,
                bracketColor = wineLine,
                densityPx = d
            )
            setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
            isClickable = true; isFocusable = true
            setOnClickListener { onCancel?.invoke() }
        }
        abort.addView(XGlyph(context, wineLine, d), LinearLayout.LayoutParams(
            (10 * d).toInt(), (10 * d).toInt()
        ).apply { rightMargin = (8 * d).toInt() })
        abort.addView(TextView(context).apply {
            text = "ABORT"
            setTextColor(wineLine)
            textSize = 10f
            typeface = monoBold
            letterSpacing = 0.2f
            includeFontPadding = false
        })
        panel.addView(abort)

        column.addView(panel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        addView(column, LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.BOTTOM })
    }

    fun update(distanceText: String, durationText: String, arrivalText: String, fraction: Float) {
        distValue.text = distanceText
        timeValue.text = durationText
        etaValue.text = arrivalText
        val f = fraction.coerceIn(0f, 1f)
        pctLabel.text = "${(f * 100).toInt()}%"
        progressBar.setFraction(f)
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

    private fun valueCell(label: String, initial: String): Pair<LinearLayout, TextView> {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(tag(label, fgDim), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = (10 * d).toInt() })
        val value = TextView(context).apply {
            text = initial
            setTextColor(fg)
            textSize = 20f
            typeface = monoBold
            letterSpacing = 0.04f
            includeFontPadding = false
        }
        row.addView(value)
        return row to value
    }

    private fun divider(): View = View(context).apply {
        setBackgroundColor(hairDim)
        layoutParams = LinearLayout.LayoutParams(
            (1 * d).toInt().coerceAtLeast(1), (26 * d).toInt()
        ).apply {
            leftMargin = (22 * d).toInt(); rightMargin = (22 * d).toInt()
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

/** Horizontal progress rail: ticks + wine-red fill + diamond marker. */
private class ProgressRailView(
    context: Context,
    private val d: Float,
    private val dim: Int,
    private val wine: Int,
    private val fg: Int
) : View(context) {
    private var f: Float = 0f
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = dim; strokeWidth = d
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = wine; strokeWidth = d
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = dim; strokeWidth = d
    }
    private val wineTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = wine; strokeWidth = d
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = fg; strokeWidth = d
    }
    private val markerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.BLACK
    }

    fun setFraction(v: Float) { f = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val midY = h / 2f
        canvas.drawLine(0f, midY, w, midY, basePaint)
        canvas.drawLine(0f, midY, w * f, midY, fillPaint)

        // Ticks every 10%, majors every 50%
        for (i in 0..10) {
            val x = (i / 10f) * w
            val major = i % 5 == 0
            val top = if (major) 2 * d else 5 * d
            val bot = top + if (major) 14 * d else 8 * d
            val p = if ((i / 10f) <= f) wineTickPaint else tickPaint
            canvas.drawLine(x, top, x, bot, p)
        }

        // Diamond position marker
        val mx = w * f
        canvas.drawLine(mx, 0f, mx, h, markerPaint)
        val ms = 3.5f * d
        val path = android.graphics.Path().apply {
            moveTo(mx, 0f)
            lineTo(mx + ms, ms)
            lineTo(mx, ms * 2)
            lineTo(mx - ms, ms)
            close()
        }
        canvas.drawPath(path, markerFill)
        canvas.drawPath(path, markerPaint)
    }
}

private class XGlyph(context: Context, private val c: Int, private val d: Float) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = c; strokeWidth = 1.2f * d; strokeCap = Paint.Cap.SQUARE
    }
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawLine(d, d, w - d, h - d, p)
        canvas.drawLine(w - d, d, d, h - d, p)
    }
}
