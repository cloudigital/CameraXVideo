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

class BerlinClockView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var onToggleRecord: (() -> Unit)? = null
    var onToggleCamera: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var calendar = Calendar.getInstance()

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

    private val hourTop = 4
    private val hourBottom = 4
    private val minuteTop = 11
    private val minuteBottom = 4

    private val startStopIndex = 3
    private val cameraToggleIndex = 0

    private var controlTextMap: MutableMap<Int, String> = mutableMapOf()

    fun updateTime() {
        calendar = Calendar.getInstance()
        invalidate()
    }

    fun setControlLabel(index: Int, label: String) {
        controlTextMap[index] = label
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return

        super.onDraw(canvas)
        val totalRows = 4
        val cellHeight = height / totalRows.toFloat()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        var y = 0f

        drawRow(canvas, hourTop, hour / 5, y, cellHeight, Color.RED, highlightIndex = cameraToggleIndex)
        y += cellHeight
        drawRow(canvas, hourBottom, hour % 5, y, cellHeight, Color.rgb(139, 0, 0))
        y += cellHeight
        drawRow(canvas, minuteTop, minute / 5, y, cellHeight, Color.YELLOW, Color.RED, Color.rgb(139, 0, 0), blinkIndex = (minute / 5 - 1).coerceAtLeast(0), blink = second % 2 == 0)
        y += cellHeight
        drawRow(canvas, minuteBottom, minute % 5, y, cellHeight, Color.YELLOW, secondaryColor = Color.rgb(139, 100, 0), highlightIndex = startStopIndex)

        tooltipText?.let {
            canvas.drawText(it, width / 2f, height / 2f, tooltipPaint)
        }
    }

    private fun drawRow(
        canvas: Canvas,
        count: Int,
        active: Int,
        y: Float,
        cellHeight: Float,
        primaryColor: Int,
        specialColor: Int? = null,
        secondaryColor: Int? = null,
        highlightIndex: Int = -1,
        blinkIndex: Int = -1,
        blink: Boolean = false
    ) {
        if (count == 0) return
        val cellWidth = width / count.toFloat()
        val margin = cellHeight * 0.1f

        for (i in 0 until count) {
            val color = when {
                i < active && specialColor != null && (i + 1) % 3 == 0 -> specialColor
                i < active -> primaryColor
                else -> secondaryColor ?: Color.DKGRAY
            }

            paint.color = color
            paint.style = Paint.Style.FILL

            val left = i * cellWidth + margin
            val right = (i + 1) * cellWidth - margin
            val top = y + margin
            val bottom = y + cellHeight - margin

            canvas.drawRect(left, top, right, bottom, paint)

            if (i == highlightIndex || (i == blinkIndex && blink)) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 8f
                canvas.drawRect(left, top, right, bottom, paint)
            }

            controlTextMap[i]?.let { text ->
                drawCenteredTextInCell(canvas, text, left, top, right, bottom)
            }
        }
    }

    private fun drawCenteredTextInCell(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = (bottom - top) * 0.5f
        }
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2 - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(text, centerX, centerY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val cellHeight = height / 4f
            if (event.y in 0f..cellHeight) {
                val cellWidth = width / hourTop.toFloat()
                val left = cameraToggleIndex * cellWidth
                val right = left + cellWidth
                if (event.x in left..right) {
                    showTooltip("Switch Camera")
                    onToggleCamera?.invoke()
                    return true
                }
            }
            if (event.y in 3 * cellHeight..4 * cellHeight) {
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
