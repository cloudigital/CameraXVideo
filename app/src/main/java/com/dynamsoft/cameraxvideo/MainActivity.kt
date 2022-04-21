package com.dynamsoft.cameraxvideo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {
    private var PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var btn = findViewById<Button>(R.id.startRecordingButton)
        btn.setOnClickListener {
            startRecording()
        }

        // add the storage access permission request for Android 9 and below.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionList = PERMISSIONS_REQUIRED.toMutableList()
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            PERMISSIONS_REQUIRED = permissionList.toTypedArray()
        }

        if (!hasPermissions(this)) {
            // Request camera-related permissions
            activityResultLauncher.launch(PERMISSIONS_REQUIRED)
        }
    }

    private fun startRecording() {
        val radioButton720P = findViewById<RadioButton>(R.id.radioButton720P)
        val radioButton1080P = findViewById<RadioButton>(R.id.radioButton1080P)
        val radioButton4K = findViewById<RadioButton>(R.id.radioButton4K)
        val durationEditText = findViewById<EditText>(R.id.durationEditText)

        var intent = Intent(this,CameraActivity::class.java)
        intent.putExtra("duration",Integer.parseInt(durationEditText.text.toString()))

        var resolution = "720P"
        if (radioButton720P.isChecked) {
            resolution = "720P"
        }else if (radioButton1080P.isChecked) {
            resolution = "1080P"
        }else {
            resolution = "4K"
        }
        intent.putExtra("resolution",resolution)

        startActivity(intent)
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in PERMISSIONS_REQUIRED && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
}