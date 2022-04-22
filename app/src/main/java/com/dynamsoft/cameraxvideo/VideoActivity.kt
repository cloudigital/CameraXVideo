package com.dynamsoft.cameraxvideo

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.io.InputStream
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask
import com.dynamsoft.dbr.*
import java.lang.StringBuilder


class VideoActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var resultTextView: TextView
    private lateinit var reader: BarcodeReader
    private lateinit var decodeButton: Button
    private lateinit var uri:Uri
    private var framesWithBarcodeFound = 0
    private var framesProcessed = 0
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
            }
        }
        resultTextView = findViewById(R.id.resultTextView)
        decodeButton = findViewById<Button>(R.id.decodeButton)
        decodeButton.setOnClickListener {
            if (decodeButton.text == "Stop") {
                decodeButton.text = "Decode"
                if (videoView.isPlaying){
                    videoView.stopPlayback()
                    videoView.setVideoURI(uri)
                }
            }else{
                framesProcessed = 0
                framesWithBarcodeFound = 0
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
    }

    private fun initDBR(){
        BarcodeReader.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9"
        ) { isSuccessful, e ->
            if (!isSuccessful) {
                e.printStackTrace()
            }
        }
        reader = BarcodeReader()
    }

    private fun updateResults(textResults:Array<TextResult>,currentPosition:Int) {
        val sb:StringBuilder = StringBuilder()
        sb.append(currentPosition).append("/").append(videoView.duration).append("\n")
        sb.append(generalDecodingResults(textResults))
        resultTextView.setText(sb.toString())
    }

    private fun updateResults(textResults:Array<TextResult>,totalFrames:Int,currentFrameIndex:Int) {
        val sb:StringBuilder = StringBuilder()
        sb.append(currentFrameIndex+1).append("/").append(totalFrames).append("\n")
        sb.append(generalDecodingResults(textResults))
        resultTextView.setText(sb.toString())
    }

    private fun generalDecodingResults(textResults:Array<TextResult>):String {
        val sb:StringBuilder = StringBuilder()
        sb.append("frames processed: ").append(framesProcessed).append("\n")
        sb.append("frames with barcodes found: ").append(framesWithBarcodeFound).append("\n")
        if (textResults.size>0){
            for (tr in textResults) {
                sb.append(tr.barcodeText)
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
                    decodeButton.setText("Stop")
                    if (which == 0) {
                        decodeEveryFrame()
                    }else{
                        decodeVideo()
                    }
                })
        builder.create().show()
    }


    private fun decodeEveryFrame() {
        imageView.visibility = View.VISIBLE
        videoView.visibility = View.INVISIBLE
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val frameGrabber = FFmpegFrameGrabber(inputStream)
        frameGrabber.start()
        val totalFrames = frameGrabber.lengthInVideoFrames
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
                val textResults = reader.decodeBufferedImage(bm)
                if (textResults.size>0) {
                    framesWithBarcodeFound++
                }

                runOnUiThread {
                    updateResults(textResults,totalFrames,i)
                    imageView.setImageBitmap(bm)
                }
            }
            runOnUiThread {
                decodeButton.text = "Decode"
            }

        }
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
        val timer = Timer()
        timer.scheduleAtFixedRate(timerTask {
            if (decodeButton.text != "Stop") {
                timer.cancel()
            }else{
                if (videoView.isPlaying) {
                    val position = videoView.currentPosition
                    val bm = captureVideoFrame(mmRetriever,position)
                    framesProcessed++
                    val textResults = reader.decodeBufferedImage(bm)
                    if (textResults.size>0) {
                        framesWithBarcodeFound++
                    }
                    runOnUiThread {
                        updateResults(textResults,position)
                    }
                }
            }
        },100,2)
    }

    private fun captureVideoFrame(mmRetriever:MediaMetadataRetriever, currentPosition:Int):Bitmap?{
        val bm = mmRetriever.getFrameAtTime((currentPosition * 1000).toLong())
        Log.d("DBR","bm width: "+bm?.width)
        return bm
    }
}