package com.glasses.app.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasses.app.viewmodel.TestViewModel

@Composable
fun TestScreen(
    viewModel: TestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Linkai星韵AI眼镜测试界面",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 连接状态卡片
        ConnectionStatusCard(
            isConnected = uiState.isConnected,
            batteryLevel = uiState.batteryLevel,
            deviceName = uiState.deviceName
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 蓝牙控制按钮
        BluetoothControlButtons(
            isScanning = uiState.isScanning,
            isConnected = uiState.isConnected,
            onStartScan = { viewModel.startScan() },
            onStopScan = { viewModel.stopScan() },
            onDisconnect = { viewModel.disconnect() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 设备列表
        if (uiState.devices.isNotEmpty()) {
            DeviceList(
                devices = uiState.devices,
                onDeviceClick = { device -> viewModel.connectDevice(device) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 录音测试按钮
        RecordingTestButtons(
            isRecording = uiState.isRecording,
            isConnected = uiState.isConnected,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 日志显示
        LogDisplay(logs = uiState.logs)
    }
}

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    batteryLevel: Int,
    deviceName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "连接状态",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isConnected) "已连接" else "未连接",
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (isConnected && deviceName != null) {
                Text(text = "设备: $deviceName")
                Text(text = "电量: $batteryLevel%")
            }
        }
    }
}

@Composable
fun BluetoothControlButtons(
    isScanning: Boolean,
    isConnected: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = if (isScanning) onStopScan else onStartScan,
            modifier = Modifier.weight(1f),
            enabled = !isConnected
        ) {
            Text(if (isScanning) "停止扫描" else "扫描设备")
        }
        
        if (isConnected) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("断开连接")
            }
        }
    }
}

@Composable
fun DeviceList(
    devices: List<DeviceInfo>,
    onDeviceClick: (DeviceInfo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "发现的设备 (${devices.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.height(200.dp)
            ) {
                items(devices) { device ->
                    TextButton(
                        onClick = { onDeviceClick(device) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(device.name)
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun RecordingTestButtons(
    isRecording: Boolean,
    isConnected: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Button(
        onClick = if (isRecording) onStopRecording else onStartRecording,
        modifier = Modifier.fillMaxWidth(),
        enabled = isConnected,
        colors = if (isRecording) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        Text(if (isRecording) "停止录音" else "开始录音")
    }
}

@Composable
fun LogDisplay(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "日志",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.height(200.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

data class DeviceInfo(
    val name: String,
    val address: String
)
