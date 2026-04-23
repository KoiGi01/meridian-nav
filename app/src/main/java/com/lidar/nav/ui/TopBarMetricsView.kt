package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class TopBarMetricsView(context: Context) : LinearLayout(context) {

    private val density = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val speedValue: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.TRANSPARENT)

        // Box 1: Speed with bracket + cyan glow text
        val speedBox = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
            val padH = (20 * density).toInt()
            val padV = (10 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = CyberBracketDrawable(
                legLengthPx = 12 * density,
                strokeWidthPx = 1.5f * density,
                strokeColor = Color.parseColor("#00E5FF"),
                fillColor = Color.parseColor("#99001015")
            )
        }

        speedValue = TextView(context).apply {
            text = "--"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 52f
            typeface = Typeface.create(mono, Typeface.BOLD)
            includeFontPadding = false
            // software layer needed for shadow glow
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            setShadowLayer(16f, 0f, 0f, Color.parseColor("#AA00E5FF"))
        }
        speedBox.addView(speedValue)

        speedBox.addView(TextView(context).apply {
            text = "KM/H"
            setTextColor(Color.parseColor("#8800E5FF"))
            textSize = 15f
            typeface = mono
            setPadding((8 * density).toInt(), 0, 0, (10 * density).toInt())
        })

        addView(speedBox, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            rightMargin = (16 * density).toInt()
        })

        // Box 2: PWR_CELL_STATUS + EST_RANGE
        val pwrBox = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (18 * density).toInt()
            val padV = (10 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = CyberBracketDrawable(
                legLengthPx = 12 * density,
                strokeWidthPx = 1.5f * density,
                strokeColor = Color.parseColor("#00E5FF"),
                fillColor = Color.parseColor("#99001015")
            )
        }

        // Row 1: label + value
        val pwrRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        pwrRow.addView(TextView(context).apply {
            text = "PWR_CELL_STATUS"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = mono
            letterSpacing = 0.1f
        })
        pwrRow.addView(TextView(context).apply {
            text = "  84%"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = mono
        })
        pwrBox.addView(pwrRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = (6 * density).toInt()
        })

        // Segment bar
        val barContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        val activeSegments = 4
        val totalSegments = 5
        for (i in 0 until totalSegments) {
            barContainer.addView(android.view.View(context).apply {
                setBackgroundColor(
                    if (i < activeSegments) Color.parseColor("#00E5FF")
                    else Color.parseColor("#2200E5FF")
                )
            }, LayoutParams((28 * density).toInt(), (4 * density).toInt()).apply {
                rightMargin = (4 * density).toInt()
            })
        }
        pwrBox.addView(barContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = (6 * density).toInt()
        })

        // Row 2: range
        val rangeRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        rangeRow.addView(TextView(context).apply {
            text = "EST_RANGE:"
            setTextColor(Color.parseColor("#6600E5FF"))
            textSize = 11f
            typeface = mono
            letterSpacing = 0.08f
        })
        rangeRow.addView(TextView(context).apply {
            text = "  412 KM"
            setTextColor(Color.parseColor("#9900E5FF"))
            textSize = 11f
            typeface = mono
        })
        pwrBox.addView(rangeRow)

        addView(pwrBox, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun updateSpeed(speedKmH: Int?) {
        speedValue.text = speedKmH?.toString() ?: "--"
    }
}
