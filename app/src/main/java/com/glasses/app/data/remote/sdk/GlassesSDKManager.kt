package com.glasses.app.data.remote.sdk

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Linkai星韵AI眼镜SDK管理器
 * 封装SDK的设备扫描、连接、断开、电量查询等功能
 * 参考官方demo的SDK调用模式
 */
class GlassesSDKManager private constructor(context: Context) {

    // 使用ApplicationContext避免内存泄漏
    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "GlassesSDKManager"
        
        @Volatile
        private var instance: GlassesSDKManager? = null

        fun getInstance(context: Context): GlassesSDKManager {
            return instance ?: synchronized(this) {
                instance ?: GlassesSDKManager(context).also { instance = it }
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
    
    // 电量监控Job
    private var batteryMonitoringJob: Job? = null

    // 设备通知监听器
    private var deviceNotifyListener: GlassesDeviceNotifyListener? = null

    /**
     * 扫描设备
     * 返回Flow，持续发送扫描到的设备
     */
    fun scanDevices(): Flow<ScannedDevice> = callbackFlow {
        // 检查蓝牙权限
        if (!hasBluetoothPermissions()) {
            close(SecurityException("Missing Bluetooth permissions"))
            return@callbackFlow
        }
        
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
        BleScannerHelper.getInstance().scanDevice(appContext, null, callback)

        awaitClose {
            // 停止扫描
            BleScannerHelper.getInstance().stopScan(appContext)
            Log.d(TAG, "Scan closed")
        }
    }
    
    /**
     * 检查蓝牙权限
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        try {
            if (hasBluetoothPermissions()) {
                BleScannerHelper.getInstance().stopScan(appContext)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when stopping scan", e)
        }
    }

    /**
     * 连接设备
     * @param deviceAddress 设备MAC地址
     */
    suspend fun connect(deviceAddress: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            if (!hasBluetoothPermissions()) {
                continuation.resume(Result.failure(SecurityException("Missing Bluetooth permissions")))
                return@suspendCancellableCoroutine
            }
            
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
    suspend fun disconnect(): Result<Unit> = suspendCancellableCoroutine { continuation ->
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
     * 获取当前缓存电量
     */
    fun getBatteryLevel(): Int = _batteryLevel.value
    
    /**
     * 更新电量信息
     * 由设备通知监听器调用
     */
    fun updateBatteryInfo(level: Int, charging: Boolean) {
        // 确保电量值在有效范围内
        val validLevel = level.coerceIn(0, 100)
        _batteryLevel.value = validLevel
        _isCharging.value = charging
        Log.d(TAG, "Battery updated: $validLevel%, charging=$charging")
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
            // 停止电量监控
            stopBatteryMonitoring()
        }
        
        // 连接成功时重置重连计数并启动电量监控
        if (state == ConnectionState.CONNECTED) {
            reconnectAttempts = 0
            isReconnecting = false
            // 注册设备通知监听器（接收电量、按键等推送）
            registerDeviceNotifyListener()
            // 同步一次电量
            syncBattery()
            // 启动周期性电量监控
            startBatteryMonitoring()
        }
    }

    /**
     * 注册设备通知监听器
     * 参考官方demo：LargeDataHandler.getInstance().addOutDeviceListener(100, listener)
     */
    private fun registerDeviceNotifyListener() {
        try {
            // 避免重复注册
            if (deviceNotifyListener == null) {
                deviceNotifyListener = GlassesDeviceNotifyListener(this)
            }
            LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener!!)
            Log.d(TAG, "Device notify listener registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device notify listener", e)
        }
    }

    /**
     * 请求同步电量数据
     * 电量结果通过 GlassesDeviceNotifyListener.parseData() 回调接收
     */
    private fun syncBattery() {
        try {
            LargeDataHandler.getInstance().syncBattery()
            Log.d(TAG, "Battery sync requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync battery", e)
        }
    }
    
    /**
     * 启动周期性电量监控
     * 每30秒请求同步一次电量，结果通过 GlassesDeviceNotifyListener 接收
     */
    private fun startBatteryMonitoring() {
        // 取消之前的监控任务
        stopBatteryMonitoring()

        batteryMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    // 请求同步电量（结果通过 DeviceNotifyListener 回调）
                    syncBattery()
                    Log.d(TAG, "Periodic battery sync requested")

                    // 等待30秒
                    delay(30000)
                } catch (e: Exception) {
                    Log.e(TAG, "Battery monitoring error", e)
                    delay(30000)
                }
            }
        }

        Log.d(TAG, "Battery monitoring started")
    }
    
    /**
     * 停止周期性电量监控
     */
    private fun stopBatteryMonitoring() {
        batteryMonitoringJob?.cancel()
        batteryMonitoringJob = null
        Log.d(TAG, "Battery monitoring stopped")
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
