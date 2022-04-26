package com.dynamsoft.cameraxvideo

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class ResultActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        webView = findViewById(R.id.webView)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        loadHTMLFromAssets();
    }
    private fun loadHTMLFromAssets(){

        var param = "?time="+System.currentTimeMillis()
        if (intent.hasExtra("filename")) {
            val fileName = intent.getStringExtra("filename")
            val externalFilesPath = getExternalFilesDir("")?.absolutePath
            var outputPath = externalFilesPath + "/" + fileName
            param = param+"&path="+outputPath
            Log.d("DBR",param)
            Toast.makeText(this,param,Toast.LENGTH_LONG).show()
        }
        webView.loadUrl("file:android_asset/index.html"+param);
    }
}