package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridReactivityView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    private var flashAlpha = 0f
    private val gridSpacingDp = 60f
    private var lastFrame = System.currentTimeMillis()

    var intensityMultiplier: Float = 1.0f

    fun onBeat(intensity: Float) {
        flashAlpha = (intensity * 0.4f * intensityMultiplier).coerceIn(0f, 0.4f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val dt = (now - lastFrame) / 1000f
        lastFrame = now

        flashAlpha = (flashAlpha - dt * 5f).coerceAtLeast(0f)

        val alpha = (flashAlpha * 255).toInt()
        if (alpha <= 0) return

        gridPaint.alpha = alpha

        val spacing = gridSpacingDp * resources.displayMetrics.density
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += spacing
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += spacing
        }

        if (flashAlpha > 0f) invalidate()
    }
}
