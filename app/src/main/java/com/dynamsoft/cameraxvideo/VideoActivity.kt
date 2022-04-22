package com.dynamsoft.cameraxvideo

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.io.InputStream
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask


class VideoActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var uri:Uri
    private var currentIndex = 0
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        imageView = findViewById(R.id.imageView)
        videoView = findViewById(R.id.videoView)

        var decodeButton = findViewById<Button>(R.id.decodeButton)
        decodeButton.setOnClickListener {
            showDialog()
        }

        uri = Uri.parse(intent.getStringExtra("uri"))
        videoView.setVideoURI(uri)
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val frameGrabber = FFmpegFrameGrabber(inputStream)
        frameGrabber.start()
        val frame = frameGrabber.grabFrame()
        imageView.setImageBitmap(AndroidFrameConverter().convert(frame))
        frameGrabber.close()
    }

    private fun showDialog() {
        val options = arrayOf<String>("Decode every frame", "Play and decode")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick a video")
            .setItems(options,
                DialogInterface.OnClickListener { dialog, which ->
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
        thread(start=true) {
            repeat(frameGrabber.lengthInVideoFrames) { i ->
                Thread.sleep(1000L)
                val frame = frameGrabber.grabFrame()
                runOnUiThread { imageView.setImageBitmap(AndroidFrameConverter().convert(frame)) }
            }
        }
    }

    private fun decodeVideo(){
        imageView.visibility = View.INVISIBLE
        videoView.visibility = View.VISIBLE
        videoView.start()
        Timer().scheduleAtFixedRate(timerTask {
            if (videoView.isPlaying) {
                captureVideoFrame(videoView.currentPosition)
            }
        },100,2)


    }
    fun captureVideoFrame(currentPosition:Int){
        val mmRetriever = MediaMetadataRetriever()
        mmRetriever.setDataSource(this,uri)
        val bm = mmRetriever.getFrameAtTime((currentPosition * 1000).toLong())
        Log.d("DBR","bm width: "+bm?.width)
    }
}