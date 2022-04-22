package com.dynamsoft.cameraxvideo

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class VideoActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private var currentIndex = 0
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        imageView = findViewById(R.id.imageView)
        val uri = Uri.parse(intent.getStringExtra("uri"))

        Log.d("DBR",uri.toString())
        grabFrames(uri)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun grabFrames(uri: Uri){
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this,uri)
        Log.d("DBR","count: test")
        val framesCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)

        Log.d("DBR","count: "+framesCount.toString())
        val timer = Timer()
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                val bitmap = retriever.getFrameAtIndex(currentIndex)
                imageView.setImageBitmap(bitmap)
                currentIndex = currentIndex + 1
                if (currentIndex >= 10) {
                    timer.cancel()
                }
            }
        }
        timer.scheduleAtFixedRate(task,0,200)

    }



}