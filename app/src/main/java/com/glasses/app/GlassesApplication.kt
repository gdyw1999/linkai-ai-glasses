package com.glasses.app

import android.app.Application
import android.content.IntentFilter
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.oudmon.ble.base.bluetooth.BleAction
import com.oudmon.ble.base.bluetooth.BleBaseControl
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.glasses.app.data.remote.sdk.BluetoothReceiver
import com.glasses.app.data.remote.sdk.GlassesBluetoothCallback

/**
 * Application类
 * 参考官方demo的SDK初始化流程
 */
class GlassesApplication : Application() {

    companion object {
        lateinit var CONTEXT: Application
            private set
    }

    override fun onCreate() {
        super.onCreate()
        CONTEXT = this
        
        // 初始化SDK
        initBle()
    }

    private fun initBle() {
        initReceiver()
        
        // 注册SDK回调监听器
        val intentFilter = BleAction.getIntentFilter()
        val glassesCallback = GlassesBluetoothCallback()
        LocalBroadcastManager.getInstance(CONTEXT)
            .registerReceiver(glassesCallback, intentFilter)
        
        // 设置上下文
        BleBaseControl.getInstance(CONTEXT).setmContext(this)
        
        // 注册设备通知监听器（暂时注释，等待SDK确认方法名称）
        // val deviceListener = GlassesDeviceListener(CONTEXT)
        // LargeDataHandler.getInstance().setGlassesDeviceNotifyListener(deviceListener)
    }

    private fun initReceiver() {
        // 初始化SDK管理器
        LargeDataHandler.getInstance()
        BleOperateManager.getInstance(this)
        BleOperateManager.getInstance().setApplication(this)
        BleOperateManager.getInstance().init()
        
        // 注册蓝牙系统状态监听
        val deviceFilter: IntentFilter = BleAction.getDeviceIntentFilter()
        val deviceReceiver = BluetoothReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(deviceReceiver, deviceFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(deviceReceiver, deviceFilter)
        }
    }
}
