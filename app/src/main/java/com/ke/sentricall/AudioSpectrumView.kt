package com.ke.sentricall

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class AudioSpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()

    // 48 bars, values between 0f and 1f
    private var levels: FloatArray = FloatArray(48) { 0f }

    private var barWidthPx: Float = 0f
    private var barSpacingPx: Float = 0f
    private var gradient: LinearGradient? = null

    init {
        barPaint.style = Paint.Style.FILL
    }

    fun setLevels(newLevels: FloatArray) {
        if (newLevels.isEmpty()) return

        if (newLevels.size != levels.size) {
            // Resize / copy to our fixed length
            val copy = FloatArray(levels.size)
            val limit = minOf(levels.size, newLevels.size)
            System.arraycopy(newLevels, 0, copy, 0, limit)
            levels = copy
        } else {
            System.arraycopy(newLevels, 0, levels, 0, levels.size)
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w <= 0 || h <= 0) return

        val barCount = levels.size
        barSpacingPx = w / (barCount * 2f)          // 50% of bar width
        barWidthPx = (w - barSpacingPx * (barCount + 1)) / barCount.toFloat()
        barWidthPx = max(barWidthPx, 2f)

        gradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                0xFF22C55E.toInt(), // green
                0xFF0EA5E9.toInt(), // cyan/blue
                0xFF6366F1.toInt(), // indigo
                0xFFF97316.toInt()  // orange
            ),
            floatArrayOf(0f, 0.33f, 0.66f, 1f),
            Shader.TileMode.CLAMP
        )
        barPaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barCount = levels.size
        val heightF = height.toFloat()
        if (barCount == 0 || width == 0 || height == 0) return

        var x = barSpacingPx

        for (i in 0 until barCount) {
            val level = levels[i].coerceIn(0f, 1f)

            val barHeight = 8f + (heightF - 16f) * level
            val top = heightF - barHeight
            val bottom = heightF.toFloat()

            barRect.set(
                x,
                top,
                x + barWidthPx,
                bottom
            )

            canvas.drawRoundRect(barRect, barWidthPx / 2f, barWidthPx / 2f, barPaint)

            x += barWidthPx + barSpacingPx
        }
    }
}