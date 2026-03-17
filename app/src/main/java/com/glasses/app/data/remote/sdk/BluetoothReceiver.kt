package com.glasses.app.data.remote.sdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import org.greenrobot.eventbus.EventBus

/**
 * 蓝牙系统状态监听器
 * 监听蓝牙开关、设备连接等系统级事件
 * 参考官方demo的BluetoothReceiver.kt实现
 */
class BluetoothReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BluetoothReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val connectState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                when (connectState) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.i(TAG, "蓝牙关闭了")
                        BleOperateManager.getInstance().setBluetoothTurnOff(false)
                        BleOperateManager.getInstance().disconnect()
                        
                        // 通知SDK管理器更新状态
                        val sdkManager = GlassesSDKManager.getInstance(context)
                        sdkManager.updateConnectionState(ConnectionState.DISCONNECTED)
                        
                        // 发送EventBus事件
                        EventBus.getDefault().post(BluetoothEvent(false))
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.i(TAG, "蓝牙开启了")
                        BleOperateManager.getInstance().setBluetoothTurnOff(true)
                        
                        // 尝试重连上次连接的设备
                        val lastAddress = DeviceManager.getInstance().deviceAddress
                        if (!lastAddress.isNullOrEmpty()) {
                            BleOperateManager.getInstance().reConnectMac = lastAddress
                            BleOperateManager.getInstance().connectDirectly(lastAddress)
                        }
                    }
                }
            }
            
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                // 配对状态变化
                Log.d(TAG, "Bond state changed")
            }
            
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                // ACL连接建立
                Log.d(TAG, "ACL connected")
            }
            
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                // ACL连接断开
                Log.d(TAG, "ACL disconnected")
            }
            
            BluetoothDevice.ACTION_FOUND -> {
                // 发现设备（经典蓝牙）
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    // 当蓝牙地址和当前BLE地址相等时调用配对
                    BleOperateManager.getInstance().createBondBluetoothJieLi(device)
                }
            }
        }
    }
}

/**
 * 蓝牙事件
 * 用于EventBus传递连接状态
 */
data class BluetoothEvent(val connected: Boolean)
