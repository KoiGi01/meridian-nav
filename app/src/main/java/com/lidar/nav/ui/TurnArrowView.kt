package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class TurnArrowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var maneuver = "straight"

    fun setManeuver(type: String) {
        maneuver = type.lowercase()
        invalidate()
    }

    fun setUrgent(urgent: Boolean) {
        paint.color = if (urgent) Color.parseColor("#FFB800") else Color.WHITE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.4f

        val path = Path()
        when {
            maneuver.contains("right") -> {
                path.moveTo(cx - r, cy + r)
                path.lineTo(cx - r, cy - r * 0.3f)
                path.quadTo(cx - r, cy - r, cx, cy - r)
                path.lineTo(cx + r, cy - r)
                path.moveTo(cx + r - r * 0.4f, cy - r - r * 0.3f)
                path.lineTo(cx + r, cy - r)
                path.lineTo(cx + r - r * 0.4f, cy - r + r * 0.3f)
            }
            maneuver.contains("left") -> {
                path.moveTo(cx + r, cy + r)
                path.lineTo(cx + r, cy - r * 0.3f)
                path.quadTo(cx + r, cy - r, cx, cy - r)
                path.lineTo(cx - r, cy - r)
                path.moveTo(cx - r + r * 0.4f, cy - r - r * 0.3f)
                path.lineTo(cx - r, cy - r)
                path.lineTo(cx - r + r * 0.4f, cy - r + r * 0.3f)
            }
            else -> {
                path.moveTo(cx, cy + r)
                path.lineTo(cx, cy - r)
                path.moveTo(cx - r * 0.4f, cy - r + r * 0.4f)
                path.lineTo(cx, cy - r)
                path.lineTo(cx + r * 0.4f, cy - r + r * 0.4f)
            }
        }
        canvas.drawPath(path, paint)
    }
}
