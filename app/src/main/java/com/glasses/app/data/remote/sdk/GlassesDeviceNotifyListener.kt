package com.glasses.app.data.remote.sdk

import android.util.Log
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyListener
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp

/**
 * 眼镜设备通知监听器
 * 接收眼镜端推送的电量、按键、语音唤醒、OTA等通知
 * 参考官方demo的MyDeviceNotifyListener实现
 */
class GlassesDeviceNotifyListener(
    private val sdkManager: GlassesSDKManager
) : GlassesDeviceNotifyListener() {

    companion object {
        private const val TAG = "DeviceNotifyListener"

        // 通知类型常量（loadData[6]的值）
        private const val CMD_BATTERY = 0x05       // 电量上报
        private const val CMD_QUICK_RECOGNITION = 0x02  // 快速识别/拍照
        private const val CMD_MICROPHONE = 0x03     // 麦克风激活
        private const val CMD_OTA = 0x04            // OTA升级
        private const val CMD_PAUSE = 0x0c          // 暂停/语音广播
        private const val CMD_UNBIND = 0x0d         // 解绑APP
    }

    override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
        try {
            val data = response.loadData
            if (data.size < 7) {
                Log.w(TAG, "Data too short: ${data.size} bytes")
                return
            }

            when (data[6].toInt()) {
                CMD_BATTERY -> {
                    // 电量上报：loadData[7]=电量值，loadData[8]=是否充电(1=充电中)
                    if (data.size >= 9) {
                        val battery = data[7].toInt() and 0xFF
                        val charging = (data[8].toInt() and 0xFF) == 1
                        Log.d(TAG, "Battery: $battery%, charging=$charging")
                        sdkManager.updateBatteryInfo(battery, charging)
                    }
                }

                CMD_QUICK_RECOGNITION -> {
                    Log.d(TAG, "Quick recognition event")
                }

                CMD_MICROPHONE -> {
                    if (data.size >= 8 && data[7].toInt() == 1) {
                        Log.d(TAG, "Microphone activated - user started speaking")
                    }
                }

                CMD_OTA -> {
                    Log.d(TAG, "OTA event")
                }

                CMD_PAUSE -> {
                    Log.d(TAG, "Pause event")
                }

                CMD_UNBIND -> {
                    Log.d(TAG, "Unbind event")
                }

                else -> {
                    Log.d(TAG, "Unknown cmd: 0x${data[6].toInt().toString(16)}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse device notify data", e)
        }
    }
}
