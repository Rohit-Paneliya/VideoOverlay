package com.videooverlay.library.utils

import android.app.Activity
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

object VideoOverlayUtils {

    fun getPath(uri: Uri, context: Activity): String? {
        var uri = uri
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        if (DocumentsContract.isDocumentUri(context, uri)) {
            when {
                isExternalStorageDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split =
                        docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
                isDownloadsDocument(uri) -> {
                    val id = DocumentsContract.getDocumentId(uri)
                    uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                }
                isMediaDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split =
                        docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]
                    when (type) {
                        "image" -> {
                            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                        "video" -> {
                            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        }
                        "audio" -> {
                            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                    }
                    selection = "_id=?"
                    selectionArgs = arrayOf(split[1])
                }
            }
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor?
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
}