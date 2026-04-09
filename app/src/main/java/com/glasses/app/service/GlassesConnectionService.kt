package com.glasses.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.glasses.app.MainActivity
import com.glasses.app.R
import com.glasses.app.data.remote.sdk.ConnectionState
import com.glasses.app.data.remote.sdk.GlassesSDKManager
import com.glasses.app.service.wakeup.WakeupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 前台服务 - 保持蓝牙连接和语音唤醒监听
 * 用于后台保活，确保语音唤醒功能持续可用
 * 
 * 验证需求: 10.6
 */
class GlassesConnectionService : Service() {
    
    companion object {
        private const val TAG = "GlassesConnectionService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "glasses_connection"
        private const val CHANNEL_NAME = "眼镜连接服务"
        
        // Intent Actions
        const val ACTION_START_SERVICE = "com.glasses.app.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.glasses.app.action.STOP_SERVICE"
        
        /**
         * 启动前台服务
         */
        fun startService(context: Context) {
            val intent = Intent(context, GlassesConnectionService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Service start requested")
        }
        
        /**
         * 停止前台服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, GlassesConnectionService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
            Log.d(TAG, "Service stop requested")
        }
    }
    
    // SDK管理器
    private lateinit var sdkManager: GlassesSDKManager
    
    // 唤醒管理器
    private lateinit var wakeupManager: WakeupManager
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    // 连接状态监听Job
    private var connectionStateJob: Job? = null
    
    // 电量监听Job
    private var batteryLevelJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== Service onCreate START ===")

        try {
            // 步骤1：创建通知渠道
            Log.d(TAG, "Step 1: Creating notification channel...")
            createNotificationChannel()
            Log.d(TAG, "Step 1: Notification channel created ✓")

            // 步骤2：初始化SDK管理器
            Log.d(TAG, "Step 2: Initializing SDK manager...")
            sdkManager = GlassesSDKManager.getInstance(applicationContext)
            Log.d(TAG, "Step 2: SDK manager initialized ✓")

            // 步骤3：初始化唤醒管理器（可选，失败不影响）
            Log.d(TAG, "Step 3: Initializing wakeup manager...")
            try {
                wakeupManager = WakeupManager.getInstance(applicationContext)
                Log.d(TAG, "Step 3: Wakeup manager initialized ✓")
            } catch (e: Exception) {
                Log.w(TAG, "Step 3: Wakeup manager init failed (non-critical)", e)
            }

            // 步骤4：监听连接状态
            Log.d(TAG, "Step 4: Setting up connection state observer...")
            observeConnectionState()
            Log.d(TAG, "Step 4: Connection state observer set up ✓")

            // 步骤5：监听电量
            Log.d(TAG, "Step 5: Setting up battery level observer...")
            observeBatteryLevel()
            Log.d(TAG, "Step 5: Battery level observer set up ✓")
            
            Log.d(TAG, "=== Service onCreate COMPLETED ===")
        } catch (e: Exception) {
            Log.e(TAG, "=== Service onCreate FAILED ===", e)
            writeCrashLog("Service onCreate failed", e)
            throw e
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand: action=${intent?.action}, startId=$startId")
        
        try {
            when (intent?.action) {
                ACTION_START_SERVICE -> {
                    Log.d(TAG, "Starting foreground service...")
                    // 启动前台服务
                    startForegroundService()
                    Log.d(TAG, "Foreground service started successfully")
                }
                ACTION_STOP_SERVICE -> {
                    Log.d(TAG, "Stopping service...")
                    // 停止服务
                    stopSelf()
                }
                else -> {
                    Log.w(TAG, "Unknown action or null intent, starting foreground service anyway")
                    startForegroundService()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand failed", e)
            writeCrashLog("onStartCommand failed", e)
            // 即使失败也要尝试启动前台服务，避免ANR
            try {
                startForegroundService()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to start foreground service in error handler", e2)
            }
        }
        
        // START_STICKY: 服务被杀死后会自动重启
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        // 不支持绑定
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        
        // 取消所有协程
        serviceScope.cancel()
        
        // 取消监听Job
        connectionStateJob?.cancel()
        batteryLevelJob?.cancel()
    }
    
    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        try {
            Log.d(TAG, "Creating notification for foreground service...")
            val notification = createNotification(
                connectionState = sdkManager.connectionState.value,
                batteryLevel = sdkManager.batteryLevel.value,
                isCharging = sdkManager.isCharging.value
            )
            Log.d(TAG, "Notification created successfully")
            
            // 启动前台服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "Starting foreground service with CONNECTED_DEVICE type (API ${Build.VERSION.SDK_INT})")
                // Android 10+ 需要指定前台服务类型
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                Log.d(TAG, "Starting foreground service (API ${Build.VERSION.SDK_INT})")
                startForeground(NOTIFICATION_ID, notification)
            }
            
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            writeCrashLog("startForegroundService failed", e)
            throw e
        }
    }
    
    /**
     * 创建通知渠道 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持与眼镜的蓝牙连接和语音唤醒监听"
                // 不显示角标
                setShowBadge(false)
                // 不震动
                enableVibration(false)
                // 不发出声音
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created")
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(
        connectionState: ConnectionState,
        batteryLevel: Int,
        isCharging: Boolean
    ): Notification {
        try {
            Log.d(TAG, "Creating notification: state=$connectionState, battery=$batteryLevel%, charging=$isCharging")
            
            // 点击通知跳转到MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // 根据连接状态生成通知内容
            val (title, content) = when (connectionState) {
                ConnectionState.CONNECTED -> {
                    val batteryText = if (isCharging) {
                        "电量 $batteryLevel% (充电中)"
                    } else {
                        "电量 $batteryLevel%"
                    }
                    "Linkai星韵AI眼镜" to "已连接，语音唤醒已启用 · $batteryText"
                }
                ConnectionState.CONNECTING -> {
                    "Linkai星韵AI眼镜" to "正在连接..."
                }
                ConnectionState.DISCONNECTED -> {
                    "Linkai星韵AI眼镜" to "未连接"
                }
            }
            
            Log.d(TAG, "Notification content: title='$title', content='$content'")
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // 持久通知，不可滑动删除
                .setAutoCancel(false)
                .build()
            
            Log.d(TAG, "Notification created successfully")
            return notification
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification", e)
            writeCrashLog("createNotification failed", e)
            throw e
        }
    }
    
    /**
     * 监听连接状态变化
     */
    private fun observeConnectionState() {
        connectionStateJob = sdkManager.connectionState
            .onEach { state ->
                Log.d(TAG, "Connection state changed: $state")
                // 更新通知
                updateNotification()
            }
            .launchIn(serviceScope)
    }
    
    /**
     * 监听电量变化
     */
    private fun observeBatteryLevel() {
        batteryLevelJob = sdkManager.batteryLevel
            .onEach { level ->
                Log.d(TAG, "Battery level changed: $level%")
                // 更新通知
                updateNotification()
            }
            .launchIn(serviceScope)
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        val notification = createNotification(
            connectionState = sdkManager.connectionState.value,
            batteryLevel = sdkManager.batteryLevel.value,
            isCharging = sdkManager.isCharging.value
        )
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 写入崩溃日志到文件
     */
    private fun writeCrashLog(message: String, throwable: Throwable) {
        try {
            val crashDir = java.io.File(getExternalFilesDir(null), ".")
            if (!crashDir.exists()) crashDir.mkdirs()
            val crashFile = java.io.File(crashDir, "crash_log.txt")
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            java.io.FileWriter(crashFile, true).use { writer ->
                writer.append("=== SERVICE CRASH $timestamp ===\n")
                writer.append("Message: $message\n")
                writer.append("Exception: ${throwable.javaClass.name}\n")
                writer.append("Error: ${throwable.message}\n")
                writer.append("Stack trace:\n")
                throwable.stackTrace.forEach { element ->
                    writer.append("    at $element\n")
                }
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }
    }
}
