package com.dynamsoft.cameraxvideo

import android.os.*
import android.view.KeyEvent
import android.widget.ImageButton
import android.widget.Toast
import android.content.Intent            
import android.content.pm.ActivityInfo
import android.net.Uri                    
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StealthRecordActivity : ComponentActivity() {

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var isRecording = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var saveGallery = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_stealth_record)

        findViewById<ImageButton>(R.id.btn_toggle_camera).setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            startCamera()
        }

        findViewById<ImageButton>(R.id.btn_record).setOnClickListener {
            toggleRecording()
        }

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

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, videoCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleRecording() {
        
        
        val recordButton = findViewById<ImageButton>(R.id.btn_record)
        if (isRecording) {
            recording?.stop()
            isRecording = false
            recordButton.setBackgroundColor(getColor(android.R.color.holo_green_dark))
        } else {
            val filename = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"
            
            //Ghi ra thư mục riêng của app
            var publicDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            // Ghi ra Galery 
            if (saveGallery) publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            
            
            val outputFile = File(publicDir, "CameraX/$filename")
            outputFile.parentFile?.mkdirs()
        
            val outputOptions = FileOutputOptions.Builder(outputFile).build()

            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        Toast.makeText(this, "Saved: ${outputFile.absolutePath}", Toast.LENGTH_SHORT).show()

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
            recordButton.setBackgroundColor(getColor(android.R.color.holo_red_dark))
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
