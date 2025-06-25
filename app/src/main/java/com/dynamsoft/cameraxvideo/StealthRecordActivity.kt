package com.dynamsoft.cameraxvideo

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.*
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StealthRecordActivity : ComponentActivity() {

    private lateinit var cameraSelector: CameraSelector
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var isRecording = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var saveGallery = true
    
    private lateinit var berlinClock: BerlinClockView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        //setContentView(R.layout.activity_stealth_record)

        //--------------
        // Fullscreen flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        actionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        berlinClock = BerlinClockView(this, null)
        setContentView(berlinClock)
        //--------------
        //berlinClock = findViewById(R.id.berlinClock)

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
        
        
        //val recordButton = findViewById<ImageButton>(R.id.btn_record)
        if (isRecording) {
            recording?.stop()
            isRecording = false
            //recordButton.setBackgroundColor(getColor(android.R.color.holo_green_dark))
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
        } else {
            val filename = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"
            
            //Ghi ra thư mục riêng của app
            var publicDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            // Ghi ra Galery 
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
                        //Toast.makeText(this, "Saved: ${outputFile.absolutePath}", Toast.LENGTH_SHORT).show()

                        if (saveGallery){
                            // Gửi MediaScanner để hiện trong Gallery
                            val uri = Uri.fromFile(outputFile)
                            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                                data = uri
                            }
                            sendBroadcast(scanIntent)
                        }
                        
                    }
                }

            isRecording = true
            //recordButton.setBackgroundColor(getColor(android.R.color.holo_red_dark))
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
