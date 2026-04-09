package com.glasses.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.AlertDialog

/**
 * 华为设备保活引导工具类
 * 用于引导用户将App加入"受保护应用"列表，防止被系统杀死
 * 
 * 验证需求: 10.6
 */
object HuaweiProtectedAppsHelper {
    
    /**
     * 检测是否为华为或荣耀设备
     */
    fun isHuaweiDevice(): Boolean {
        return Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
               Build.MANUFACTURER.equals("HONOR", ignoreCase = true)
    }
    
    /**
     * 显示华为设备保活引导对话框
     * 引导用户将App加入"受保护应用"列表
     */
    fun showProtectedAppsGuide(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("后台保活设置")
            .setMessage(
                "为了保持语音唤醒功能正常工作，请将本应用加入\"受保护应用\"列表：\n\n" +
                "1. 打开\"设置\"\n" +
                "2. 进入\"应用\" > \"应用启动管理\"\n" +
                "3. 找到\"Linkai星韵AI眼镜\"\n" +
                "4. 关闭\"自动管理\"，并开启\"允许后台活动\""
            )
            .setPositiveButton("去设置") { _, _ ->
                openProtectedAppsSettings(context)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 打开华为设备的"受保护应用"设置页面
     * 如果无法打开特定页面，则打开应用详情页
     */
    private fun openProtectedAppsSettings(context: Context) {
        try {
            // 尝试打开华为的应用启动管理页面
            val intent = Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开特定页面，打开应用设置页
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // 如果还是失败，打开通用设置页
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
}
