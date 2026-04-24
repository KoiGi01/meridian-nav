package com.lidar.nav.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * The signature Meridian panel chrome:
 *   - solid dark fill
 *   - 1px hairline border
 *   - 4 short L-brackets at the corners in a slightly brighter color
 *
 * Used across every overlay panel — TurnCard, TripSheet, SpeedBubble, Search,
 * MapControls buttons.
 */
class FramedPanelDrawable(
    private val bg: Int,
    private val borderColor: Int,
    private val bracketColor: Int,
    private val densityPx: Float,
    private val bracketLenDp: Float = 10f,
    private val borderStrokeDp: Float = 1f,
    private val bracketStrokeDp: Float = 1f
) : Drawable() {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bg
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = borderColor
        strokeWidth = borderStrokeDp * densityPx
    }
    private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = bracketColor
        strokeWidth = bracketStrokeDp * densityPx
        strokeCap = Paint.Cap.SQUARE
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val sw = borderPaint.strokeWidth
        val l = b.left + sw / 2f
        val t = b.top + sw / 2f
        val r = b.right - sw / 2f
        val bot = b.bottom - sw / 2f

        canvas.drawRect(l, t, r, bot, fillPaint)
        canvas.drawRect(l, t, r, bot, borderPaint)

        val lb = bracketLenDp * densityPx
        // TL
        canvas.drawLine(l, t, l + lb, t, bracketPaint)
        canvas.drawLine(l, t, l, t + lb, bracketPaint)
        // TR
        canvas.drawLine(r - lb, t, r, t, bracketPaint)
        canvas.drawLine(r, t, r, t + lb, bracketPaint)
        // BL
        canvas.drawLine(l, bot - lb, l, bot, bracketPaint)
        canvas.drawLine(l, bot, l + lb, bot, bracketPaint)
        // BR
        canvas.drawLine(r - lb, bot, r, bot, bracketPaint)
        canvas.drawLine(r, bot - lb, r, bot, bracketPaint)
    }

    override fun setAlpha(alpha: Int) { fillPaint.alpha = alpha; borderPaint.alpha = alpha; bracketPaint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { fillPaint.colorFilter = cf; borderPaint.colorFilter = cf; bracketPaint.colorFilter = cf }
    @Deprecated("deprecated in API level 29")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
