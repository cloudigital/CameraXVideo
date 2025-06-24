package com.dynamsoft.cameraxvideo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.*

class BerlinClockView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Màu các ô vuông
    private val colorOff = Color.DKGRAY
    private val colorHour = Color.RED
    private val colorMinute = Color.YELLOW
    private val colorQuarter = Color.rgb(255, 165, 0)
    private val colorSecond = Color.RED

    // Định nghĩa trạng thái đồng hồ
    private var calendar: Calendar = Calendar.getInstance()

    // Callback cho các nút chức năng
    var onToggleRecord: (() -> Unit)? = null
    var onToggleCamera: (() -> Unit)? = null

    // Xác định vị trí 2 nút chức năng đặc biệt
    private val startStopIndex = 4      // ví dụ: ô thứ 4 trong hàng phút
    private val switchCamIndex = 0      // ví dụ: ô đầu tiên hàng giờ đơn

    init {
        isClickable = true
        setOnClickListener { x ->
            val index = hitTest(lastTouchX, lastTouchY)
            if (index == startStopIndex) onToggleRecord?.invoke()
            if (index == 100 + switchCamIndex) onToggleCamera?.invoke()
        }
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        if (event != null) {
            lastTouchX = event.x
            lastTouchY = event.y
        }
        return super.onTouchEvent(event)
    }

    fun updateTime() {
        calendar = Calendar.getInstance()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        val padding = 16f
        val blockSpacing = 8f

        val blockWidth = (width - padding * 2 - blockSpacing * 10) / 11
        val blockHeight = blockWidth

        var top = padding

        // Row 1: Giây (1 ô tròn ở giữa)
        val secLight = if (calendar.get(Calendar.SECOND) % 2 == 0) colorSecond else colorOff
        paint.color = secLight
        val cx = width / 2
        val cy = top + blockHeight / 2
        canvas.drawCircle(cx, cy, blockHeight / 2, paint)

        top += blockHeight + blockSpacing

        // Row 2: 4 ô giờ 5h
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val hour5s = hours / 5
        for (i in 0 until 4) {
            paint.color = if (i < hour5s) colorHour else colorOff
            val left = padding + i * (blockWidth + blockSpacing)
            canvas.drawRect(left, top, left + blockWidth, top + blockHeight, paint)
        }

        top += blockHeight + blockSpacing

        // Row 3: 4 ô giờ đơn
        val hour1s = hours % 5
        for (i in 0 until 4) {
            paint.color = if (i < hour1s) colorHour else colorOff
            val left = padding + i * (blockWidth + blockSpacing)
            canvas.drawRect(left, top, left + blockWidth, top + blockHeight, paint)
        }

        top += blockHeight + blockSpacing

        // Row 4: 11 ô phút 5'
        val minutes = calendar.get(Calendar.MINUTE)
        val min5s = minutes / 5
        for (i in 0 until 11) {
            paint.color = when {
                i >= min5s -> colorOff
                i == 2 || i == 5 || i == 8 -> colorQuarter
                else -> colorMinute
            }
            val left = padding + i * (blockWidth + blockSpacing)
            canvas.drawRect(left, top, left + blockWidth, top + blockHeight, paint)
        }

        top += blockHeight + blockSpacing

        // Row 5: 4 ô phút đơn
        val min1s = minutes % 5
        for (i in 0 until 4) {
            paint.color = if (i < min1s) colorMinute else colorOff
            val left = padding + i * (blockWidth + blockSpacing)
            canvas.drawRect(left, top, left + blockWidth, top + blockHeight, paint)
        }
    }

    // Hit test cho việc click nút chức năng
    private fun hitTest(x: Float, y: Float): Int {
        val width = width.toFloat()
        val padding = 16f
        val blockSpacing = 8f
        val blockWidth = (width - padding * 2 - blockSpacing * 10) / 11
        val blockHeight = blockWidth

        // xác định click vào ô nào (row và column)
        var row = ((y - padding) / (blockHeight + blockSpacing)).toInt()
        val col = ((x - padding) / (blockWidth + blockSpacing)).toInt()

        return when (row) {
            3 -> col    // Hàng phút 5' (11 ô)
            2 -> 100 + col  // Hàng giờ đơn (4 ô) → gắn prefix 100
            else -> -1
        }
    }
}
