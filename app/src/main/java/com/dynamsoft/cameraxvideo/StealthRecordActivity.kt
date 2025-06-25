package com.dynamsoft.cameraxvideo

import android.Manifest
import android.content.Context
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
import android.util.Log

class StealthRecordActivity : AppCompatActivity() {

    private var PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private lateinit var cameraSelector: CameraSelector
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var isRecording = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var saveGallery = true
    private var pendingSwitchCamera = false

    private lateinit var berlinClock: BerlinClockView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler(CrashLogger1(this))

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(R.layout.activity_stealth_record)
        berlinClock = findViewById(R.id.berlinClock)

        berlinClock.setRecordingState(false)
        berlinClock.setCameraState(lensFacing == CameraSelector.LENS_FACING_FRONT)

        berlinClock.onToggleRecord = {
            toggleRecording()
        }

        berlinClock.onToggleCamera = {
            handleToggleCamera()
        }

        if (!hasPermissions(this, *PERMISSIONS_REQUIRED)) {
            requestPermissions(PERMISSIONS_REQUIRED, 1001)
        } else {
            setupClock()
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                setupClock()
                startCamera()
            } else {
                Toast.makeText(this, "Cần cấp quyền Camera và Ghi âm để sử dụng", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setupClock() {
        val handler = Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                berlinClock.updateTime()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun handleToggleCamera() {
        if (isRecording) {
            pendingSwitchCamera = true
            recording?.stop()
        } else {
            switchCamera()
        }
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT
        else
            CameraSelector.LENS_FACING_BACK

        berlinClock.setCameraState(lensFacing == CameraSelector.LENS_FACING_FRONT)
        startCamera()
        toggleRecording()
    }

    private fun startCamera() {
        if (!hasPermissions(this, *PERMISSIONS_REQUIRED)) return

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
        if (!hasPermissions(this, *PERMISSIONS_REQUIRED)) return

        if (isRecording) {
            recording?.stop()
            isRecording = false
            vibrateStop()
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
                        if (pendingSwitchCamera) {
                            pendingSwitchCamera = false
                            switchCamera()
                        }
                    }
                }

            isRecording = true
            vibrateStart()
        }
        berlinClock.setRecordingState(isRecording)
    }

    private fun vibrateStart() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun vibrateStop() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 30, 50, 30) // delay, vibrate, pause, vibrate
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 30, 50, 30), -1)
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

class CrashLogger1(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(downloadsDir, "crash_log.txt")
            logFile.appendText(
                """----- ${Date()} -----
Thread: ${t.name}
Exception: ${e.message}
${Log.getStackTraceString(e)}
-----------------------------
"""
            )
        } catch (_: Exception) {
        }

        defaultHandler?.uncaughtException(t, e)
    }
}