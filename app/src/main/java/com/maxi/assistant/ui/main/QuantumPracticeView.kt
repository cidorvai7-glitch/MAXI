package com.maxi.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class QuantumParticlesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null

    private var centerX = 0f
    private var centerY = 0f
    private var maxRadius = 0f

    private val NUM_PARTICLES = 100
    private val PARTICLE_SIZE = 5f
    private val PARTICLE_SPEED = 0.5f

    init {
        particlePaint.color = Color.CYAN // Default particle color
        particlePaint.setShadowLayer(10f, 0f, 0f, Color.BLUE)
        setLayerType(LAYER_TYPE_SOFTWARE, particlePaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        maxRadius = (w.coerceAtMost(h) / 2 * 0.8f)

        if (particles.isEmpty()) {
            for (i in 0 until NUM_PARTICLES) {
                particles.add(createRandomParticle())
            }
        }
    }

    private fun createRandomParticle(): Particle {
        val angle = Random.nextFloat() * 360
        val radius = Random.nextFloat() * maxRadius
        val x = centerX + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val y = centerY + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val speed = Random.nextFloat() * PARTICLE_SPEED + 0.1f
        val direction = Random.nextFloat() * 360
        val life = Random.nextInt(50, 200)
        return Particle(x, y, speed, direction, life)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        particles.forEach { particle ->
            particlePaint.alpha = (particle.life / 200f * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(particle.x, particle.y, PARTICLE_SIZE, particlePaint)
        }
    }

    fun startAnimation() {
        stopAnimation()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10000L // Long duration for continuous animation
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { 
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun updateParticles() {
        val tempParticles = mutableListOf<Particle>()
        particles.forEach { particle ->
            particle.x += particle.speed * cos(Math.toRadians(particle.direction.toDouble())).toFloat()
            particle.y += particle.speed * sin(Math.toRadians(particle.direction.toDouble())).toFloat()
            particle.life--

            // If particle goes out of bounds or dies, re-create it
            if (particle.life <= 0 || !isParticleInBounds(particle)) {
                tempParticles.add(createRandomParticle())
            } else {
                tempParticles.add(particle)
            }
        }
        particles.clear()
        particles.addAll(tempParticles)
    }

    private fun isParticleInBounds(particle: Particle): Boolean {
        val distance = Math.sqrt(Math.pow((particle.x - centerX).toDouble(), 2.0) + Math.pow((particle.y - centerY).toDouble(), 2.0))
        return distance < maxRadius * 1.2 // Allow a bit outside maxRadius before resetting
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    data class Particle(
        var x: Float,
        var y: Float,
        var speed: Float,
        var direction: Float,
        var life: Int
    )
}
