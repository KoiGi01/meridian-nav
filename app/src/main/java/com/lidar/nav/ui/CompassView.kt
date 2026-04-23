package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CompassView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private var bearing: Float = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#00E5FF")
        strokeWidth = 2.5f * density
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#AA00E5FF"))
    }
    private val innerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CC000A14")
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#4400E5FF")
        strokeWidth = 1f * density
    }
    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF6040")
        setShadowLayer(6f, 0f, 0f, Color.parseColor("#88FF6040"))
    }
    private val southPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val needlePath = Path()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setBearing(mapBearing: Float) {
        bearing = mapBearing
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - (ringPaint.strokeWidth / 2f) - (4 * density)

        canvas.drawCircle(cx, cy, radius, innerFillPaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // 8 cardinal ticks
        val tickOuter = radius - 4 * density
        val tickInner = radius - 11 * density
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0))
            val cos = Math.cos(angle).toFloat()
            val sin = Math.sin(angle).toFloat()
            canvas.drawLine(
                cx + sin * tickInner, cy - cos * tickInner,
                cx + sin * tickOuter, cy - cos * tickOuter,
                tickPaint
            )
        }

        // Needle rotates opposite to map bearing (keeps pointing geographic north)
        canvas.save()
        canvas.rotate(-bearing, cx, cy)

        val needleLen = radius * 0.52f
        val needleW = 5f * density

        // North half — orange-red
        needlePath.reset()
        needlePath.moveTo(cx, cy - needleLen)
        needlePath.lineTo(cx - needleW, cy)
        needlePath.lineTo(cx + needleW, cy)
        needlePath.close()
        canvas.drawPath(needlePath, northPaint)

        // South half — white
        needlePath.reset()
        needlePath.moveTo(cx, cy + needleLen * 0.65f)
        needlePath.lineTo(cx - needleW, cy)
        needlePath.lineTo(cx + needleW, cy)
        needlePath.close()
        canvas.drawPath(needlePath, southPaint)

        canvas.restore()
    }
}
