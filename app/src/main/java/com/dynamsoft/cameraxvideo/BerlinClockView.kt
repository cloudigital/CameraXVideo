package com.dynamsoft.cameraxvideo

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager

class BerlinClockView : Activity() {

    private var isRecording = false
    private var isFrontCamera = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        actionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val clockView = BerlinClockView(this, null)
        setContentView(clockView)

        clockView.onToggleRecord = {
            isRecording = !isRecording
            // TODO: start/stop recording logic
        }

        clockView.onToggleCamera = {
            isFrontCamera = !isFrontCamera
            // TODO: switch camera logic
        }

        clockView.post(object : Runnable {
            override fun run() {
                clockView.updateTime()
                clockView.postDelayed(this, 1000)
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isRecording = !isRecording
            // TODO: start/stop recording logic
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
