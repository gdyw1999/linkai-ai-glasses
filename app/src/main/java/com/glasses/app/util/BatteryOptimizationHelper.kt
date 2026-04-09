package com.glasses.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.app.AlertDialog

/**
 * 电池优化请求工具类
 * 用于请求忽略电池优化，并提供厂商特定的保活引导
 * 
 * 验证需求: 10.6
 */
object BatteryOptimizationHelper {
    
    /**
     * 检查是否已忽略电池优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true // Android 6.0以下没有电池优化
    }
    
    /**
     * 请求忽略电池优化
     * 显示对话框引导用户设置
     */
    fun requestIgnoreBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = activity.packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(activity)
                    .setTitle("电池优化设置")
                    .setMessage("为了保持语音唤醒功能，建议关闭电池优化")
                    .setPositiveButton("去设置") { _, _ ->
                        openBatteryOptimizationSettings(activity)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }
    
    /**
     * 打开电池优化设置页面
     */
    private fun openBatteryOptimizationSettings(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            }
        } catch (e: Exception) {
            // 如果无法打开特定页面，打开应用设置页
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                // 如果还是失败，打开通用设置页
                val intent = Intent(Settings.ACTION_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }
    
    /**
     * 显示厂商特定的保活引导
     * 根据不同厂商提供不同的设置指引
     */
    fun showManufacturerSpecificGuide(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        val message = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                "小米/Redmi设备：\n\n" +
                "1. 设置 > 应用设置 > 应用管理\n" +
                "2. 找到本应用 > 省电策略\n" +
                "3. 选择\"无限制\"\n\n" +
                "4. 设置 > 应用设置 > 应用管理\n" +
                "5. 找到本应用 > 自启动\n" +
                "6. 开启\"允许自启动\""
            }
            manufacturer.contains("oppo") -> {
                "OPPO设备：\n\n" +
                "1. 设置 > 电池 > 应用耗电管理\n" +
                "2. 找到本应用\n" +
                "3. 允许后台运行\n\n" +
                "4. 设置 > 应用管理 > 应用列表\n" +
                "5. 找到本应用 > 应用自启动\n" +
                "6. 开启自启动"
            }
            manufacturer.contains("vivo") -> {
                "vivo设备：\n\n" +
                "1. 设置 > 电池 > 后台高耗电\n" +
                "2. 找到本应用\n" +
                "3. 允许后台高耗电\n\n" +
                "4. 设置 > 更多设置 > 权限管理\n" +
                "5. 找到本应用 > 自启动\n" +
                "6. 开启自启动"
            }
            manufacturer.contains("samsung") -> {
                "三星设备：\n\n" +
                "1. 设置 > 应用程序 > 本应用\n" +
                "2. 电池 > 优化电池用量\n" +
                "3. 选择\"不优化\"\n\n" +
                "4. 设置 > 设备维护 > 电池\n" +
                "5. 应用程序电源管理\n" +
                "6. 将本应用添加到\"不受监控的应用\""
            }
            manufacturer.contains("oneplus") -> {
                "OnePlus设备：\n\n" +
                "1. 设置 > 电池 > 电池优化\n" +
                "2. 找到本应用\n" +
                "3. 选择\"不优化\"\n\n" +
                "4. 设置 > 应用 > 应用管理\n" +
                "5. 找到本应用 > 电池\n" +
                "6. 选择\"不限制\""
            }
            manufacturer.contains("realme") -> {
                "Realme设备：\n\n" +
                "1. 设置 > 电池 > 省电模式\n" +
                "2. 应用快速冻结\n" +
                "3. 将本应用从列表中移除\n\n" +
                "4. 设置 > 应用管理\n" +
                "5. 找到本应用 > 应用自启动\n" +
                "6. 开启自启动"
            }
            else -> {
                "为了保持语音唤醒功能正常工作，请在系统设置中：\n\n" +
                "1. 关闭本应用的电池优化\n" +
                "2. 允许本应用后台运行\n" +
                "3. 允许本应用自启动（如有此选项）\n\n" +
                "具体设置路径请参考您的设备说明书"
            }
        }
        
        AlertDialog.Builder(context)
            .setTitle("后台保活设置")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show()
    }
}
