package com.example.duetly.dialogs

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.Random

class EqualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val random = Random()
    private val barCount = 4
    private val bars = FloatArray(barCount)
    private val targetBars = FloatArray(barCount)
    private val animators = Array(barCount) { ValueAnimator() }
    private val handler = Handler(Looper.getMainLooper())

    init {
        paint.color = Color.parseColor("#0B0113")
        setupAnimators()
        startEndlessAnimation()
    }

    private fun setupAnimators() {
        for (i in 0 until barCount) {
            animators[i] = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 350 // Зменшено тривалість анімації до 500 мс (було 1000 мс)
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    bars[i] = animator.animatedValue as Float
                    invalidate()
                }
            }
        }
    }

    private fun startEndlessAnimation() {
        animateBars()
        handler.postDelayed({ startEndlessAnimation() }, 350) // Запускаємо нову анімацію кожні 500 мс
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barWidth = width / barCount.toFloat()
        val padding = barWidth * 0.2f

        for (i in 0 until barCount) {
            val left = i * barWidth + padding
            val top = height - (height * bars[i])
            val right = (i + 1) * barWidth - padding
            val bottom = height.toFloat()

            canvas.drawRect(left, top, right, bottom, paint)
        }
    }

    private fun animateBars() {
        for (i in 0 until barCount) {
            targetBars[i] = random.nextFloat()
            animators[i].setFloatValues(bars[i], targetBars[i])
            animators[i].start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 60.dpToPx(context) // Змінено з 80 на 60
        val desiredHeight = 60.dpToPx(context) // Змінено з 80 на 60

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}

// Функція розширення для конвертації dp в пікселі
fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()