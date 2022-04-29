package com.dynamsoft.cameraxvideo

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import com.dynamsoft.dbr.BarcodeReader
import com.dynamsoft.dbr.EnumImagePixelFormat
import com.dynamsoft.dbr.EnumPresetTemplate
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class LiveScanActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var reader: BarcodeReader
    private val zxingReader = MultiFormatReader()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_scan)
        val decorView: View = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        initDBR()
        bindCameraUseCases()
    }

    private fun initDBR(){
        reader = BarcodeReader()
        reader.updateRuntimeSettings(EnumPresetTemplate.VIDEO_SINGLE_BARCODE)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener ({

            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()
            val orientation = baseContext.resources.configuration.orientation

            var previewView = findViewById<PreviewView>(R.id.livePreviewView)
            previewView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    dimensionRatio = "V,9:16"
                } else {
                    dimensionRatio = "H,16:9"
                }
            }
            val targetResolution = intent.getStringExtra("resolution")
            var targetSDK = "DBR"
            if (intent.hasExtra("SDK")) {
                targetSDK = intent.getStringExtra("SDK")!!
            }

            var targetDuration = intent.getIntExtra("duration",10)
            var targetWidth:Int = 1280
            var targetHeight:Int = 720
            if (targetResolution == "720P") {
                targetWidth = 1280
                targetHeight = 720
            }else if (targetResolution == "1080P") {
                targetWidth = 1920
                targetHeight = 1080
            }else if (targetResolution == "4K") {
                targetWidth = 3840
                targetHeight = 2160
            }

            var resolution:Size
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                resolution = Size(targetHeight,targetWidth)
            } else {
                resolution = Size(targetWidth,targetHeight)
            }

            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                .setTargetResolution(resolution)
                .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()


            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                Log.d("DBR","get image "+image.width+"x"+image.height)
                decodeImage(image,targetSDK)
                image.close()
            })

            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageAnalysis)

            // Use the camera object to link our preview use case with the view
            preview.setSurfaceProvider(previewView.surfaceProvider)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun decodeImage(image:ImageProxy,SDK:String){
        val buffer: ByteBuffer = image.planes[0].buffer
        val nRowStride = image.planes[0].rowStride
        val nPixelStride = image.planes[0].pixelStride
        val length: Int = buffer.remaining()
        val bytes = ByteArray(length)
        buffer.get(bytes)
        if (SDK == "DBR") {

            val results = reader.decodeBuffer(bytes,image.width,image.height,nRowStride*nPixelStride,EnumImagePixelFormat.IPF_NV21)
            if (results.size>0) {
                Log.d("DBR","found barcodes")
            }
        }else{
            val source = PlanarYUVLuminanceSource(
                bytes,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val rawResult = zxingReader.decode(binaryBitmap)
                Log.d("DBR","zxing found barcodes")
            } catch (e: NotFoundException) {
                e.printStackTrace()
            }
        }

    }
}