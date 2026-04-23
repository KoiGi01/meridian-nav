package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class BottomRailView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val d = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    // Speed
    private val speedNumber: TextView
    private val speedUnit: TextView

    // Maneuver
    private val turnArrow: TurnArrowView
    private val maneuverStreet: TextView
    private val maneuverDist: TextView
    private val wordmark: TextView

    // Trip
    private val tripZone: LinearLayout
    private val etaValue: TextView
    private val distValue: TextView
    private val timeValue: TextView

    // Search
    private val searchIcon: ImageView

    var onSearch: (() -> Unit)? = null
    var onCancelRoute: (() -> Unit)? = null

    private var navigating = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.parseColor("#F2000000"))

        // Accent line at top
        val accentLine = View(context).apply {
            setBackgroundColor(Color.parseColor("#00E5FF"))
        }

        // We embed the rail inside a vertical container so we can add the accent line above
        // Actually easier: override dispatchDraw or just use a wrapper. We'll use a different
        // approach — the accent is drawn by the parent in MainActivity as a simple View.
        // So here we just build the rail row.

        // ── SPEED SECTION (fixed 140dp) ──────────────────────────────────────────
        val speedSection = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((20 * d).toInt(), 0, (16 * d).toInt(), 0)
        }

        speedNumber = TextView(context).apply {
            text = "--"
            setTextColor(Color.WHITE)
            textSize = 44f
            typeface = Typeface.create(mono, Typeface.BOLD)
            includeFontPadding = false
            letterSpacing = -0.03f
        }
        speedUnit = TextView(context).apply {
            text = "KM/H"
            setTextColor(Color.parseColor("#55FFFFFF"))
            textSize = 11f
            typeface = mono
            letterSpacing = 0.15f
        }
        speedSection.addView(speedNumber)
        speedSection.addView(speedUnit)
        addView(speedSection, LayoutParams((140 * d).toInt(), LayoutParams.MATCH_PARENT))

        // Divider
        addView(divider())

        // ── MANEUVER SECTION (flex) ───────────────────────────────────────────────
        val maneuverSection = FrameLayout(context)

        // Wordmark shown when idle
        wordmark = TextView(context).apply {
            text = "MERIDIAN"
            setTextColor(Color.parseColor("#22FFFFFF"))
            textSize = 20f
            typeface = mono
            letterSpacing = 0.4f
            gravity = Gravity.CENTER
        }
        maneuverSection.addView(wordmark, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.CENTER })

        // Maneuver content — hidden until navigating
        val maneuverContent = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        turnArrow = TurnArrowView(context)
        maneuverContent.addView(turnArrow, LinearLayout.LayoutParams(
            (36 * d).toInt(), (36 * d).toInt()
        ).apply { rightMargin = (16 * d).toInt() })

        val maneuverText = LinearLayout(context).apply { orientation = VERTICAL }
        maneuverDist = TextView(context).apply {
            text = "--"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.create(mono, Typeface.BOLD)
            includeFontPadding = false
        }
        maneuverStreet = TextView(context).apply {
            text = ""
            setTextColor(Color.parseColor("#AAFFFFFF"))
            textSize = 13f
            typeface = mono
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        maneuverText.addView(maneuverDist)
        maneuverText.addView(maneuverStreet)
        maneuverContent.addView(maneuverText)

        maneuverSection.addView(maneuverContent, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.CENTER_VERTICAL; leftMargin = (16 * d).toInt() })

        // Store ref for show/hide
        maneuverSection.tag = maneuverContent

        addView(maneuverSection, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        // Divider
        addView(divider())

        // ── TRIP SECTION (fixed 180dp) — hidden when idle ─────────────────────────
        tripZone = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * d).toInt(), 0, (8 * d).toInt(), 0)
            visibility = View.GONE
            isLongClickable = true
            setOnLongClickListener {
                onCancelRoute?.invoke()
                true
            }
        }

        val etaCol = tripColumn("ETA", "--:--")
        val distCol = tripColumn("DIST", "--")
        val timeCol = tripColumn("T−", "--")
        etaValue = etaCol.second
        distValue = distCol.second
        timeValue = timeCol.second

        listOf(etaCol.first, distCol.first, timeCol.first).forEach { col ->
            tripZone.addView(col, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ))
        }
        addView(tripZone, LayoutParams((180 * d).toInt(), LayoutParams.MATCH_PARENT))

        // Divider before search
        addView(divider())

        // ── SEARCH ICON ───────────────────────────────────────────────────────────
        searchIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            setColorFilter(Color.parseColor("#66FFFFFF"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            isClickable = true
            isFocusable = true
            setOnClickListener { onSearch?.invoke() }
        }
        addView(searchIcon, LayoutParams((56 * d).toInt(), LayoutParams.MATCH_PARENT))
    }

    // ── PUBLIC API ────────────────────────────────────────────────────────────────

    fun updateSpeed(kmh: Int?) {
        speedNumber.text = kmh?.toString() ?: "--"
    }

    fun updateManeuver(street: String, maneuverType: String, distanceM: Float) {
        turnArrow.setManeuver(maneuverType)
        maneuverStreet.text = street
        maneuverDist.text = formatDistance(distanceM)
    }

    fun updateManeuverDistance(distanceM: Float) {
        if (navigating) maneuverDist.text = formatDistance(distanceM)
    }

    fun updateTrip(distanceText: String, durationText: String, arrivalText: String) {
        distValue.text = distanceText
        timeValue.text = durationText
        etaValue.text = arrivalText
    }

    fun showNavigating() {
        navigating = true
        wordmark.visibility = View.GONE
        // maneuverContent is tagged on the FrameLayout
        val maneuverSection = getChildAt(2) as? FrameLayout
        (maneuverSection?.tag as? View)?.visibility = View.VISIBLE
        tripZone.visibility = View.VISIBLE
    }

    fun showIdle() {
        navigating = false
        wordmark.visibility = View.VISIBLE
        val maneuverSection = getChildAt(2) as? FrameLayout
        (maneuverSection?.tag as? View)?.visibility = View.GONE
        tripZone.visibility = View.GONE
        maneuverStreet.text = ""
        maneuverDist.text = "--"
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────────

    private fun divider() = View(context).apply {
        setBackgroundColor(Color.parseColor("#18FFFFFF"))
    }.also { v ->
        // width set via addView params; height fills parent
        val lp = LayoutParams((1 * d).toInt().coerceAtLeast(1), LayoutParams.MATCH_PARENT)
        lp.topMargin = (12 * d).toInt()
        lp.bottomMargin = (12 * d).toInt()
        v.layoutParams = lp
    }

    private fun tripColumn(label: String, initial: String): Pair<LinearLayout, TextView> {
        val col = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        col.addView(TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#55FFFFFF"))
            textSize = 9f
            typeface = mono
            letterSpacing = 0.2f
            gravity = Gravity.CENTER_HORIZONTAL
        })
        val value = TextView(context).apply {
            text = initial
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.create(mono, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        col.addView(value)
        return col to value
    }

    private fun formatDistance(m: Float): String {
        val miles = m / 1609.344f
        return if (miles >= 0.1f) "%.1f mi".format(miles) else "${(m * 3.28084f).toInt()} ft"
    }
}
