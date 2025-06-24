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

    // Cấu hình số ô theo thiết kế đồng hồ Berlin
    private val hourTop = 4
    private val hourBottom = 4
    private val minuteTop = 11
    private val minuteBottom = 4

    // Ô điều khiển
    private val startStopIndex = 3       // phút hàng dưới (ô thứ 4)
    private val cameraToggleIndex = 0    // giờ hàng trên (ô đầu tiên)

    fun updateTime() {
        calendar = Calendar.getInstance()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalRows = 5
        val columns = 11 // hàng dài nhất là 11 ô

        val cellWidth = width / columns.toFloat()
        val cellHeight = height / totalRows.toFloat()

        var y = 0f

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // Giây - vòng tròn nhấp nháy ở giữa dòng đầu tiên
        paint.color = if (second % 2 == 0) Color.RED else Color.DKGRAY
        canvas.drawCircle(width / 2f, y + cellHeight / 2, cellHeight / 3, paint)
        y += cellHeight

        // Hàng giờ trên (ô 5h)
        for (i in 0 until hourTop) {
            paint.color = if (hour / 5 > i) Color.RED else Color.GRAY
            if (i == cameraToggleIndex) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
            } else {
                paint.style = Paint.Style.FILL
            }
            canvas.drawRect(
                i * cellWidth, y,
                i * cellWidth + cellWidth, y + cellHeight,
                paint
            )
        }
        y += cellHeight

        // Hàng giờ dưới (ô 1h)
        for (i in 0 until hourBottom) {
            paint.color = if (hour % 5 > i) Color.RED else Color.GRAY
            paint.style = Paint.Style.FILL
            canvas.drawRect(
                i * cellWidth, y,
                i * cellWidth + cellWidth, y + cellHeight,
                paint
            )
        }
        y += cellHeight

        // Hàng phút trên (ô 5p)
        for (i in 0 until minuteTop) {
            paint.color = when {
                minute / 5 > i && (i + 1) % 3 == 0 -> Color.RED
                minute / 5 > i -> Color.YELLOW
                else -> Color.GRAY
            }
            paint.style = Paint.Style.FILL
            canvas.drawRect(
                i * cellWidth, y,
                i * cellWidth + cellWidth, y + cellHeight,
                paint
            )
        }
        y += cellHeight

        // Hàng phút dưới (ô 1p)
        for (i in 0 until minuteBottom) {
            paint.color = if (minute % 5 > i) Color.YELLOW else Color.GRAY
            if (i == startStopIndex) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
            } else {
                paint.style = Paint.Style.FILL
            }
            canvas.drawRect(
                i * cellWidth, y,
                i * cellWidth + cellWidth, y + cellHeight,
                paint
            )
        }

        // Hiển thị tooltip nếu có
        tooltipText?.let {
            canvas.drawText(it, width / 2f, height / 2f, tooltipPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val totalRows = 5
            val columns = 11
            val cellWidth = width / columns.toFloat()
            val cellHeight = height / totalRows.toFloat()

            // Hàng phút dưới (start/stop): hàng 4
            val yMinStart = cellHeight * 4
            val yMaxStart = cellHeight * 5
            val xStart = startStopIndex * cellWidth
            val xEnd = xStart + cellWidth
            if (event.y in yMinStart..yMaxStart && event.x in xStart..xEnd) {
                showTooltip("Start/Stop Recording")
                onToggleRecord?.invoke()
                return true
            }

            // Hàng giờ trên (switch camera): hàng 1
            val yMinCam = cellHeight
            val yMaxCam = 2 * cellHeight
            val xCamStart = cameraToggleIndex * cellWidth
            val xCamEnd = xCamStart + cellWidth
            if (event.y in yMinCam..yMaxCam && event.x in xCamStart..xCamEnd) {
                showTooltip("Switch Camera")
                onToggleCamera?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
