package com.videooverlay.library.interfaces

import android.net.Uri
import com.arthenica.mobileffmpeg.LogMessage
import com.arthenica.mobileffmpeg.Statistics
import com.videooverlay.library.custom.ExecutionLogs
import com.videooverlay.library.custom.ProgressStatistics

interface VideoOverlayCallBack {
    fun showLoader()
    fun hideLoader()
    fun progressStatistics(statistics: ProgressStatistics)
    fun progressLogs(executionLogs: ExecutionLogs)
    fun success(outputFileUri:Uri)
    fun failed()
}