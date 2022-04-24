package com.dynamsoft.cameraxvideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.lang.StringBuilder

class BatchTestActivity : AppCompatActivity() {
    private lateinit var progressBar:ProgressBar
    private lateinit var filesTextView:TextView
    private lateinit var progressTextView:TextView
    private lateinit var fileUris:ArrayList<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_test)
        progressBar = findViewById(R.id.progressBar)
        filesTextView = findViewById(R.id.filesTextView)
        progressTextView = findViewById(R.id.progressTextView)

        val startTestingButton = findViewById<Button>(R.id.startTestingButton)
        startTestingButton.setOnClickListener {

        }
        if (intent.hasExtra("files")) {
            fileUris = intent.getStringArrayListExtra("files") as ArrayList<String>
            Log.d("DBR","files: "+fileUris.size)
            progressBar.max = fileUris.size
            updateFilesInfo()
        }
    }

    private fun updateFilesInfo(){
        val sb:StringBuilder = StringBuilder()
        sb.append("Files:\n")
        for (uri in fileUris) {
            sb.append(uri)
            sb.append("\n")
        }
        filesTextView.text = sb.toString()
    }

    private fun updateProgressInfo(progress:Int){
        progressBar.progress = progress
        progressTextView.text = "Progress: "+progress+"/"+progressBar.max
    }
}