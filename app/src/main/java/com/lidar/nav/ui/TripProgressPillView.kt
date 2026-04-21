package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class TripProgressPillView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val density = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val etaValue: TextView
    private val distValue: TextView
    private val timeValue: TextView
    private val segBar: SegmentedBar
    private val cancelButton: IconButton

    var onCancel: (() -> Unit)? = null

    init {
        val padH = (20 * density).toInt()
        val padV = (10 * density).toInt()
        setPadding(padH, padV, padH, padV)
        background = BracketDrawable(
            legLengthPx = 12 * density,
            strokeWidthPx = 1.2f * density,
            strokeColor = Color.WHITE,
            fillColor = Color.parseColor("#CC000000")
        )

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val textBlock = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        row.addView(textBlock, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val dataRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        textBlock.addView(dataRow)

        val etaCol = fieldColumn("ETA", "--:--").also { dataRow.addView(it.first, spaced()) }
        val distCol = fieldColumn("DIST", "--").also { dataRow.addView(it.first, spaced()) }
        val timeCol = fieldColumn("T-", "--").also { dataRow.addView(it.first, spaced()) }
        etaValue = etaCol.second
        distValue = distCol.second
        timeValue = timeCol.second

        segBar = SegmentedBar(context)
        textBlock.addView(segBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (4 * density).toInt()
        ).apply { topMargin = (8 * density).toInt() })

        cancelButton = IconButton(context).apply {
            setIconResource(R.drawable.ic_close)
            setLabel("ABORT")
            setOnClickListener { onCancel?.invoke() }
        }
        val btnSize = (56 * density).toInt()
        row.addView(cancelButton, LinearLayout.LayoutParams(btnSize, btnSize).apply {
            leftMargin = (16 * density).toInt()
        })
    }

    private fun fieldColumn(label: String, initial: String): Pair<LinearLayout, TextView> {
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 9f
            typeface = mono
            letterSpacing = 0.2f
        })
        val value = TextView(context).apply {
            text = initial
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.create(mono, Typeface.BOLD)
        }
        col.addView(value)
        return col to value
    }

    private fun spaced() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { rightMargin = (24 * density).toInt() }

    fun update(distanceText: String, durationText: String, arrivalText: String, fraction: Float) {
        this.distValue.text = distanceText
        this.timeValue.text = durationText
        this.etaValue.text = arrivalText
        segBar.fraction = fraction.coerceIn(0f, 1f)
    }

    private class SegmentedBar(context: Context) : View(context) {
        var fraction: Float = 0f
            set(value) { field = value; invalidate() }

        private val segments = 20
        private val gapRatio = 0.25f

        private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33FFFFFF")
        }
        private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6b0919")
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val totalGapUnits = (segments - 1) * gapRatio
            val segW = w / (segments + totalGapUnits)
            val gapW = segW * gapRatio
            val filledCount = (segments * fraction).toInt()
            var x = 0f
            for (i in 0 until segments) {
                val paint = if (i < filledCount) activePaint else inactivePaint
                canvas.drawRect(RectF(x, 0f, x + segW, h), paint)
                x += segW + gapW
            }
        }
    }
}
