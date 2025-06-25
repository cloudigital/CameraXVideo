package com.dynamsoft.cameraxvideo

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.*
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StealthRecordActivity : AppCompatActivity() {

    private lateinit var cameraSelector: CameraSelector
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var isRecording = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var saveGallery = true

    private lateinit var berlinClock: BerlinClockView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        
        Toast.makeText(this, "before setContentView", Toast.LENGTH_SHORT).show()
        setContentView(R.layout.activity_stealth_record)
        Toast.makeText(this, "after setContentView", Toast.LENGTH_SHORT).show()
        // Fullscreen flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        //berlinClock = BerlinClockView(this, null)
        //setContentView(berlinClock)
        
        Toast.makeText(this, "before findViewById", Toast.LENGTH_SHORT).show()
        berlinClock = findViewById(R.id.berlinClock)
        Toast.makeText(this, "after findViewById", Toast.LENGTH_SHORT).show()
        
        berlinClock.onToggleRecord = {
            toggleRecording()
        }

        berlinClock.onToggleCamera = {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK
            startCamera()
        }

        // Cập nhật đồng hồ mỗi giây
        val handler = Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                berlinClock.updateTime()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable)

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleRecording() {
        if (isRecording) {
            recording?.stop()
            isRecording = false
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
        } else {
            val filename = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"

            var publicDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            if (saveGallery)
                publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

            val outputFile = File(publicDir, "CameraX/$filename")
            outputFile.parentFile?.mkdirs()

            val outputOptions = FileOutputOptions.Builder(outputFile).build()

            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        if (saveGallery) {
                            val uri = Uri.fromFile(outputFile)
                            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                                data = uri
                            }
                            sendBroadcast(scanIntent)
                        }
                    }
                }

            isRecording = true
            Toast.makeText(this, "Timeup !", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            toggleRecording()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
