package com.videooverlay.library

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.LogMessage
import com.arthenica.mobileffmpeg.Statistics
import com.videooverlay.library.custom.CallBackOfQuery
import com.videooverlay.library.custom.ExecutionLogs
import com.videooverlay.library.custom.OverlayPosition
import com.videooverlay.library.custom.ProgressStatistics
import com.videooverlay.library.interfaces.FFmpegCallBack
import com.videooverlay.library.interfaces.VideoOverlayCallBack
import com.videooverlay.library.utils.VideoOverlayUtils
import java.io.File
import java.util.*

class VideoOverlay private constructor(builder: Builder) {

    private var dataImageUri: Uri? = null

    private var context: AppCompatActivity = builder.activity
    private var mainVideoPath: String = ""
    private var overlayPosition: OverlayPosition = OverlayPosition.TOP_RIGHT
    private var imageInputFilePath: String = ""
    private var overlayView: View? = null // view overlay
    private var listener: VideoOverlayCallBack? = null

    init {
        this.mainVideoPath = builder.mainVideoPath
        this.overlayPosition = builder.overlayPosition
        this.imageInputFilePath = builder.imageInputFilePath
        this.overlayView = builder.overlayView
        this.listener = builder.listener
    }

    fun start() {
        //If user has passed view as an overlay then convert it to an image and send image path for overlay
        overlayView?.let {
            convertViewToImage(it)
        }
        startVideoRendering()
    }

    private fun startVideoRendering() {

        //We need to create a temporary file in the cache, as we need to pass this path to FFmpeg
        val tempFile = File(context.cacheDir, "${getUniqueFileName()}.mp4")
        val outputFilePath = tempFile.absolutePath

        if (TextUtils.isEmpty(mainVideoPath)) {
            throw RuntimeException("Source video path is mandatory.")
        }

        if (TextUtils.isEmpty(imageInputFilePath)) {
            throw RuntimeException("Overlay image path is mandatory.")
        }

        if (TextUtils.isEmpty(outputFilePath)) {
            throw RuntimeException("Output video path is mandatory.")
        }

        val query = addVideoOverlayAtBottom(
            mainVideoPath,
            imageInputFilePath,
            outputFilePath, overlayPosition.toString()
        )

        listener?.showLoader()

        CallBackOfQuery().callQuery(
            context,
            query,
            object : FFmpegCallBack {
                override fun statisticsProcess(statistics: Statistics) {
                    statistics.apply {
                        listener?.progressStatistics(
                            ProgressStatistics(
                                executionId,
                                videoFrameNumber,
                                videoFps,
                                videoQuality,
                                size,
                                time,
                                bitrate,
                                speed
                            )
                        )
                    }
                }

                override fun process(logMessage: LogMessage) {
                    logMessage.apply {
                        listener?.progressLogs(ExecutionLogs(executionId, text))
                    }
                }

                override fun success() {
                    listener?.hideLoader()
                    storeVideoRenderedFile(context, tempFile)?.let { uri ->
                        if (tempFile.exists()) {
                            // once we write this file to our external storage with image overlay,
                            // we should delete temp file and image overlay file
                            tempFile.delete()
                        }
                        dataImageUri?.let { deleteFileFromStorage(it) }

                        listener?.success(uri)
                    }
                }

                override fun cancel() {
                    listener?.hideLoader()
                    listener?.failed()
                }

                override fun failed() {
                    listener?.hideLoader()
                    listener?.failed()
                }
            })

    }

    private fun convertViewToImage(view: View) {
        dataImageUri = getImageOfView(view)
        dataImageUri?.let {
            VideoOverlayUtils.getPath(it, context)?.let { imagePath ->
                this.imageInputFilePath = imagePath
            }
        }
    }

    private fun getImageOfView(view: View): Uri? {
        val bitmap: Bitmap = getBitmapFromView(view)
        val videoName = "${System.currentTimeMillis()}"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, videoName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageUri = context.contentResolver.insert(collection, values)

        imageUri?.let {
            context.contentResolver.openOutputStream(it).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.update(it, values, null, null)
                }
            }
        }

        return imageUri
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        bitmap.setHasAlpha(true)
        return bitmap
    }

    private fun deleteFileFromStorage(uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            VideoOverlayUtils.getPath(uri, context)?.let {
                val file = File(it)
                file.delete()
            }
        }
    }

    private fun storeVideoRenderedFile(context: AppCompatActivity, videoFile: File): Uri? {

        val videoFileName = "${System.currentTimeMillis()}"

        val values = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/VideoOverlay/")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                ContentValues().apply {
                    put(MediaStore.DownloadColumns.DISPLAY_NAME, videoFileName)
                    put(MediaStore.DownloadColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.DownloadColumns.RELATIVE_PATH, "Download/VideoOverlay/")
                    put(MediaStore.DownloadColumns.IS_PENDING, 0)
                }
            }
            else -> {
                ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                }
            }
        }

        val collection = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            else -> {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        }

        val imageUri = context.contentResolver.insert(collection, values)

        imageUri?.let {
            context.contentResolver.openOutputStream(it).use { out ->
                out?.write(videoFile.readBytes())

                values.clear()
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                        values.put(MediaStore.DownloadColumns.IS_PENDING, 0)
                    }
                    else -> {
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.update(it, values, null, null)
                }

//                Log.d(TAG.plus("----VideoPath"), imageUri.path.toString())
            }
        }

        return imageUri
    }

    private fun addVideoOverlayAtBottom(
        inputVideo: String,
        imageInput: String,
        output: String,
        overlayPosition: String
    ): Array<String> {
        val inputs: ArrayList<String> = ArrayList()
        inputs.apply {
            add("-i")
            add(inputVideo)
            add("-i")
            add(imageInput)
            add("-filter_complex")
            add(overlayPosition)
            add("-preset")
            add("ultrafast")
            add(output)
        }
        return inputs.toArray(arrayOfNulls<String>(inputs.size))
    }

    private fun getUniqueFileName(): String {
        val gen = Random()
        var n = 10000
        n = gen.nextInt(n)
        return "$n"
    }

    class Builder(val activity: AppCompatActivity) {
        var mainVideoPath: String = ""
        var overlayPosition: OverlayPosition = OverlayPosition.TOP_RIGHT
        var imageInputFilePath: String = ""
        var overlayView: View? = null
        var listener: VideoOverlayCallBack? = null

        fun setMainVideoFilePath(mainVideoPath: String) =
            apply { this.mainVideoPath = mainVideoPath }

        fun setOverlayImagePosition(overlayPosition: OverlayPosition) =
            apply { this.overlayPosition = overlayPosition }

        //overlay image path
        fun setOverlayImage(imageInputFilePath: String) =
            apply { this.imageInputFilePath = imageInputFilePath }

        //overlay as view
        fun setOverlayImage(view: View) =
            apply { this.overlayView = view }

        fun setListener(listener: VideoOverlayCallBack) = apply { this.listener = listener }

        fun build(): VideoOverlay {
            if (TextUtils.isEmpty(mainVideoPath)) {
                throw RuntimeException("Video path must not be null or empty")
            }
            if (TextUtils.isEmpty(imageInputFilePath) && overlayView == null) {
                throw RuntimeException("Image path or view must required for video overlay")
            }
            if (listener == null) {
                throw RuntimeException("Must implement the listener to get the output video uri")
            }

            return VideoOverlay(this)
        }
    }
}