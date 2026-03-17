package com.glasses.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.glasses.app.R

/**
 * 前台服务 - 保持蓝牙连接和语音唤醒监听
 * 用于后台保活
 */
class GlassesConnectionService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "glasses_connection"
    }
    
    override fun onCreate() {
        super.onCreate()
        // 创建通知渠道
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 创建前台通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("青橙眼镜")
            .setContentText("正在保持连接...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        // 通知渠道创建逻辑
        // 在Android 8.0+上需要创建通知渠道
    }
}
