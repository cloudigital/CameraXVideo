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

    private val padding = 20f
    private val spacing = 10f

    // Ô điều khiển
    private val startStopIndex = 3       // phút hàng dưới (ô thứ 4)
    private val cameraToggleIndex = 0    // giờ hàng trên (ô đầu tiên)

    fun updateTime() {
        calendar = Calendar.getInstance()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthUnit = (width - padding * 2 - spacing * 10) / 11
        val heightUnit = widthUnit

        val top = padding
        var y = top

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // Giây - vòng tròn nhấp nháy
        paint.color = if (second % 2 == 0) Color.RED else Color.DKGRAY
        canvas.drawCircle(width / 2f, y + heightUnit / 2, heightUnit / 2.5f, paint)
        y += heightUnit + spacing

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
                padding + i * (widthUnit + spacing), y,
                padding + i * (widthUnit + spacing) + widthUnit, y + heightUnit,
                paint
            )
        }
        y += heightUnit + spacing

        // Hàng giờ dưới (ô 1h)
        for (i in 0 until hourBottom) {
            paint.color = if (hour % 5 > i) Color.RED else Color.GRAY
            paint.style = Paint.Style.FILL
            canvas.drawRect(
                padding + i * (widthUnit + spacing), y,
                padding + i * (widthUnit + spacing) + widthUnit, y + heightUnit,
                paint
            )
        }
        y += heightUnit + spacing

        // Hàng phút trên (ô 5p)
        for (i in 0 until minuteTop) {
            paint.color = when {
                minute / 5 > i && (i + 1) % 3 == 0 -> Color.RED // đánh dấu 15, 30, 45 phút
                minute / 5 > i -> Color.YELLOW
                else -> Color.GRAY
            }
            paint.style = Paint.Style.FILL
            canvas.drawRect(
                padding + i * (widthUnit + spacing), y,
                padding + i * (widthUnit + spacing) + widthUnit, y + heightUnit,
                paint
            )
        }
        y += heightUnit + spacing

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
                padding + i * (widthUnit + spacing), y,
                padding + i * (widthUnit + spacing) + widthUnit, y + heightUnit,
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
            val yUnit = (width - padding * 2 - spacing * 10) / 11 + spacing

            // Tọa độ hàng phút dưới (start/stop)
            val bottomY = padding + yUnit * 4
            val topY = bottomY - yUnit

            for (i in 0 until minuteBottom) {
                val left = padding + i * (yUnit - spacing)
                val right = left + (yUnit - spacing)
                if (i == startStopIndex && event.y in topY..bottomY && event.x in left..right) {
                    showTooltip("Start/Stop Recording")
                    onToggleRecord?.invoke()
                    return true
                }
            }

            // Tọa độ hàng giờ trên (switch camera)
            val cameraY = padding + yUnit
            val cameraBottom = cameraY + (yUnit - spacing)

            for (i in 0 until hourTop) {
                val left = padding + i * (yUnit - spacing)
                val right = left + (yUnit - spacing)
                if (i == cameraToggleIndex && event.y in cameraY..cameraBottom && event.x in left..right) {
                    showTooltip("Switch Camera")
                    onToggleCamera?.invoke()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
