package com.glasses.app

import android.app.Application
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.oudmon.ble.base.bluetooth.BleAction
import com.oudmon.ble.base.bluetooth.BleBaseControl
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.glasses.app.data.remote.sdk.BluetoothReceiver
import com.glasses.app.data.remote.sdk.GlassesBluetoothCallback
import com.glasses.app.util.CrashLogHelper
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Application类
 * 参考官方demo的SDK初始化流程
 */
class GlassesApplication : Application() {

    companion object {
        private const val TAG = "GlassesApplication"
        lateinit var CONTEXT: Application
            private set
        private var isSDKInitialized = false
    }

    override fun onCreate() {
        super.onCreate()
        CONTEXT = this

        // 注册全局崩溃日志处理器，将崩溃信息写入文件
        setupCrashHandler()
    }

    /**
     * 注册全局崩溃日志处理器
     * 崩溃日志写入应用外部存储目录下的 crash_log.txt
     * 可通过手机文件管理器访问：Android/data/com.glasses.app/files/crash_log.txt
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "=== APP CRASHED ===", throwable)
                Log.e(TAG, "Thread: ${thread.name}")
                Log.e(TAG, "Crash log path: ${CrashLogHelper.getCrashLogPath(this)}")
                
                // 使用统一的崩溃日志工具
                CrashLogHelper.writeCrashLog(this, "APP_CRASH", "Thread: ${thread.name}", throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash log", e)
            }
            // 调用系统默认处理（显示崩溃对话框）
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        Log.d(TAG, "Crash handler registered. Log path: ${CrashLogHelper.getCrashLogPath(this)}")
    }

    /**
     * 延迟初始化SDK
     * 在首次需要时调用，避免应用启动时阻塞
     */
    fun initializeSDK() {
        if (isSDKInitialized) return

        try {
            // 步骤1：初始化SDK核心组件
            initSDKCore()

            // 步骤2：注册系统蓝牙状态监听（独立try-catch，失败不影响后续）
            try {
                registerBluetoothReceiver()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register bluetooth receiver", e)
            }

            // 步骤3：注册SDK连接回调
            try {
                registerSDKCallback()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register SDK callback", e)
            }

            // 步骤4：设置BLE控制上下文
            try {
                BleBaseControl.getInstance(CONTEXT).setmContext(this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set BLE context", e)
            }

            isSDKInitialized = true
            Log.d(TAG, "SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SDK", e)
        }
    }

    /**
     * 初始化SDK核心组件
     */
    private fun initSDKCore() {
        LargeDataHandler.getInstance()
        BleOperateManager.getInstance(this)
        BleOperateManager.getInstance().setApplication(this)
        BleOperateManager.getInstance().init()
    }

    /**
     * 注册系统蓝牙状态广播接收器
     */
    private fun registerBluetoothReceiver() {
        val deviceFilter: IntentFilter = BleAction.getDeviceIntentFilter()
        val deviceReceiver = BluetoothReceiver()
        // registerReceiver(receiver, filter, flags) 三参数版本是 API 33+ 才有
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(deviceReceiver, deviceFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(deviceReceiver, deviceFilter)
        }
    }

    /**
     * 注册SDK连接状态回调（通过LocalBroadcastManager）
     */
    private fun registerSDKCallback() {
        val intentFilter = BleAction.getIntentFilter()
        val glassesCallback = GlassesBluetoothCallback()
        LocalBroadcastManager.getInstance(CONTEXT)
            .registerReceiver(glassesCallback, intentFilter)
    }
}
