package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
 * Top-left turn instruction slab.
 *
 * Composition:
 *   [ ARROW_ICON ][ DISTANCE + UNIT  ────  IN ]           [ ETA bar ]
 *                [ ● street name                      ]
 *
 * Above the panel: tag strip "MANEUVER · TURN_RIGHT ──── NAV_03 · PRI"
 * Below the panel: ghost "THEN ↰ Mission St · 0.4 mi" (dashed hairline).
 *
 * Urgent (<= 500m): distance color shifts to amber #FFB300, the vertical bar
 * fills proportionally with amber. Otherwise phosphor white + wine red bar.
 */
class TurnCardView @JvmOverloads constructor(
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

    private val tagManeuver: TextView
    private val tagPri: TextView
    private val arrow: TurnArrowView
    private val distanceValue: TextView
    private val distanceUnit: TextView
    private val streetLabel: TextView
    private val urgencyBar: UrgencyBarView
    private val panel: LinearLayout
    private val ghostRow: LinearLayout
    private val ghostArrow: TurnArrowView
    private val ghostLabel: TextView

    private var isCardVisible = false
    private var currentDistanceM = Float.MAX_VALUE

    init {
        orientation = VERTICAL
        visibility = View.GONE
        alpha = 0f

        // ── Tag strip ────────────────────────────────────────────────────────
        val tagStrip = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tagManeuver = tag("MANEUVER · --", fgDim)
        tagPri = tag("NAV_03 · PRI", fgGhost)
        tagStrip.addView(tagManeuver)
        tagStrip.addView(View(context).apply { setBackgroundColor(hairDim) },
            LayoutParams(0, (1 * d).toInt().coerceAtLeast(1), 1f).apply {
                leftMargin = (14 * d).toInt(); rightMargin = (14 * d).toInt()
            })
        tagStrip.addView(tagPri)
        addView(tagStrip, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = (6 * d).toInt()
        })

        // ── Main panel ───────────────────────────────────────────────────────
        panel = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = FramedPanelDrawable(bgPanel, hairHot, hairHot, d)
            setPadding((20 * d).toInt(), (18 * d).toInt(), (20 * d).toInt(), (18 * d).toInt())
        }

        arrow = TurnArrowView(context)
        panel.addView(arrow, LayoutParams((72 * d).toInt(), (72 * d).toInt()).apply {
            rightMargin = (20 * d).toInt()
        })

        val textColumn = LinearLayout(context).apply { orientation = VERTICAL }

        // DIST row
        val distRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        distanceValue = TextView(context).apply {
            text = "--"
            setTextColor(fg)
            textSize = 48f
            typeface = monoBold
            includeFontPadding = false
            letterSpacing = -0.01f
        }
        distanceUnit = TextView(context).apply {
            text = "MI"
            setTextColor(fg)
            textSize = 14f
            typeface = mono
            letterSpacing = 0.2f
            includeFontPadding = false
        }
        distRow.addView(distanceValue)
        distRow.addView(distanceUnit, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = (8 * d).toInt(); bottomMargin = (6 * d).toInt(); gravity = Gravity.BOTTOM })
        distRow.addView(View(context).apply { setBackgroundColor(hairDim) },
            LayoutParams(0, (1 * d).toInt().coerceAtLeast(1), 1f).apply {
                leftMargin = (12 * d).toInt(); rightMargin = (10 * d).toInt()
                gravity = Gravity.CENTER_VERTICAL
            })
        distRow.addView(tag("IN", fgDim))
        textColumn.addView(distRow)

        // Street row
        val streetRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        streetRow.addView(View(context).apply {
            background = BorderSquareDrawable(hairHot, d)
        }, LayoutParams((6 * d).toInt(), (6 * d).toInt()).apply { rightMargin = (10 * d).toInt() })
        streetLabel = TextView(context).apply {
            text = ""
            setTextColor(fg)
            textSize = 14f
            typeface = mono
            letterSpacing = 0.04f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        streetRow.addView(streetLabel)
        textColumn.addView(streetRow, LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (8 * d).toInt() })

        panel.addView(textColumn, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        // Divider
        panel.addView(View(context).apply { setBackgroundColor(hairDim) },
            LayoutParams((1 * d).toInt().coerceAtLeast(1), (64 * d).toInt()).apply {
                leftMargin = (16 * d).toInt(); rightMargin = (16 * d).toInt()
            })

        // Urgency bar
        val urgencyCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        urgencyCol.addView(tag("ETA·T", fgDim))
        urgencyBar = UrgencyBarView(context, d, hairDim, wineLine, amber)
        urgencyCol.addView(urgencyBar, LayoutParams((20 * d).toInt(), (56 * d).toInt()).apply {
            topMargin = (6 * d).toInt(); bottomMargin = (6 * d).toInt()
        })
        urgencyCol.addView(tag("0", fgDim))
        panel.addView(urgencyCol)

        addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // ── Ghost next-turn ──────────────────────────────────────────────────
        ghostRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = DashedBorderDrawable(hairDim, d, bgPanel)
            alpha = 0.6f
            setPadding((16 * d).toInt(), (10 * d).toInt(), (16 * d).toInt(), (10 * d).toInt())
            visibility = View.GONE
        }
        ghostRow.addView(tag("THEN", fgGhost), LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = (14 * d).toInt() })
        ghostArrow = TurnArrowView(context)
        ghostRow.addView(ghostArrow, LayoutParams((26 * d).toInt(), (26 * d).toInt()).apply {
            rightMargin = (12 * d).toInt()
        })
        ghostLabel = TextView(context).apply {
            text = ""
            setTextColor(fgDim)
            textSize = 12f
            typeface = mono
            letterSpacing = 0.04f
            maxLines = 1
        }
        ghostRow.addView(ghostLabel)

        addView(ghostRow, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (10 * d).toInt(); leftMargin = (40 * d).toInt() })
    }

    fun showManeuver(street: String, maneuverType: String, distanceM: Float) {
        arrow.setManeuver(maneuverType)
        streetLabel.text = street
        currentDistanceM = distanceM
        applyDistance(distanceM)
        tagManeuver.text = "MANEUVER · " + maneuverType.uppercase().replace(' ', '_')

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
        applyDistance(distanceM)
    }

    fun showNextManeuver(nextStreet: String?, nextManeuverType: String?) {
        if (nextStreet.isNullOrBlank()) {
            ghostRow.visibility = View.GONE
            return
        }
        ghostArrow.setManeuver(nextManeuverType ?: "straight")
        ghostLabel.text = nextStreet
        ghostRow.visibility = View.VISIBLE
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
        ghostRow.visibility = View.GONE
    }

    private fun applyDistance(m: Float) {
        val urgent = m <= 500f
        val color = if (urgent) amber else fg
        val miles = m / 1609.344f
        if (miles >= 0.1f) {
            distanceValue.text = "%.1f".format(miles)
            distanceUnit.text = "MI"
        } else {
            distanceValue.text = (m * 3.28084f).toInt().toString()
            distanceUnit.text = "FT"
        }
        distanceValue.setTextColor(color)
        distanceUnit.setTextColor(color)
        arrow.setUrgent(urgent)
        urgencyBar.setDistance(m, urgent)
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

/** 6dp square border — the bullet before street names. */
private class BorderSquareDrawable(private val c: Int, private val d: Float) : android.graphics.drawable.Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = c; strokeWidth = d
    }
    override fun draw(canvas: android.graphics.Canvas) {
        val b = bounds; val sw = p.strokeWidth
        canvas.drawRect(b.left + sw / 2f, b.top + sw / 2f, b.right - sw / 2f, b.bottom - sw / 2f, p)
    }
    override fun setAlpha(a: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Deprecated("deprecated in API level 29")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

/** Dashed-border panel with filled bg — for the ghost "THEN" pill. */
private class DashedBorderDrawable(
    private val stroke: Int, private val d: Float, private val bg: Int
) : android.graphics.drawable.Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = bg }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = stroke; strokeWidth = d
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(4 * d, 3 * d), 0f)
    }
    override fun draw(canvas: android.graphics.Canvas) {
        val b = bounds; val sw = line.strokeWidth
        canvas.drawRect(b.left + sw / 2f, b.top + sw / 2f, b.right - sw / 2f, b.bottom - sw / 2f, fill)
        canvas.drawRect(b.left + sw / 2f, b.top + sw / 2f, b.right - sw / 2f, b.bottom - sw / 2f, line)
    }
    override fun setAlpha(a: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Deprecated("deprecated in API level 29")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

/** Vertical countdown bar — fills upward as distance decreases. */
private class UrgencyBarView(
    context: Context,
    private val d: Float,
    private val dimColor: Int,
    private val normalColor: Int,
    private val urgentColor: Int
) : View(context) {
    private var fill = 0f
    private var urgent = false
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = dimColor; strokeWidth = d
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = normalColor
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = dimColor; strokeWidth = d
    }

    fun setDistance(m: Float, urgent: Boolean) {
        fill = (1f - (m.coerceAtMost(2000f) / 2000f)).coerceIn(0f, 1f)
        this.urgent = urgent
        fillPaint.color = if (urgent) urgentColor else normalColor
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val sw = border.strokeWidth
        canvas.drawRect(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f, border)
        val fillH = (h - sw * 2f) * fill
        canvas.drawRect(sw, h - sw - fillH, w - sw, h - sw, fillPaint)
        // tick marks at 25/50/75%
        listOf(0.25f, 0.5f, 0.75f).forEach { p ->
            val y = h - sw - (h - sw * 2f) * p
            canvas.drawLine(-3 * d, y, 3 * d, y, tickPaint)
            canvas.drawLine(w - 3 * d, y, w + 3 * d, y, tickPaint)
        }
    }
}
