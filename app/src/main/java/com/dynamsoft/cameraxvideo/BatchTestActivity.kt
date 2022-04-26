package com.dynamsoft.cameraxvideo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

class BatchTestActivity : AppCompatActivity() {
    private lateinit var progressBar:ProgressBar
    private lateinit var progressTextView:TextView
    private lateinit var fileUris:ArrayList<String>
    private val resultPathMap:HashMap<Int,String> = HashMap<Int,String>()
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
        if (intent.hasExtra("files")) {
            fileUris = intent.getStringArrayListExtra("files") as ArrayList<String>
            Log.d("DBR","files: "+fileUris.size)
            initRecycleView(fileUris)
            progressBar.max = fileUris.size
        }

    }
    private val done = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val outputPath = it.data!!.getStringExtra("outputPath")
            if (outputPath != null) {
                resultPathMap.put(currentIndex,outputPath)
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

        val adapter = FilesAdapter(this, uris)
        adapter.onItemClick = { position ->
            if (resultPathMap.containsKey(position)) {
                Toast.makeText(this,resultPathMap.get(position),Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"This one has not been tested.",Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
    }
}