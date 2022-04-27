package com.dynamsoft.cameraxvideo

import android.os.Bundle
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileReader


class ResultActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        webView = findViewById(R.id.webView)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webView.isHorizontalScrollBarEnabled = false
        webView.loadUrl("file:android_asset/index.html")
        webView.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                loadData()
            }
        })

    }

    private fun loadData(){
        if (intent.hasExtra("filename")) {
            val fileName = intent.getStringExtra("filename")
            val externalFilesPath = getExternalFilesDir("")?.absolutePath
            var path = externalFilesPath + "/" + fileName
            val f = FileReader(File(path))
            val jsonString = f.readText()
            f.close()
            val js = "javascript:displayJSONData('" + jsonString + "')"
            webView.evaluateJavascript(js,
                ValueCallback<String?> { Log.d("DBR", "received") })

        }
    }
}