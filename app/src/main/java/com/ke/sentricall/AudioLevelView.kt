package com.ke.sentricall

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class AudioLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111827")
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22C55E")
    }

    private val levels = ArrayDeque<Int>()
    private val maxBars = 24

    fun addLevel(level: Int) {
        val v = level.coerceIn(0, 100)
        if (levels.size >= maxBars) {
            levels.removeFirst()
        }
        levels.addLast(v)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // rounded background
        val radius = h / 2f
        canvas.drawRoundRect(0f, 0f, w, h, radius, radius, bgPaint)

        if (levels.isEmpty()) return

        val barWidth = w / max(maxBars * 1.5f, 1f)
        val gap = barWidth / 2f
        var x = gap

        for (value in levels) {
            val fraction = value / 100f
            val barHeight = (h - 8f) * fraction
            val top = h - 4f - barHeight
            canvas.drawRoundRect(
                x,
                top,
                x + barWidth,
                h - 4f,
                barWidth / 2f,
                barWidth / 2f,
                barPaint
            )
            x += barWidth + gap
        }
    }
}