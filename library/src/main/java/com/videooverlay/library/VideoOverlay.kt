package com.videooverlay.library

import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.LogMessage
import com.arthenica.mobileffmpeg.Statistics
import com.videooverlay.library.custom.CallBackOfQuery
import com.videooverlay.library.custom.ExecutionLogs
import com.videooverlay.library.custom.ProgressStatistics
import com.videooverlay.library.interfaces.FFmpegCallBack
import com.videooverlay.library.interfaces.VideoOverlayCallBack
import java.io.File
import java.util.*

class VideoOverlay(activity: AppCompatActivity) {

    private var dataImageUri: Uri?=null
    private val context: AppCompatActivity = activity

    fun startVideoRendering(
        mainVideoPath: String,
        imageInputFilePath: String, listener: VideoOverlayCallBack
    ) {

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
            outputFilePath
        )

        listener.showLoader()

        CallBackOfQuery().callQuery(
            context,
            query,
            object : FFmpegCallBack {
                override fun statisticsProcess(statistics: Statistics) {
                    statistics.apply {
                        listener.progressStatistics(
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
                        listener.progressLogs(ExecutionLogs(executionId, text))
                    }
                }

                override fun success() {
                    listener.hideLoader()
                    storeVideoRenderedFile(context, tempFile)?.let { uri ->
                        if (tempFile.exists()) {
                            // once we write this file to our external storage with image overlay,
                            // we should delete temp file and image overlay file
                            tempFile.delete()
                        }
                        dataImageUri?.let { deleteFileFromStorage(it)}

                        listener.success(uri)
                    }
                }

                override fun cancel() {
                    listener.hideLoader()
                    listener.failed()
                }

                override fun failed() {
                    listener.hideLoader()
                    listener.failed()
                }
            })

    }

    fun startVideoRendering(
        mainVideoPath: String,
        view: View, listener: VideoOverlayCallBack
    ) {
        dataImageUri = getImageOfView(view)
        dataImageUri?.let {
            getPath(it)?.let { imagePath ->
                startVideoRendering(mainVideoPath, imagePath, listener)
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
            getPath(uri)?.let {
                val file = File(it)
                file.delete()
            }
        }
    }

    private fun getPath(uri: Uri): String? {
        var uri = uri
        val needToCheckUri = Build.VERSION.SDK_INT >= 19
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        if (needToCheckUri && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                uri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                if ("image" == type) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                selection = "_id=?"
                selectionArgs = arrayOf(split[1])
            }
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )
                val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
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
        output: String
    ): Array<String> {
        val inputs: ArrayList<String> = ArrayList()
        inputs.apply {
            add("-i")
            add(inputVideo)
            add("-i")
            add(imageInput)
            add("-filter_complex")
            add("overlay=x=(W-w)/2:y=H-h-5") // center bottom
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
}