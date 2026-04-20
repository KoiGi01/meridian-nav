package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var alpha: Float,
        var radius: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    var energyLevel: Float = 0.3f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    var beatIntensity: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            if (value > 0.5f) spawnBurst((value * 8).toInt())
        }

    var intensityMultiplier: Float = 1.0f

    private val particleCount = 80
    private var lastFrameTime = System.currentTimeMillis()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        repeat(particleCount) { particles.add(spawnParticle()) }
    }

    private fun spawnParticle(): Particle {
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
        val speed = (0.2f + Random.nextFloat() * 0.8f)
        return Particle(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            alpha = 0.1f + Random.nextFloat() * 0.4f,
            radius = 1f + Random.nextFloat() * 2f
        )
    }

    private fun spawnBurst(count: Int) {
        repeat(count) {
            val p = spawnParticle()
            p.vx *= 3f
            p.vy *= 3f
            p.alpha = 0.8f
            particles.add(p)
        }
        while (particles.size > particleCount + 40) particles.removeAt(0)
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastFrameTime) / 1000f).coerceAtMost(0.05f)
        lastFrameTime = now

        val speedFactor = (0.3f + energyLevel * 0.7f) * intensityMultiplier

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) { invalidate(); return }

        val toRemove = mutableListOf<Particle>()
        for (p in particles) {
            p.x += p.vx * dt * speedFactor * 0.03f
            p.y += p.vy * dt * speedFactor * 0.03f
            p.alpha -= dt * 0.02f * speedFactor
            if (p.alpha <= 0f || p.x < 0f || p.x > 1f || p.y < 0f || p.y > 1f) {
                toRemove.add(p)
            } else {
                paint.alpha = (p.alpha * 255 * intensityMultiplier).toInt().coerceIn(0, 255)
                canvas.drawCircle(p.x * w, p.y * h, p.radius, paint)
            }
        }
        particles.removeAll(toRemove)
        while (particles.size < particleCount) particles.add(spawnParticle())

        invalidate()
    }
}
