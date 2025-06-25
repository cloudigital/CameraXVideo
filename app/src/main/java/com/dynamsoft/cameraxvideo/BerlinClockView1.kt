package com.dynamsoft.cameraxvideo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*

class BerlinClockView_bk(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var onToggleRecord: (() -> Unit)? = null
    var onToggleCamera: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var calendar = Calendar.getInstance()

    // Tooltip
    private var tooltipText: String? = null
    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }

    private val tooltipHandler = Handler(Looper.getMainLooper())

    private fun showTooltip(text: String) {
        tooltipText = text
        invalidate()
        tooltipHandler.removeCallbacksAndMessages(null)
        tooltipHandler.postDelayed({
            tooltipText = null
            invalidate()
        }, 2000)
    }

    // Số lượng ô từng hàng
    private val hourTop = 4
    private val hourBottom = 4
    private val minuteTop = 11
    private val minuteBottom = 4

    // Ô điều khiển
    private val startStopIndex = 3
    private val cameraToggleIndex = 0

    fun updateTime() {
        calendar = Calendar.getInstance()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalRows = 5
        val cellHeight = height / 6f
        val margin = cellHeight * 0.1f
        val fullWidth = width.toFloat()

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // Row 0: Seconds Circle
        paint.color = if (second % 2 == 0) Color.YELLOW else Color.DKGRAY
        canvas.drawCircle(width / 2f, cellHeight / 2, cellHeight / 2.5f, paint)

        var y = cellHeight

        // Row 1: Top Hours (4)
        drawRow(canvas, hourTop, hour / 5, y, Color.RED, highlightIndex = cameraToggleIndex)
        y += cellHeight

        // Row 2: Bottom Hours (4)
        drawRow(canvas, hourBottom, hour % 5, y, Color.rgb(139, 0, 0))
        y += cellHeight

        // Row 3: Top Minutes (11)
        drawRow(canvas, minuteTop, minute / 5, y, Color.YELLOW, Color.RED, Color.rgb(139, 0, 0))
        y += cellHeight

        // Row 4: Bottom Minutes (4)
        drawRow(canvas, minuteBottom, minute % 5, y, Color.YELLOW, secondaryColor = Color.rgb(139, 100, 0), highlightIndex = startStopIndex)

        // Tooltip
        tooltipText?.let {
            canvas.drawText(it, width / 2f, height / 2f, tooltipPaint)
        }
    }

    private fun drawRow(
        canvas: Canvas,
        count: Int,
        active: Int,
        y: Float,
        primaryColor: Int,
        specialColor: Int? = null,
        secondaryColor: Int? = null,
        highlightIndex: Int = -1
    ) {
        val cellWidth = width / count.toFloat()
        val height = height / 6f
        val margin = height * 0.1f

        for (i in 0 until count) {
            val color = when {
                i < active && specialColor != null && (i + 1) % 3 == 0 -> specialColor
                i < active -> primaryColor
                else -> secondaryColor ?: Color.DKGRAY
            }
            paint.color = color
            paint.style = if (i == highlightIndex) Paint.Style.STROKE else Paint.Style.FILL
            paint.strokeWidth = if (i == highlightIndex) 8f else 0f

            val left = i * cellWidth + margin
            val right = (i + 1) * cellWidth - margin
            val top = y + margin
            val bottom = y + height - margin
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val cellHeight = height / 6f

            // Row 1 - camera toggle
            if (event.y in cellHeight..2 * cellHeight) {
                val cellWidth = width / hourTop.toFloat()
                val left = cameraToggleIndex * cellWidth
                val right = left + cellWidth
                if (event.x in left..right) {
                    showTooltip("Switch Camera")
                    onToggleCamera?.invoke()
                    return true
                }
            }

            // Row 5 - start/stop
            if (event.y in 5 * cellHeight..6 * cellHeight) {
                val cellWidth = width / minuteBottom.toFloat()
                val left = startStopIndex * cellWidth
                val right = left + cellWidth
                if (event.x in left..right) {
                    showTooltip("Start/Stop Recording")
                    onToggleRecord?.invoke()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
