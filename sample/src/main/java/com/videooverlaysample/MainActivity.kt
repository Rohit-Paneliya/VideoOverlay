package com.videooverlaysample

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import com.videooverlay.library.VideoOverlay
import com.videooverlay.library.custom.ExecutionLogs
import com.videooverlay.library.custom.OverlayPosition
import com.videooverlay.library.custom.ProgressStatistics
import com.videooverlay.library.interfaces.VideoOverlayCallBack

class MainActivity : AppCompatActivity(), VideoOverlayCallBack {

    private var videoView: VideoView? = null
    private lateinit var progressDialog: ProgressDialog
    private val sampleVideoPath =
        "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/VID-20220120-WA0000.mp4"

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressDialog = ProgressDialog(this)

        videoView = findViewById(R.id.videoView)
        val imageView = findViewById<AppCompatImageView>(R.id.imageView)
        val buttonStart = findViewById<AppCompatButton>(R.id.buttonStart)

        val videoPath = "android.resource://$packageName/raw/test"
        videoView?.setVideoURI(Uri.parse(videoPath))
        videoView?.start()

        buttonStart.setOnClickListener {
            VideoOverlay.Builder(this)
                .setMainVideoFilePath(sampleVideoPath)
                .setOverlayImagePosition(OverlayPosition.BOTTOM_CENTER)
                .setOverlayImage(imageView)
//                .setOutputFolderName("Output")
                .setListener(this)
                .build()
                .start()
        }
    }

    fun showMessage(keyName: String, string: String) {
        Log.d("---------------$keyName", string)
    }

    override fun showLoader() {
        progressDialog.show()
        showMessage("loader", "showloader")
    }

    override fun hideLoader() {
        progressDialog.dismiss()
        showMessage("loader", "hideloader")
    }

    override fun progressStatistics(statistics: ProgressStatistics) {
        showMessage("progressStatistics", statistics.toString())
    }

    override fun progressLogs(executionLogs: ExecutionLogs) {
        showMessage("progressLogs", executionLogs.toString())
    }

    override fun success(outputFileUri: Uri) {
        progressDialog.dismiss()
        showMessage("success", "failed")
        videoView?.setVideoURI(outputFileUri)
        videoView?.start()
    }

    override fun failed() {
        progressDialog.dismiss()
        showMessage("failed", "failed")
    }
}