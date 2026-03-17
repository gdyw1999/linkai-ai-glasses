package com.glasses.app.data.remote.sdk

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 青橙SDK管理器
 * 封装SDK的设备扫描、连接、断开、电量查询等功能
 * 参考官方demo的SDK调用模式
 */
class GlassesSDKManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "GlassesSDKManager"
        
        @Volatile
        private var instance: GlassesSDKManager? = null

        fun getInstance(context: Context): GlassesSDKManager {
            return instance ?: synchronized(this) {
                instance ?: GlassesSDKManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // 连接状态
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 当前连接的设备
    private var currentDevice: BluetoothDevice? = null
    
    // 自动重连相关
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private var lastDisconnectTime = 0L
    private val reconnectDelayMs = 2000L // 2秒延迟
    private var isReconnecting = false
    
    // 电量相关
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    /**
     * 扫描设备
     * 返回Flow，持续发送扫描到的设备
     */
    fun scanDevices(): Flow<ScannedDevice> = callbackFlow {
        val callback = object : ScanWrapperCallback {
            private val scannedDevices = mutableSetOf<String>()

            override fun onStart() {
                Log.d(TAG, "Scan started")
            }

            override fun onStop() {
                Log.d(TAG, "Scan stopped")
                channel.close()
            }

            override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
                if (device != null && !device.name.isNullOrEmpty()) {
                    // 过滤重复设备
                    if (!scannedDevices.contains(device.address)) {
                        scannedDevices.add(device.address)
                        val scannedDevice = ScannedDevice(
                            name = device.name,
                            address = device.address,
                            rssi = rssi
                        )
                        trySend(scannedDevice)
                        Log.d(TAG, "Device found: ${device.name} - ${device.address}")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
                channel.close(Exception("Scan failed: $errorCode"))
            }

            override fun onParsedData(device: BluetoothDevice?, scanRecord: ScanRecord?) {
                // Not used
            }

            override fun onBatchScanResults(results: MutableList<android.bluetooth.le.ScanResult>?) {
                // Not used
            }
        }

        // 开始扫描
        BleScannerHelper.getInstance().scanDevice(context, null, callback)

        awaitClose {
            // 停止扫描
            BleScannerHelper.getInstance().stopScan(context)
            Log.d(TAG, "Scan closed")
        }
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        BleScannerHelper.getInstance().stopScan(context)
    }

    /**
     * 连接设备
     * @param deviceAddress 设备MAC地址
     */
    suspend fun connect(deviceAddress: String): Result<Unit> = suspendCoroutine { continuation ->
        try {
            _connectionState.value = ConnectionState.CONNECTING
            
            // 使用SDK连接设备
            BleOperateManager.getInstance().connectDirectly(deviceAddress)
            
            // 注意：实际的连接结果会通过BluetoothReceiver的广播接收
            // 这里我们假设连接请求已发送成功
            Log.d(TAG, "Connection request sent for device: $deviceAddress")
            continuation.resume(Result.success(Unit))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect device: $deviceAddress", e)
            _connectionState.value = ConnectionState.DISCONNECTED
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect(): Result<Unit> = suspendCoroutine { continuation ->
        try {
            // 重置重连计数（手动断开不需要自动重连）
            resetReconnectAttempts()
            
            currentDevice?.let { device ->
                BleOperateManager.getInstance().unBindDevice()
                _connectionState.value = ConnectionState.DISCONNECTED
                currentDevice = null
                Log.d(TAG, "Device disconnected: ${device.address}")
                continuation.resume(Result.success(Unit))
            } ?: run {
                Log.w(TAG, "No device to disconnect")
                continuation.resume(Result.success(Unit))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect device", e)
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 获取电量
     */
    suspend fun getBatteryLevel(): Result<Int> = suspendCoroutine { continuation ->
        try {
            // 添加电量回调监听
            LargeDataHandler.getInstance().addBatteryCallBack("glasses_sdk_manager") { _, response ->
                // 回调会在电量数据返回时触发
                Log.d(TAG, "Battery callback received")
            }
            
            // 调用SDK同步电量
            LargeDataHandler.getInstance().syncBattery()
            
            Log.d(TAG, "Battery sync request sent")
            // 返回当前缓存的电量值
            continuation.resume(Result.success(_batteryLevel.value))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery level", e)
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * 更新电量信息
     * 由设备通知监听器调用
     */
    fun updateBatteryInfo(level: Int, charging: Boolean) {
        _batteryLevel.value = level
        _isCharging.value = charging
        Log.d(TAG, "Battery updated: $level%, charging=$charging")
    }

    /**
     * 更新连接状态
     * 由BluetoothReceiver调用
     */
    fun updateConnectionState(state: ConnectionState) {
        val previousState = _connectionState.value
        _connectionState.value = state
        Log.d(TAG, "Connection state updated: $previousState -> $state")
        
        // 处理意外断开的自动重连
        if (previousState == ConnectionState.CONNECTED && state == ConnectionState.DISCONNECTED) {
            handleUnexpectedDisconnect()
        }
        
        // 连接成功时重置重连计数
        if (state == ConnectionState.CONNECTED) {
            reconnectAttempts = 0
            isReconnecting = false
        }
    }
    
    /**
     * 处理意外断开连接
     * 实现最多3次自动重连
     */
    private fun handleUnexpectedDisconnect() {
        lastDisconnectTime = System.currentTimeMillis()
        
        // 如果还有重连次数且不在重连中
        if (reconnectAttempts < maxReconnectAttempts && !isReconnecting) {
            reconnectAttempts++
            isReconnecting = true
            
            Log.d(TAG, "Attempting auto-reconnect ($reconnectAttempts/$maxReconnectAttempts)")
            
            // 延迟后尝试重连
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                attemptReconnect()
            }, reconnectDelayMs)
        } else if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w(TAG, "Max reconnect attempts reached, giving up")
            reconnectAttempts = 0
            isReconnecting = false
        }
    }
    
    /**
     * 尝试重连
     */
    private fun attemptReconnect() {
        val deviceAddress = currentDevice?.address ?: DeviceManager.getInstance().deviceAddress
        
        if (!deviceAddress.isNullOrEmpty()) {
            Log.d(TAG, "Reconnecting to device: $deviceAddress")
            _connectionState.value = ConnectionState.CONNECTING
            
            try {
                BleOperateManager.getInstance().connectDirectly(deviceAddress)
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect failed", e)
                isReconnecting = false
                
                // 如果还有重连次数，继续尝试
                if (reconnectAttempts < maxReconnectAttempts) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        handleUnexpectedDisconnect()
                    }, reconnectDelayMs)
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        } else {
            Log.w(TAG, "No device address to reconnect")
            isReconnecting = false
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    /**
     * 重置重连计数
     * 用于手动断开连接时
     */
    fun resetReconnectAttempts() {
        reconnectAttempts = 0
        isReconnecting = false
    }

    /**
     * 设置当前连接的设备
     */
    fun setCurrentDevice(device: BluetoothDevice?) {
        currentDevice = device
    }

    /**
     * 获取当前连接的设备
     */
    fun getCurrentDevice(): BluetoothDevice? = currentDevice
}

/**
 * 连接状态
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * 扫描到的设备
 */
data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int
)
