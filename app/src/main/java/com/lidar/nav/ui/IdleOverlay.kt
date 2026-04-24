package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

/**
 * IDLE state overlay — tactical standby.
 *
 * Layout:
 *   top-left:  MERIDIAN wordmark + sensor status strip
 *   top-right: live WGS84 coordinate readout
 *   center:    faint reticle (drawn by BackdropView — no-op in layout here)
 *   bottom-left: elongated "specify target" query slab → searchButton
 *   bottom-right: STANDBY indicator with blinking wine-red dot
 *
 * Aesthetic: hairline frames, L-brackets, JetBrains Mono, phosphor white + wine red.
 * No pills, no rounded sheets, no cyan.
 */
class IdleOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val searchButton: LinearLayout

    private val d = resources.displayMetrics.density

    private val mono: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }
    private val monoBold: Typeface? by lazy { Typeface.create(mono, Typeface.BOLD) }

    // Palette
    private val fg        = Color.parseColor("#E8EDF0")
    private val fgDim     = Color.parseColor("#8CE8EDF0") // 55%
    private val fgGhost   = Color.parseColor("#52E8EDF0") // 32%
    private val hairHot   = Color.parseColor("#8CE8EDF0")
    private val hairDim   = Color.parseColor("#1AE8EDF0")
    private val wineLine  = Color.parseColor("#8A1226")
    private val bgPanel   = Color.parseColor("#C7000000")

    private val blinkDot: View

    init {
        isMotionEventSplittingEnabled = false
        setBackgroundColor(Color.TRANSPARENT)

        // ── TOP-LEFT: wordmark stack ────────────────────────────────────────
        val topLeft = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val wordRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // Square glyph
        wordRow.addView(View(context).apply {
            background = BracketBoxDrawable(fg, filledInsetDp = 3, densityPx = d)
        }, LinearLayout.LayoutParams((14 * d).toInt(), (14 * d).toInt()).apply {
            rightMargin = (10 * d).toInt()
        })
        wordRow.addView(TextView(context).apply {
            text = "MERIDIAN"
            setTextColor(fg)
            textSize = 16f
            letterSpacing = 0.32f
            typeface = monoBold
            includeFontPadding = false
        })
        wordRow.addView(View(context).apply {
            setBackgroundColor(hairHot)
        }, LinearLayout.LayoutParams((28 * d).toInt(), (1 * d).toInt().coerceAtLeast(1)).apply {
            leftMargin = (10 * d).toInt()
            rightMargin = (10 * d).toInt()
        })
        wordRow.addView(tag("NAV / TACT · v4.2", fgDim))
        topLeft.addView(wordRow)

        val statusRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        listOf(
            "FIX_3D" to fg,
            "SATS 11/14" to fgGhost,
            "IMU · OK" to fgGhost,
            "LIDAR · STBY" to fgGhost
        ).forEachIndexed { i, (t, c) ->
            val tv = tag(t, c)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { if (i > 0) leftMargin = (18 * d).toInt() }
            statusRow.addView(tv, lp)
        }
        topLeft.addView(statusRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (8 * d).toInt() })

        addView(topLeft, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = (28 * d).toInt()
            topMargin = (24 * d).toInt()
        })

        // ── TOP-RIGHT: coordinate readout ───────────────────────────────────
        val topRight = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        topRight.addView(tag("POS · WGS84", fgDim).apply { gravity = Gravity.END })
        topRight.addView(TextView(context).apply {
            text = "37°46′29.4″N"
            setTextColor(fg)
            textSize = 11f
            typeface = mono
            letterSpacing = 0.08f
            gravity = Gravity.END
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (4 * d).toInt() })
        topRight.addView(TextView(context).apply {
            text = "122°25′11.6″W"
            setTextColor(fg)
            textSize = 11f
            typeface = mono
            letterSpacing = 0.08f
            gravity = Gravity.END
        })
        topRight.addView(TextView(context).apply {
            text = "HDG 274° · ELEV 68m"
            setTextColor(fgGhost)
            textSize = 10f
            typeface = mono
            letterSpacing = 0.12f
            gravity = Gravity.END
        })
        addView(topRight, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.END
            rightMargin = (28 * d).toInt()
            topMargin = (24 * d).toInt()
        })

        // ── BOTTOM-LEFT: search invocation slab ─────────────────────────────
        // Tag strip above the slab
        val searchTagRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        searchTagRow.addView(tag("QUERY · DESTINATION", fgDim))
        searchTagRow.addView(View(context).apply { setBackgroundColor(hairDim) },
            LinearLayout.LayoutParams(0, (1 * d).toInt().coerceAtLeast(1), 1f).apply {
                leftMargin = (12 * d).toInt(); rightMargin = (12 * d).toInt()
            })
        searchTagRow.addView(tag("SRCH_01", fgGhost))

        val searchWrap = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        searchWrap.addView(searchTagRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (6 * d).toInt() })

        searchButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = FramedPanelDrawable(
                bg = bgPanel, borderColor = hairHot, bracketColor = hairHot, densityPx = d
            )
            setPadding((22 * d).toInt(), (18 * d).toInt(), (22 * d).toInt(), (18 * d).toInt())
        }
        // Magnifier glyph drawn as TextView — geometric, matches the rest.
        searchButton.addView(TextView(context).apply {
            text = "⌕"
            setTextColor(fg)
            textSize = 22f
            typeface = mono
            includeFontPadding = false
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = (18 * d).toInt() })

        val textStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        textStack.addView(tag("SPECIFY TARGET", fgDim))
        textStack.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = "▌"
                setTextColor(wineLine)
                textSize = 18f
                typeface = mono
                includeFontPadding = false
            })
            addView(TextView(context).apply {
                text = " specify target_"
                setTextColor(fg)
                textSize = 18f
                typeface = mono
                letterSpacing = 0.06f
                includeFontPadding = false
            })
        })

        searchButton.addView(textStack, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ))

        searchWrap.addView(searchButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        addView(searchWrap, LayoutParams((520 * d).toInt(), LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            leftMargin = (28 * d).toInt()
            bottomMargin = (28 * d).toInt()
        })

        // ── BOTTOM-RIGHT: STANDBY indicator ─────────────────────────────────
        val standby = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        standby.addView(tag("SYS · STANDBY · NO ROUTE", fgGhost).apply { gravity = Gravity.END })
        val dotRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        blinkDot = View(context).apply { setBackgroundColor(wineLine) }
        dotRow.addView(blinkDot, LinearLayout.LayoutParams((6 * d).toInt(), (6 * d).toInt()).apply {
            rightMargin = (6 * d).toInt()
        })
        dotRow.addView(tag("AWAIT INPUT", fgDim))
        standby.addView(dotRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (6 * d).toInt() })

        addView(standby, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            rightMargin = (28 * d).toInt()
            bottomMargin = (38 * d).toInt()
        })

        // Blink the standby dot
        val blinker = object : Runnable {
            var on = true
            override fun run() {
                blinkDot.alpha = if (on) 1f else 0.15f
                on = !on
                postDelayed(this, 700)
            }
        }
        post(blinker)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false

    private fun tag(text: String, color: Int, size: Float = 9f): TextView = TextView(context).apply {
        this.text = text
        setTextColor(color)
        textSize = size
        typeface = mono
        letterSpacing = 0.18f
        includeFontPadding = false
    }
}

/**
 * 14dp square: hairline border + filled inner square.
 */
private class BracketBoxDrawable(
    private val color: Int,
    private val filledInsetDp: Int,
    private val densityPx: Float
) : Drawable() {
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = densityPx
        this.color = this@BracketBoxDrawable.color
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = this@BracketBoxDrawable.color
    }
    override fun draw(canvas: Canvas) {
        val b = bounds
        val sw = border.strokeWidth
        canvas.drawRect(
            b.left + sw / 2f, b.top + sw / 2f,
            b.right - sw / 2f, b.bottom - sw / 2f, border
        )
        val inset = filledInsetDp * densityPx
        canvas.drawRect(
            b.left + inset, b.top + inset, b.right - inset, b.bottom - inset, fill
        )
    }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Deprecated("deprecated in API level 29")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
