package com.dynamsoft.cameraxvideo

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BatchTestActivity : AppCompatActivity() {
    private lateinit var progressBar:ProgressBar
    private lateinit var progressTextView:TextView
    private lateinit var fileUris:ArrayList<String>
    private var outputFilename:String = ""
    private val filenames:ArrayList<String> = ArrayList<String>()
    private val resultPathMap:HashMap<String,String> = HashMap<String,String>()
    private var currentIndex:Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_test)
        progressBar = findViewById(R.id.progressBar)
        progressTextView = findViewById(R.id.progressTextView)

        val startTestingButton = findViewById<Button>(R.id.startTestingButton)
        startTestingButton.setOnClickListener {
            batchTest()
        }
        val saveResultsButton = findViewById<Button>(R.id.saveResultsButton)
        saveResultsButton.setOnClickListener {
            saveResults()
        }
        val checkResultsButton = findViewById<Button>(R.id.checkResultsButton)
        checkResultsButton.setOnClickListener {
            checkResults()
        }
        if (intent.hasExtra("files")) {
            fileUris = intent.getStringArrayListExtra("files") as ArrayList<String>
            getFilenamesFromUris()
            Log.d("DBR","files: "+fileUris.size)
            initRecycleView(fileUris)
            progressBar.max = fileUris.size
        }
    }

    private fun getFilenamesFromUris() {
        for (uriString in fileUris) {
            val uri = Uri.parse(uriString)
            val filename = uri.contentSchemeName()
            if (filename!=null) {
                filenames.add(filename)
            }
        }
    }

    private val done = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val outputPath = it.data!!.getStringExtra("outputPath")
            if (outputPath != null) {
                val filename = filenames.get(currentIndex)
                resultPathMap[filename] = outputPath
            }
            currentIndex++
            progressBar.progress = currentIndex
            if (currentIndex <= fileUris.size - 1) {
                batchTest()
            }
        }
    }

    private fun batchTest() {
        var intent = Intent(this,VideoActivity::class.java)
        intent.putExtra("uri",fileUris.get(currentIndex))
        intent.putExtra("automation",true)
        done.launch(intent)
    }

    private fun initRecycleView(uris: ArrayList<String>){
        var recyclerView = findViewById<RecyclerView>(R.id.filesRecyclerView)
        val layoutManager = LinearLayoutManager(this);
        recyclerView.layoutManager = layoutManager;
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                layoutManager.orientation
            )
        )

        val adapter = FilesAdapter(this, filenames)
        adapter.onItemClick = { position ->
            val key = filenames.get(position)
            if (resultPathMap.containsKey(key)) {
                Toast.makeText(this,resultPathMap[key],Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(this,"This one has not been tested.",Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun saveResults(){
        var resultMap = HashMap<String,HashMap<String, SDKResult>>()
        for (key in resultPathMap.keys){
            val path = resultPathMap[key]
            val f = FileReader(File(path))
            val jsonString = f.readText()
            f.close()
            var benchmarkResult = Json.decodeFromString<HashMap<String, SDKResult>>(jsonString)
            resultMap.put(key,benchmarkResult)
        }
        val string = Json.encodeToString(resultMap)
        val pattern = "yyyy-MM-dd-HH-mm-ss-SSS"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val date: String = simpleDateFormat.format(Date())
        val path = writeStringAsFile(string, "result-$date.json")
        outputFilename = "result-$date.json"
        Toast.makeText(this, "File written to $path",Toast.LENGTH_SHORT).show()
    }

    private fun checkResults() {
        if (outputFilename != "") {
            showResultActivity(outputFilename)
        }else{
            val externalFilesPath = getExternalFilesDir("")?.absolutePath
            var folder = File(externalFilesPath)
            var resultFilenamesList = ArrayList<String>()
            for (file in folder.listFiles()){
                if (file.name.startsWith("result-")) {
                    resultFilenamesList.add(file.name)
                }
            }
            showDialog(resultFilenamesList.toTypedArray())
        }
    }

    private fun showDialog(options:Array<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick a result file")
            .setItems(options,
                DialogInterface.OnClickListener { dialog, which ->
                    showResultActivity(options[which])
                })
        builder.create().show()
    }

    private fun showResultActivity(filename:String){
        var intent = Intent(this,ResultActivity::class.java)
        intent.putExtra("filename",filename)
        startActivity(intent)
    }

    fun Uri.contentSchemeName(): String? {
        return contentResolver.query(this, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.getString(name)
        }
    }

    private fun writeStringAsFile(fileContents: String?, fileName: String):String {
        try {
            val externalFilesPath = getExternalFilesDir("")?.absolutePath
            var outputPath = externalFilesPath + "/" + fileName
            val out = FileWriter(File(outputPath))
            out.write(fileContents)
            out.close()
            return outputPath
        } catch (e: IOException) {
        }
        return ""
    }
}