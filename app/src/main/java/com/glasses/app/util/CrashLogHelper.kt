package com.glasses.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃日志辅助类
 * 提供统一的崩溃日志写入和读取功能
 */
object CrashLogHelper {
    private const val TAG = "CrashLogHelper"
    private const val CRASH_LOG_FILE = "crash_log.txt"
    
    /**
     * 写入崩溃日志
     */
    fun writeCrashLog(context: Context, tag: String, message: String, throwable: Throwable) {
        try {
            val crashFile = getCrashLogFile(context)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
            
            java.io.FileWriter(crashFile, true).use { writer ->
                writer.append("=== CRASH [$tag] $timestamp ===\n")
                writer.append("Message: $message\n")
                writer.append("Exception: ${throwable.javaClass.name}\n")
                writer.append("Error: ${throwable.message}\n")
                writer.append("Stack trace:\n")
                throwable.stackTrace.forEach { element ->
                    writer.append("    at $element\n")
                }
                
                // 打印cause chain
                var cause = throwable.cause
                var depth = 1
                while (cause != null && depth <= 5) {
                    writer.append("Caused by ($depth): ${cause.javaClass.name}: ${cause.message}\n")
                    cause.stackTrace.take(5).forEach { element ->
                        writer.append("    at $element\n")
                    }
                    cause = cause.cause
                    depth++
                }
                writer.append("\n")
            }
            
            Log.d(TAG, "Crash log written to ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }
    }
    
    /**
     * 读取崩溃日志
     */
    fun readCrashLog(context: Context): String {
        return try {
            val crashFile = getCrashLogFile(context)
            if (crashFile.exists()) {
                crashFile.readText()
            } else {
                "No crash log found"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read crash log", e)
            "Failed to read crash log: ${e.message}"
        }
    }
    
    /**
     * 清空崩溃日志
     */
    fun clearCrashLog(context: Context) {
        try {
            val crashFile = getCrashLogFile(context)
            if (crashFile.exists()) {
                crashFile.delete()
                Log.d(TAG, "Crash log cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash log", e)
        }
    }
    
    /**
     * 获取崩溃日志文件
     */
    private fun getCrashLogFile(context: Context): File {
        val crashDir = File(context.getExternalFilesDir(null), ".")
        if (!crashDir.exists()) {
            crashDir.mkdirs()
        }
        return File(crashDir, CRASH_LOG_FILE)
    }
    
    /**
     * 获取崩溃日志文件路径
     */
    fun getCrashLogPath(context: Context): String {
        return getCrashLogFile(context).absolutePath
    }
}
