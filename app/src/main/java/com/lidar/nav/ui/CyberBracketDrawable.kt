package com.lidar.nav.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

class CyberBracketDrawable(
    private val legLengthPx: Float,
    private val strokeWidthPx: Float,
    private val strokeColor: Int = Color.parseColor("#00E5FF"),
    private val fillColor: Int = Color.parseColor("#80001015"),
    private val glowRadius: Float = 10f
) : Drawable() {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = strokeColor
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
        setShadowLayer(glowRadius, 0f, 0f, strokeColor)
    }

    private val path = Path()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val inset = strokeWidthPx / 2f + glowRadius
        
        // Draw Fill
        canvas.drawRect(
            bounds.left + inset, bounds.top + inset,
            bounds.right - inset, bounds.bottom - inset,
            fillPaint
        )

        // Draw Brackets
        path.reset()
        val left = bounds.left + inset
        val top = bounds.top + inset
        val right = bounds.right - inset
        val bottom = bounds.bottom - inset

        // Top Left
        path.moveTo(left + legLengthPx, top)
        path.lineTo(left, top)
        path.lineTo(left, top + legLengthPx)

        // Top Right
        path.moveTo(right - legLengthPx, top)
        path.lineTo(right, top)
        path.lineTo(right, top + legLengthPx)

        // Bottom Left
        path.moveTo(left, bottom - legLengthPx)
        path.lineTo(left, bottom)
        path.lineTo(left + legLengthPx, bottom)

        // Bottom Right
        path.moveTo(right, bottom - legLengthPx)
        path.lineTo(right, bottom)
        path.lineTo(right - legLengthPx, bottom)

        canvas.drawPath(path, strokePaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    // Ensure getPadding allows child content to sit inside the brackets properly
    override fun getPadding(padding: Rect): Boolean {
        val p = (glowRadius + strokeWidthPx + 4f).toInt()
        padding.set(p, p, p, p)
        return true
    }
}
