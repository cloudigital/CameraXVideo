package com.dynamsoft.cameraxvideo

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame


class VideoActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        imageView = findViewById(R.id.imageView)
        val uri = Uri.parse(intent.getStringExtra("uri"))

        Log.d("DBR",uri.toString())
        var inputStream = this.getContentResolver().openInputStream(uri);
        var frameGrabber = FFmpegFrameGrabber(inputStream)
        frameGrabber.start()
        var flag = 0
        val ftp = frameGrabber.lengthInFrames
        while (flag <= ftp) {
            val frame = frameGrabber.grabFrame();
            if (frame != null) {
                    Log.d("DBR","update")
                imageView.setImageBitmap(FrameToBitmap(frame))
            }
            flag++
        }
        //frameGrabber.stop()
        //frameGrabber.close()
    }

    private fun FrameToBitmap(frame: Frame):Bitmap {
        val converter = AndroidFrameConverter()
        return converter.convert(frame)
    }

}