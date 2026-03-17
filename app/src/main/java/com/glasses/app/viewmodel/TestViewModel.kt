package com.glasses.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glasses.app.data.remote.sdk.GlassesSDKManager
import com.glasses.app.manager.RecordingManager
import com.glasses.app.manager.RecordingState
import com.glasses.app.ui.test.DeviceInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TestUiState(
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val isRecording: Boolean = false,
    val batteryLevel: Int = 0,
    val deviceName: String? = null,
    val devices: List<DeviceInfo> = emptyList(),
    val logs: List<String> = emptyList()
)

class TestViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sdkManager = GlassesSDKManager.getInstance(application)
    private val recordingManager = RecordingManager.getInstance(application)
    
    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    init {
        observeConnectionState()
        observeBatteryLevel()
        observeRecordingState()
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            sdkManager.connectionState.collect { state ->
                val isConnected = state == com.glasses.app.data.remote.sdk.ConnectionState.CONNECTED
                _uiState.update { it.copy(isConnected = isConnected) }
                addLog("连接状态: ${if (isConnected) "已连接" else "未连接"}")
            }
        }
    }
    
    private fun observeBatteryLevel() {
        viewModelScope.launch {
            sdkManager.batteryLevel.collect { level ->
                _uiState.update { it.copy(batteryLevel = level) }
                if (level > 0) {
                    addLog("电量更新: $level%")
                }
            }
        }
    }
    
    private fun observeRecordingState() {
        viewModelScope.launch {
            recordingManager.recordingState.collect { state ->
                _uiState.update { it.copy(isRecording = state.isRecording) }
                
                when (state) {
                    is RecordingState.Idle -> addLog("录音状态: 空闲")
                    is RecordingState.Recording -> addLog("录音状态: 录音中")
                    is RecordingState.Processing -> addLog("录音状态: 处理中 ${(state.progress * 100).toInt()}%")
                    is RecordingState.Completed -> addLog("录音完成: ${state.audioFile.name}")
                    is RecordingState.Error -> addLog("录音错误: ${state.message}")
                }
            }
        }
    }
    
    fun startScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, devices = emptyList()) }
            addLog("开始扫描设备...")
            
            sdkManager.scanDevices().collect { scannedDevice ->
                val currentDevices = _uiState.value.devices
                val deviceList = currentDevices + DeviceInfo(
                    name = scannedDevice.name,
                    address = scannedDevice.address
                )
                _uiState.update { it.copy(devices = deviceList) }
                addLog("发现设备: ${scannedDevice.name}")
            }
        }
    }
    
    fun stopScan() {
        sdkManager.stopScan()
        _uiState.update { it.copy(isScanning = false) }
        addLog("停止扫描")
    }
    
    fun connectDevice(device: DeviceInfo) {
        viewModelScope.launch {
            addLog("正在连接: ${device.name}")
            _uiState.update { it.copy(isScanning = false) }
            
            val result = sdkManager.connect(device.address)
            if (result.isSuccess) {
                addLog("连接成功: ${device.name}")
            } else {
                addLog("连接失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            addLog("正在断开连接...")
            val result = sdkManager.disconnect()
            if (result.isSuccess) {
                addLog("断开连接成功")
                _uiState.update { it.copy(deviceName = null, batteryLevel = 0) }
            } else {
                addLog("断开连接失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            addLog("开始录音...")
            val result = recordingManager.startRecording()
            if (result.isSuccess) {
                addLog("录音已启动")
            } else {
                addLog("录音启动失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            addLog("停止录音...")
            val result = recordingManager.stopRecording()
            if (result.isSuccess) {
                val audioFile = result.getOrNull()
                addLog("录音已保存: ${audioFile?.name}")
            } else {
                addLog("录音停止失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    private fun addLog(message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $message"
        _uiState.update { 
            it.copy(logs = (it.logs + logMessage).takeLast(50)) // 保留最近50条日志
        }
    }
}
