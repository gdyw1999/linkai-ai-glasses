package com.glasses.app.data.remote.sdk

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.bluetooth.QCBluetoothCallbackCloneReceiver
import com.oudmon.ble.base.communication.Constants
import com.oudmon.ble.base.communication.LargeDataHandler
import com.glasses.app.GlassesApplication
import org.greenrobot.eventbus.EventBus

/**
 * Linkai星韵AI眼镜SDK蓝牙回调监听器
 * 监听连接状态、服务发现、特征值变化等SDK级事件
 * 参考官方demo的MyBluetoothReceiver.kt实现
 */
class GlassesBluetoothCallback : QCBluetoothCallbackCloneReceiver() {
    
    companion object {
        private const val TAG = "GlassesBluetoothCallback"
    }
    
    /**
     * 连接状态回调
     */
    override fun connectStatue(device: BluetoothDevice?, connected: Boolean) {
        Log.d(TAG, "connectStatue: device=${device?.name}, connected=$connected")
        
        if (device != null && connected) {
            // 连接成功
            if (device.name != null) {
                DeviceManager.getInstance().deviceName = device.name
            }
            
            // 更新SDK管理器状态
            val sdkManager = GlassesSDKManager.getInstance(GlassesApplication.CONTEXT)
            sdkManager.updateConnectionState(ConnectionState.CONNECTED)
            sdkManager.setCurrentDevice(device)
            
        } else {
            // 连接失败或断开
            EventBus.getDefault().post(BluetoothEvent(false))
            
            // 更新SDK管理器状态
            val sdkManager = GlassesSDKManager.getInstance(GlassesApplication.CONTEXT)
            sdkManager.updateConnectionState(ConnectionState.DISCONNECTED)
            sdkManager.setCurrentDevice(null)
        }
    }
    
    /**
     * 服务发现完成回调
     * 必须收到此回调才可以下发其它指令（如设置时间、同步设置项等）
     */
    override fun onServiceDiscovered() {
        Log.d(TAG, "onServiceDiscovered: 服务发现完成")
        
        // 初始化使能
        LargeDataHandler.getInstance().initEnable()
        
        // 标记SDK已就绪
        BleOperateManager.getInstance().isReady = true
        
        // 发送连接成功事件
        EventBus.getDefault().post(BluetoothEvent(true))
    }
    
    /**
     * 特征值变化回调
     */
    override fun onCharacteristicChange(address: String?, uuid: String?, data: ByteArray?) {
        // 处理特征值变化（如电量、传感器数据等）
        Log.d(TAG, "onCharacteristicChange: uuid=$uuid")
    }
    
    /**
     * 特征值读取回调
     */
    override fun onCharacteristicRead(uuid: String?, data: ByteArray?) {
        if (uuid != null && data != null) {
            val value = String(data, Charsets.UTF_8)
            when (uuid) {
                Constants.CHAR_FIRMWARE_REVISION.toString() -> {
                    // 固件版本
                    Log.d(TAG, "Firmware version: $value")
                    // 可以保存到SharedPreferences或发送事件
                }
                Constants.CHAR_HW_REVISION.toString() -> {
                    // 硬件版本
                    Log.d(TAG, "Hardware version: $value")
                    // 可以保存到SharedPreferences或发送事件
                }
            }
        }
    }
}
