package com.dynamsoft.cameraxvideo

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity() {
    private var currentRecording: Recording? = null
    private val FILENAMEFORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(this) }
    private var audioEnabled = false
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var context: Context;
    private lateinit var durationTextView: TextView;
    private var targetDuration = 10;
    private var targetQuality = Quality.HD;

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        context = this;
        durationTextView = findViewById<TextView>(R.id.durationTextView)
        val decorView: View = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        val resolution = intent.getStringExtra("resolution")
        targetDuration = intent.getIntExtra("duration",10)

        if (resolution == "720P") {
            targetQuality = Quality.HD
        }else if (resolution == "1080P") {
            targetQuality = Quality.FHD
        }else if (resolution == "4K") {
            targetQuality = Quality.UHD
        }

        runBlocking {
            bindCaptureUsecase()
        }
    }

    private suspend fun bindCaptureUsecase() {
        val cameraProvider = ProcessCameraProvider.getInstance(this).await()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        var previewView = findViewById<PreviewView>(R.id.previewView)
        previewView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = baseContext.resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                dimensionRatio = "V,9:16"
            }else{
                dimensionRatio = "H,16:9"
            }
        }

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

        // create the user required QualitySelector (video resolution): we know this is
        // supported, a valid qualitySelector will be created.
        var quality = targetQuality
        val qualitySelector = QualitySelector.from(quality)
        // build a recorder, which can:
        //   - record video/audio to MediaStore(only shown here), File, ParcelFileDescriptor
        //   - be used create recording(s) (the recording performs recording)
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                videoCapture,
                preview
            )
            startRecording();
        } catch (exc: Exception) {
            exc.printStackTrace()
            Toast.makeText(context,exc.localizedMessage,Toast.LENGTH_LONG).show()
            goBack()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAMEFORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            this.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording = videoCapture.output
            .prepareRecording(this, mediaStoreOutput)
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(mainThreadExecutor, captureListener)
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        val durationInNanos: Long = event.recordingStats.recordedDurationNanos;
        val durationInSeconds: Double = durationInNanos/1000/1000/1000.0
        Log.d("DBR","duration: "+durationInNanos)
        durationTextView.setText("Duration: "+durationInSeconds)
        if (durationInSeconds >= targetDuration) {
            if (currentRecording != null) {
                currentRecording!!.stop();
                Toast.makeText(context, "Saved", Toast.LENGTH_LONG).show()
                goBack();
            }
        }
        if (event is VideoRecordEvent.Finalize) {
            // display the captured video
        }
    }

    private fun goBack(){
        this.onBackPressed()
    }
}