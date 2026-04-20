package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class CompassView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        alpha = 200
    }

    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6b0919")
        style = Paint.Style.FILL
    }

    var bearing: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) / 2f) * 0.85f

        canvas.save()
        canvas.rotate(-bearing, cx, cy)

        canvas.drawCircle(cx, cy, r, linePaint)

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val innerR = if (i % 2 == 0) r * 0.75f else r * 0.85f
            canvas.drawLine(
                cx + (innerR * sin(angle)).toFloat(),
                cy - (innerR * cos(angle)).toFloat(),
                cx + (r * sin(angle)).toFloat(),
                cy - (r * cos(angle)).toFloat(),
                linePaint
            )
        }

        val path = Path().apply {
            moveTo(cx, cy - r * 0.6f)
            lineTo(cx - r * 0.1f, cy)
            lineTo(cx + r * 0.1f, cy)
            close()
        }
        canvas.drawPath(path, accentPaint)

        val southPath = Path().apply {
            moveTo(cx, cy + r * 0.6f)
            lineTo(cx - r * 0.1f, cy)
            lineTo(cx + r * 0.1f, cy)
            close()
        }
        canvas.drawPath(southPath, linePaint)

        canvas.restore()
    }
}
