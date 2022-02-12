package com.videooverlay.library.custom

/*
*   Statistics for running executions.
* */
class ProgressStatistics(
    var executionId: Long,
    var videoFrameNumber: Int,
    var videoFps: Float,
    var videoQuality: Float,
    var size: Long,
    var time: Int,
    var bitrate: Double,
    var speed: Double
) {

    fun update(newStatistics: ProgressStatistics?) {
        if (newStatistics != null) {
            executionId = newStatistics.executionId
            if (newStatistics.videoFrameNumber > 0) {
                videoFrameNumber = newStatistics.videoFrameNumber
            }
            if (newStatistics.videoFps > 0) {
                videoFps = newStatistics.videoFps
            }
            if (newStatistics.videoQuality > 0) {
                videoQuality = newStatistics.videoQuality
            }
            if (newStatistics.size > 0) {
                size = newStatistics.size
            }
            if (newStatistics.time > 0) {
                time = newStatistics.time
            }
            if (newStatistics.bitrate > 0) {
                bitrate = newStatistics.bitrate
            }
            if (newStatistics.speed > 0) {
                speed = newStatistics.speed
            }
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Statistics{")
        stringBuilder.append("executionId=")
        stringBuilder.append(executionId)
        stringBuilder.append(", videoFrameNumber=")
        stringBuilder.append(videoFrameNumber)
        stringBuilder.append(", videoFps=")
        stringBuilder.append(videoFps)
        stringBuilder.append(", videoQuality=")
        stringBuilder.append(videoQuality)
        stringBuilder.append(", size=")
        stringBuilder.append(size)
        stringBuilder.append(", time=")
        stringBuilder.append(time)
        stringBuilder.append(", bitrate=")
        stringBuilder.append(bitrate)
        stringBuilder.append(", speed=")
        stringBuilder.append(speed)
        stringBuilder.append('}')
        return stringBuilder.toString()
    }
}