package com.dynamsoft.cameraxvideo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder


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
            var jsonString = f.readText()
            f.close()
            Log.d("DBR","JSONString:"+jsonString)
            val js = "javascript:displayJSONData('" + jsonString + "')"
            webView.evaluateJavascript(js,
                ValueCallback<String?> { Log.d("DBR", "received") })
        }
    }
}