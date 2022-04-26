package com.dynamsoft.cameraxvideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView

class ResultActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        webView = findViewById(R.id.webView)
        loadHTMLFromAssets();
    }
    private fun loadHTMLFromAssets(){

        var param = ""
        if (intent.hasExtra("filename")) {
            val fileName = intent.getStringExtra("filename")
            val externalFilesPath = getExternalFilesDir("")?.absolutePath
            var outputPath = externalFilesPath + "/" + fileName
            param = "?path="+outputPath
        }
        webView.loadUrl("file:android_asset/index.html"+param);
    }
}