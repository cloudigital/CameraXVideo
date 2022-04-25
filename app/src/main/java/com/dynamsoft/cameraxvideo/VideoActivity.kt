package com.dynamsoft.cameraxvideo

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.dynamsoft.dbr.BarcodeReader
import com.dynamsoft.dbr.EnumBinarizationMode
import com.dynamsoft.dbr.EnumLocalizationMode
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask


class VideoActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var resultTextView: TextView
    private lateinit var reader: BarcodeReader
    private lateinit var decodeButton: Button
    private lateinit var sdkSpinner: Spinner
    private lateinit var uri:Uri
    private var decoding = false
    private var framesWithBarcodeFound = 0
    private var framesProcessed = 0
    private var firstDecodedFrameIndex = -1
    private var firstBarcodeFoundPosition = -1
    private var firstFoundResult = ""
    private val sdkList = arrayOf<String?>("DBR", "ZXing")
    private var currentSDKIndex = 0
    private var benchmarkMode = false
    private lateinit var framesModeResult:FramesModeResult
    private lateinit var videoModeResult:VideoModeResult
    private var benchmarkResult = HashMap<String,SDKResult>()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        initDBR();
        imageView = findViewById(R.id.imageView)
        videoView = findViewById(R.id.videoView)
        videoView.setOnCompletionListener {
            if (decodeButton.text == "Stop") {
                decodeButton.text = "Decode"
                if (benchmarkMode == true) {
                    decodeButton.setText("Stop")
                    decodeEveryFrame()
                }
            }
        }
        resultTextView = findViewById(R.id.resultTextView)
        sdkSpinner = findViewById<Spinner>(R.id.spinner)

        val mArrayAdapter = ArrayAdapter<Any?>(this, R.layout.spinner_list, sdkList)
        mArrayAdapter.setDropDownViewResource(R.layout.spinner_list)
        sdkSpinner.adapter = mArrayAdapter
        val benchmarkButton = findViewById<Button>(R.id.benchmarkButton)
        benchmarkButton.setOnClickListener {
            benchmark()
        }
        decodeButton = findViewById<Button>(R.id.decodeButton)
        decodeButton.setOnClickListener {
            if (decodeButton.text == "Stop") {
                decodeButton.text = "Decode"
                if (videoView.isPlaying){
                    videoView.stopPlayback()
                    videoView.setVideoURI(uri)
                }
            }else{
                resetStats()
                showDialog()
            }
        }

        uri = Uri.parse(intent.getStringExtra("uri"))
        videoView.setVideoURI(uri)
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val frameGrabber = FFmpegFrameGrabber(inputStream)
        frameGrabber.start()
        val frame = frameGrabber.grabFrame()
        imageView.setImageBitmap(rotateBitmaptoFitScreen(AndroidFrameConverter().convert(frame)))
        frameGrabber.close()

        if (intent.hasExtra("automation")) {
            benchmark()
        }
    }

    private fun resetStats(){
        framesProcessed = 0
        framesWithBarcodeFound = 0
        firstBarcodeFoundPosition = -1
        firstDecodedFrameIndex = -1
        firstFoundResult = ""
    }

    private fun benchmark() {
        resetStats()
        benchmarkMode = true
        sdkSpinner.setSelection(currentSDKIndex)
        decodeButton.setText("Stop")
        decodeVideo()
    }

    private fun initDBR(){
        BarcodeReader.initLicense("DLS2eyJoYW5kc2hha2VDb2RlIjoiMjAwMDAxLTE2NDk4Mjk3OTI2MzUiLCJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSIsInNlc3Npb25QYXNzd29yZCI6IndTcGR6Vm05WDJrcEQ5YUoifQ=="
        ) { isSuccessful, e ->
            if (!isSuccessful) {
                e.printStackTrace()
            }
        }
        reader = BarcodeReader()
        var settings = reader.runtimeSettings
        settings.deblurLevel = 9;
        settings.minResultConfidence = 0;
        settings.expectedBarcodesCount = 1
        settings.localizationModes = intArrayOf(
            EnumLocalizationMode.LM_CONNECTED_BLOCKS
        )
        settings.binarizationModes = intArrayOf(EnumBinarizationMode.BM_LOCAL_BLOCK, 0, 0, 0, 0, 0, 0, 0)
        reader.updateRuntimeSettings(settings)
        reader.setModeArgument("BinarizationModes",0,"BlockSizeX","71")
        reader.setModeArgument("BinarizationModes",0,"BlockSizeY","71")
        reader.setModeArgument("BinarizationModes",0,"EnableFillBinaryVacancy","0")
    }



    private fun decodeBitmap(bm:Bitmap,selectedPosition:Int):ArrayList<String> {
        val results:ArrayList<String> = ArrayList<String>()
        if (selectedPosition == 0) {
            val textResults = reader.decodeBufferedImage(bm)
            for (tr in textResults) {
                results.add(tr.barcodeText)
                Log.d("DBR","confidence: "+tr.results[0].confidence)
            }
        }else{
            var multiFormatReader = MultiFormatReader();

            val width: Int = bm.getWidth()
            val height: Int = bm.getHeight()

            val pixels = IntArray(width * height)
            bm.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)

            if (source != null) {
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                try {
                    val rawResult = multiFormatReader.decodeWithState(binaryBitmap)
                    results.add(rawResult.text)
                } catch (re: ReaderException) {
                    re.printStackTrace()
                } finally {
                    multiFormatReader.reset()
                }
            }
        }
        return results
    }

    private fun updateResults(textResults:ArrayList<String>,currentPosition:Int,elapsedTime:Long) {
        val sb:StringBuilder = StringBuilder()
        sb.append(currentPosition).append("/").append(videoView.duration).append("\n")
        sb.append("frame reading time: ").append(elapsedTime).append("ms").append("\n")
        sb.append(generalDecodingResults(textResults))
        resultTextView.setText(sb.toString())
    }

    private fun updateResults(textResults:ArrayList<String>,totalFrames:Int,currentFrameIndex:Int,elapsedTime:Long) {
        val sb:StringBuilder = StringBuilder()
        sb.append(currentFrameIndex+1).append("/").append(totalFrames).append("\n")
        sb.append("frame reading time: ").append(elapsedTime).append("ms").append("\n")
        sb.append(generalDecodingResults(textResults))
        resultTextView.setText(sb.toString())
    }

    private fun generalDecodingResults(textResults:ArrayList<String>):String {
        val sb:StringBuilder = StringBuilder()
        sb.append("frames processed: ").append(framesProcessed).append("\n")
        sb.append("frames with barcodes found: ").append(framesWithBarcodeFound).append("\n")

        if (firstDecodedFrameIndex != -1){
            sb.append("first decoded frame index: ").append(firstDecodedFrameIndex).append("\n")
        }

        if (firstBarcodeFoundPosition != -1){
            sb.append("first barcode found position: ").append(firstBarcodeFoundPosition).append("\n")
        }

        if (firstFoundResult != "") {
            sb.append("first barcode result: ").append(firstFoundResult).append("\n")
        }

        if (textResults.size>0){
            for (tr in textResults) {
                sb.append(tr)
                sb.append("\n")
            }
        }else{
            sb.append("No barcodes found").append("\n")
        }
        return sb.toString()
    }

    private fun showDialog() {
        val options = arrayOf<String>("Decode every frame", "Play and decode")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick a video")
            .setItems(options,
                DialogInterface.OnClickListener { dialog, which ->
                    startDecoding(which)
                })
        builder.create().show()
    }

    private fun startDecoding(which:Int){
        decodeButton.setText("Stop")
        if (which == 0) {
            decodeEveryFrame()
        }else {
            decodeVideo()
        }
    }

    private fun decodeEveryFrame() {
        imageView.visibility = View.VISIBLE
        videoView.visibility = View.INVISIBLE
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val frameGrabber = FFmpegFrameGrabber(inputStream)
        frameGrabber.start()

        val decodingResults = ArrayList<FrameDecodingResult>()

        //val totalFrames = frameGrabber.lengthInVideoFrames
        val totalFrames = 2
        val th = thread(start=true) {
            for (i in 0..totalFrames-1) {
                Thread.sleep(50)
                if (decodeButton.text != "Stop") {
                    break
                }

                val frame = frameGrabber.grabFrame()
                var bm = AndroidFrameConverter().convert(frame)
                bm = rotateBitmaptoFitScreen(bm)

                framesProcessed++
                val startTime = System.currentTimeMillis()
                val textResults = decodeBitmap(bm,sdkSpinner.selectedItemPosition)
                val endTime = System.currentTimeMillis()
                var frameDecodingResult = FrameDecodingResult(textResults,endTime - startTime)
                decodingResults.add(frameDecodingResult)

                if (textResults.size>0) {
                    framesWithBarcodeFound++
                    if (firstDecodedFrameIndex == -1) {
                        firstDecodedFrameIndex = i
                        firstFoundResult = textResults[0]
                    }
                }

                runOnUiThread {
                    updateResults(textResults,totalFrames,i,endTime - startTime)
                    imageView.setImageBitmap(bm)
                }

            }
            framesModeResult = getFrameModeStatistics(decodingResults)
            runOnUiThread {
                decodeButton.text = "Decode"
                if (benchmarkMode == true) {
                    val SDKResult = SDKResult(framesModeResult,videoModeResult)
                    benchmarkResult.put(sdkSpinner.selectedItem.toString(),SDKResult)
                    if (currentSDKIndex != sdkList.size - 1) {
                        currentSDKIndex = currentSDKIndex + 1
                        benchmark()
                    }else{
                        val string = Json.encodeToString(benchmarkResult)
                        Toast.makeText(this,string,Toast.LENGTH_LONG).show()
                        Log.d("DBR",string)
                        val pattern = "yyyy-MM-dd-HH-mm-ss-SSS"
                        val simpleDateFormat = SimpleDateFormat(pattern)
                        val date: String = simpleDateFormat.format(Date())
                        val outputPath = writeStringAsFile(string,date+".json")
                        if (intent.hasExtra("automation")) {
                            val f = File(outputPath)
                            if (f.exists()) {
                                val resultData = Intent()
                                resultData.putExtra("outputPath",outputPath)
                                this.setResult(RESULT_OK, resultData);
                            }
                            this.finish()
                        }
                    }
                }

            }
        }
    }

    private fun writeStringAsFile(fileContents: String?, fileName: String):String {
        try {
            val externalFilesPath = getExternalFilesDir("")?.absolutePath
            var outputPath = externalFilesPath + "/" + fileName
            val out = FileWriter(File(outputPath))
            out.write(fileContents)
            out.close()
            return outputPath
        } catch (e: IOException) {
        }
        return ""
    }

    private fun getVideoModeStatistics(decodingResults:HashMap<Int,FrameDecodingResult>):VideoModeResult {
        val result = VideoModeResult(decodingResults,framesWithBarcodeFound,framesProcessed,firstBarcodeFoundPosition,firstFoundResult)
        return result
    }

    private fun getFrameModeStatistics(decodingResults:ArrayList<FrameDecodingResult>):FramesModeResult {
        val result = FramesModeResult(decodingResults,framesWithBarcodeFound,framesProcessed,firstDecodedFrameIndex,firstFoundResult)
        return result
    }

    private fun rotateBitmaptoFitScreen(bm:Bitmap):Bitmap {
        if (baseContext.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (bm.width>bm.height) {
                return rotateBitmap(bm,90f)
            }
        }
        return bm
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    private fun decodeVideo(){
        val mmRetriever = MediaMetadataRetriever()
        mmRetriever.setDataSource(this,uri)
        imageView.visibility = View.INVISIBLE
        videoView.visibility = View.VISIBLE
        videoView.start()

        val decodingResults = HashMap<Int,FrameDecodingResult>()

        val timer = Timer()
        timer.scheduleAtFixedRate(timerTask {
            if (decodeButton.text != "Stop") {
                timer.cancel()
            }else{
                try {
                    if (videoView.isPlaying) {
                        val position = videoView.currentPosition

                        val bm = captureVideoFrame(mmRetriever,position)
                        framesProcessed++
                        if (decoding == false) {
                            decoding = true
                            val startTime = System.currentTimeMillis()
                            val textResults = decodeBitmap(bm!!, sdkSpinner.selectedItemPosition)
                            val endTime = System.currentTimeMillis()
                            var frameDecodingResult = FrameDecodingResult(textResults,endTime - startTime)
                            decodingResults.put(position, frameDecodingResult)

                            decoding = false
                            if (textResults.size>0) {
                                framesWithBarcodeFound++
                                if (firstBarcodeFoundPosition == -1) {
                                    firstBarcodeFoundPosition = position
                                    firstFoundResult = textResults[0]
                                }
                            }
                            videoModeResult = getVideoModeStatistics(decodingResults)
                            runOnUiThread {
                                updateResults(textResults,position,endTime - startTime)
                            }
                        }
                    }
                }catch (exc:Exception) {
                    exc.printStackTrace()
                }
            }
        },100,2)
    }

    //https://stackoverflow.com/questions/5278707/videoview-getdrawingcache-is-returning-black
    private fun captureVideoFrame(mmRetriever:MediaMetadataRetriever, currentPosition:Int):Bitmap?{
        val bm = mmRetriever.getFrameAtTime((currentPosition * 1000).toLong())
        return bm
    }
}