package com.lidar.nav.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class BracketDrawable(
    private val legLengthPx: Float,
    private val strokeWidthPx: Float,
    private val strokeColor: Int = Color.WHITE,
    private val fillColor: Int = Color.parseColor("#B3000000")
) : Drawable() {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.SQUARE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    var strokeTint: Int = strokeColor
        set(value) { field = value; strokePaint.color = value; invalidateSelf() }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left.toFloat()
        val t = b.top.toFloat()
        val r = b.right.toFloat()
        val bt = b.bottom.toFloat()
        val inset = strokeWidthPx / 2f
        canvas.drawRect(l, t, r, bt, fillPaint)

        canvas.drawLine(l + inset, t + inset, l + legLengthPx, t + inset, strokePaint)
        canvas.drawLine(l + inset, t + inset, l + inset, t + legLengthPx, strokePaint)

        canvas.drawLine(r - legLengthPx, t + inset, r - inset, t + inset, strokePaint)
        canvas.drawLine(r - inset, t + inset, r - inset, t + legLengthPx, strokePaint)

        canvas.drawLine(l + inset, bt - inset, l + legLengthPx, bt - inset, strokePaint)
        canvas.drawLine(l + inset, bt - legLengthPx, l + inset, bt - inset, strokePaint)

        canvas.drawLine(r - legLengthPx, bt - inset, r - inset, bt - inset, strokePaint)
        canvas.drawLine(r - inset, bt - legLengthPx, r - inset, bt - inset, strokePaint)
    }

    override fun setAlpha(alpha: Int) { strokePaint.alpha = alpha; fillPaint.alpha = alpha }
    override fun setColorFilter(filter: ColorFilter?) {
        strokePaint.colorFilter = filter; fillPaint.colorFilter = filter
    }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
