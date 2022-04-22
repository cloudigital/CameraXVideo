package com.dynamsoft.cameraxvideo

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.io.InputStream
import java.lang.Exception
import java.util.*
import kotlin.concurrent.thread


class VideoActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private var currentIndex = 0
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        imageView = findViewById(R.id.imageView)
        val uri = Uri.parse(intent.getStringExtra("uri"))
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val frameGrabber = FFmpegFrameGrabber(inputStream)
        frameGrabber.start()
        Log.d("DBR","length: "+frameGrabber.lengthInVideoFrames)
        thread(start=true) {
            repeat(frameGrabber.lengthInVideoFrames) { i ->
                Thread.sleep(3000L)
                val frame = frameGrabber.grabFrame()
                runOnUiThread { imageView.setImageBitmap(AndroidFrameConverter().convert(frame)) }
            }
        }
    }
}