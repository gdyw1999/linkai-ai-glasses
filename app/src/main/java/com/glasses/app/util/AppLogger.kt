package com.glasses.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用日志工具类
 * 实现日志记录(DEBUG、INFO、WARN、ERROR)
 * 实现日志文件写入，10MB自动清理
 * 需求: 8.1, 8.2, 8.7
 */
object AppLogger {
    private const val TAG = "AppLogger"
    private const val LOG_FILE = "app_log.txt"
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024L // 10MB

    private var context: Context? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLog("D", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog("I", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        writeLog("W", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeLog("E", tag, message, throwable)
    }

    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val ctx = context ?: return
        try {
            val logFile = getLogFile(ctx)

            // 超过 10MB 自动清理
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                logFile.delete()
            }

            val timestamp = dateFormat.format(Date())
            FileWriter(logFile, true).use { writer ->
                writer.append("$timestamp [$level] $tag: $message\n")
                throwable?.let {
                    writer.append("  Exception: ${it.javaClass.name}: ${it.message}\n")
                    it.stackTrace.take(5).forEach { element ->
                        writer.append("    at $element\n")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    fun getLogFile(ctx: Context): File {
        val dir = File(ctx.getExternalFilesDir(null), ".")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, LOG_FILE)
    }

    fun readLog(ctx: Context): String {
        return try {
            val file = getLogFile(ctx)
            if (file.exists()) file.readText() else "No log found"
        } catch (e: Exception) {
            "Failed to read log: ${e.message}"
        }
    }

    fun clearLog(ctx: Context) {
        try {
            getLogFile(ctx).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log", e)
        }
    }
}
