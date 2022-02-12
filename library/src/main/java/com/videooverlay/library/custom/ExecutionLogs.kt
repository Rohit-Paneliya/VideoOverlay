package com.videooverlay.library.custom

/*
*   Logs for running executions.
* */
class ExecutionLogs(val executionId: Long, val text: String) {
    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("LogMessage{")
        stringBuilder.append("executionId=")
        stringBuilder.append(executionId)
        stringBuilder.append(", text=")
        stringBuilder.append("\'")
        stringBuilder.append(text)
        stringBuilder.append('\'')
        stringBuilder.append('}')
        return stringBuilder.toString()
    }
}