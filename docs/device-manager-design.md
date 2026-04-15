# 统一外设接口设计方案

## 背景

当前 Android App 直接依赖眼镜 SDK，功能耦合严重。未来需要接入更多外设（智能笔、平板等），需要统一的外设抽象层。

## 目标

1. **解耦外设依赖**：App 不直接依赖具体设备 SDK
2. **统一调用接口**：所有外设通过同一套 API 操作
3. **易于扩展**：新增外设只需实现接口，无需改动主逻辑
4. **类型安全**：编译期检查设备能力

---

## 核心设计

### 1. 设备类型枚举

```kotlin
/**
 * 设备类型
 */
enum class DeviceType {
    GLASSES,      // AR眼镜
    SMART_PEN,    // 智能笔
    TABLET,       // 平板
    CAMERA,       // 摄像头
    MICROPHONE,   // 麦克风
    UNKNOWN       // 未知设备
}
```

### 2. 设备能力定义

```kotlin
/**
 * 设备能力 - 设备支持的功能
 */
enum class DeviceCapability {
    // 基础连接
    CONNECT,
    DISCONNECT,
    GET_BATTERY,

    // 媒体采集
    TAKE_PHOTO,
    RECORD_VIDEO,
    RECORD_AUDIO,
    SYNC_MEDIA,

    // 交互
    VOICE_WAKEUP,
    KEY_PRESS,
    TOUCH_INPUT,

    // 传感器
    GET_ORIENTATION,
    GET_LOCATION,
    GET_MOTION_DATA
}

/**
 * 设备信息
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val capabilities: Set<DeviceCapability>,
    val version: String? = null,
    val manufacturer: String? = null
)
```

### 3. 统一外设接口

```kotlin
/**
 * 智能设备接口 - 所有外设必须实现
 */
interface SmartDevice {
    // ========== 基础信息 ==========
    val deviceId: String
    val deviceInfo: DeviceInfo

    // ========== 连接管理 ==========
    val connectionState: Flow<ConnectionState>
    suspend fun connect(): Result<Unit>
    suspend fun disconnect(): Result<Unit>

    // ========== 基础能力 ==========
    suspend fun getBatteryLevel(): Result<Int>

    // ========== 媒体采集（可选实现）==========
    suspend fun takePhoto(): Result<MediaFile>
    suspend fun startVideoRecording(): Result<Unit>
    suspend fun stopVideoRecording(): Result<MediaFile>
    suspend fun startAudioRecording(): Result<Unit>
    suspend fun stopAudioRecording(): Result<MediaFile>
    suspend fun syncMedia(): Result<List<MediaFile>>

    // ========== 交互（可选实现）==========
    suspend fun enableVoiceWakeup(keyword: String): Result<Unit>
    suspend fun disableVoiceWakeup(): Result<Unit>

    // ========== 扩展命令 ==========
    suspend fun executeCommand(command: DeviceCommand): Result<DeviceResponse>

    // ========== 能力检查 ==========
    fun supportsCapability(capability: DeviceCapability): Boolean
}

/**
 * 连接状态
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * 设备命令 - 通用命令结构
 */
data class DeviceCommand(
    val action: String,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * 设备响应
 */
data class DeviceResponse(
    val success: Boolean,
    val data: Map<String, Any> = emptyMap(),
    val error: String? = null
)
```

### 4. 设备管理器

```kotlin
/**
 * 设备管理器 - 单例，统一管理所有外设
 */
class DeviceManager private constructor() {

    companion object {
        @Volatile
        private var instance: DeviceManager? = null

        fun getInstance(): DeviceManager {
            return instance ?: synchronized(this) {
                instance ?: DeviceManager().also { instance = it }
            }
        }
    }

    private val devices = mutableMapOf<String, SmartDevice>()
    private val _deviceStates = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())
    val deviceStates: StateFlow<Map<String, ConnectionState>> = _deviceStates.asStateFlow()

    /**
     * 注册设备
     */
    fun registerDevice(device: SmartDevice) {
        devices[device.deviceId] = device

        // 监听连接状态变化
        viewModelScope.launch {
            device.connectionState.collect { state ->
                _deviceStates.value = _deviceStates.value.toMutableMap().apply {
                    put(device.deviceId, state)
                }
            }
        }
    }

    /**
     * 注销设备
     */
    fun unregisterDevice(deviceId: String) {
        devices.remove(deviceId)
    }

    /**
     * 获取设备
     */
    fun getDevice(deviceId: String): SmartDevice? = devices[deviceId]

    /**
     * 获取所有设备
     */
    fun getAllDevices(): List<SmartDevice> = devices.values.toList()

    /**
     * 按类型获取设备
     */
    fun getDevicesByType(type: DeviceType): List<SmartDevice> {
        return devices.values.filter { it.deviceInfo.deviceType == type }
    }

    /**
     * 获取已连接的设备
     */
    fun getConnectedDevices(): List<SmartDevice> {
        val currentState = _deviceStates.value
        return devices.values.filter {
            currentState[it.deviceId] is ConnectionState.Connected
        }
    }

    // ========== 便捷操作方法 ==========

    /**
     * 统一拍照接口
     */
    suspend fun takePhoto(deviceId: String): Result<MediaFile> {
        val device = getDevice(deviceId)
            ?: return Result.failure(DeviceNotFoundException(deviceId))

        if (!device.supportsCapability(DeviceCapability.TAKE_PHOTO)) {
            return Result.failure(UnsupportedCapabilityException("TAKE_PHOTO"))
        }

        return device.takePhoto()
    }

    /**
     * 统一录音接口
     */
    suspend fun startAudioRecording(deviceId: String): Result<Unit> {
        val device = getDevice(deviceId)
            ?: return Result.failure(DeviceNotFoundException(deviceId))

        if (!device.supportsCapability(DeviceCapability.RECORD_AUDIO)) {
            return Result.failure(UnsupportedCapabilityException("RECORD_AUDIO"))
        }

        return device.startAudioRecording()
    }

    /**
     * 批量同步所有设备的媒体文件
     */
    suspend fun syncAllMedia(): Map<String, List<MediaFile>> {
        val results = mutableMapOf<String, List<MediaFile>>()

        devices.values.forEach { device ->
            if (device.supportsCapability(DeviceCapability.SYNC_MEDIA)) {
                device.syncMedia()
                    .onSuccess { files -> results[device.deviceId] = files }
                    .onFailure { results[device.deviceId] = emptyList() }
            }
        }

        return results
    }
}

/**
 * 设备未找到异常
 */
class DeviceNotFoundException(deviceId: String) : Exception("Device not found: $deviceId")

/**
 * 不支持的能力异常
 */
class UnsupportedCapabilityException(capability: String) : Exception("Unsupported capability: $capability")
```

---

## 眼镜设备实现示例

```kotlin
/**
 * AR眼镜设备实现
 */
class GlassesDevice(
    private val context: Context,
    override val deviceId: String,
    private val sdkManager: GlassesSDKManager
) : SmartDevice {

    override val deviceInfo = DeviceInfo(
        deviceId = deviceId,
        deviceName = "AR智能眼镜",
        deviceType = DeviceType.GLASSES,
        capabilities = setOf(
            DeviceCapability.CONNECT,
            DeviceCapability.DISCONNECT,
            DeviceCapability.GET_BATTERY,
            DeviceCapability.TAKE_PHOTO,
            DeviceCapability.RECORD_AUDIO,
            DeviceCapability.RECORD_VIDEO,
            DeviceCapability.SYNC_MEDIA,
            DeviceCapability.VOICE_WAKEUP
        )
    )

    override val connectionState: Flow<ConnectionState> =
        sdkManager.connectionState.map { state ->
            when (state) {
                is ConnectionState.Connected -> ConnectionState.Connected
                is ConnectionState.Connecting -> ConnectionState.Connecting
                is ConnectionState.Disconnected -> ConnectionState.Disconnected
                else -> ConnectionState.Error("Unknown error")
            }
        }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sdkManager.connect(deviceId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sdkManager.disconnect()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBatteryLevel(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val level = sdkManager.getBatteryLevel()
            Result.success(level)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun takePhoto(): Result<MediaFile> = withContext(Dispatchers.IO) {
        try {
            val path = MediaCaptureManager.getInstance(context).takePhotoSuspend()
            Result.success(MediaFile(path, MediaType.IMAGE))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startAudioRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            RecordingManager.getInstance(context).startRecording()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopAudioRecording(): Result<MediaFile> = withContext(Dispatchers.IO) {
        try {
            val file = RecordingManager.getInstance(context).stopRecordingSuspend()
            Result.success(MediaFile(file.absolutePath, MediaType.AUDIO))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncMedia(): Result<List<MediaFile>> = withContext(Dispatchers.IO) {
        try {
            val files = MediaSyncManager.getInstance(context).syncAndGetAllFiles()
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableVoiceWakeup(keyword: String): Result<Unit> {
        // 语音唤醒逻辑
        return Result.success(Unit)
    }

    override suspend fun disableVoiceWakeup(): Result<Unit> {
        // 禁用语音唤醒
        return Result.success(Unit)
    }

    override suspend fun executeCommand(command: DeviceCommand): Result<DeviceResponse> {
        // 扩展命令处理
        return Result.success(DeviceResponse(true))
    }

    // 以下方法眼镜不支持
    override suspend fun startVideoRecording(): Result<Unit> =
        Result.failure(UnsupportedCapabilityException("RECORD_VIDEO"))

    override suspend fun stopVideoRecording(): Result<MediaFile> =
        Result.failure(UnsupportedCapabilityException("RECORD_VIDEO"))

    override fun supportsCapability(capability: DeviceCapability): Boolean {
        return capability in deviceInfo.capabilities
    }
}
```

---

## 智能笔设备实现示例（未来）

```kotlin
/**
 * 智能笔设备实现
 */
class SmartPenDevice(
    private val context: Context,
    override val deviceId: String,
    private val penSdk: SmartPenSDK
) : SmartDevice {

    override val deviceInfo = DeviceInfo(
        deviceId = deviceId,
        deviceName = "AI智能笔",
        deviceType = DeviceType.SMART_PEN,
        capabilities = setOf(
            DeviceCapability.CONNECT,
            DeviceCapability.DISCONNECT,
            DeviceCapability.GET_BATTERY,
            DeviceCapability.RECORD_AUDIO,     // 笔可以录音
            DeviceCapability.TOUCH_INPUT,      // 笔触输入
            DeviceCapability.GET_MOTION_DATA   // 运动数据
        )
    )

    override val connectionState: Flow<ConnectionState> =
        penSdk.connectionState.map { /* 转换 */ }

    override suspend fun connect(): Result<Unit> =
        penSdk.connect(deviceId).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )

    override suspend fun disconnect(): Result<Unit> =
        penSdk.disconnect()

    override suspend fun getBatteryLevel(): Result<Int> =
        penSdk.getBattery()

    override suspend fun takePhoto(): Result<MediaFile> =
        Result.failure(UnsupportedCapabilityException("TAKE_PHOTO"))

    override suspend fun startAudioRecording(): Result<Unit> =
        penSdk.startRecording()

    override suspend fun stopAudioRecording(): Result<MediaFile> =
        penSdk.stopRecording().map { path -> MediaFile(path, MediaType.AUDIO) }

    override suspend fun syncMedia(): Result<List<MediaFile>> =
        penSdk.getRecordings().map { list -> list.map { MediaFile(it, MediaType.AUDIO) } }

    // 智能笔特有方法
    suspend fun getStrokeData(): Result<List<Stroke>> =
        penSdk.getStrokeData()

    override fun supportsCapability(capability: DeviceCapability): Boolean =
        capability in deviceInfo.capabilities
}
```

---

## 使用示例

### 注册和使用设备

```kotlin
// 在 Application 中注册设备
class ZhijiaoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val deviceManager = DeviceManager.getInstance()

        // 注册眼镜设备
        val glasses = GlassesDevice(
            context = this,
            deviceId = "glasses_001",
            sdkManager = GlassesSDKManager.getInstance(this)
        )
        deviceManager.registerDevice(glasses)
    }
}

// 在 ViewModel 中使用
class HomeViewModel : ViewModel() {
    private val deviceManager = DeviceManager.getInstance()

    fun takePhotoWithGlasses() {
        viewModelScope.launch {
            val glasses = deviceManager.getDevicesByType(DeviceType.GLASSES).firstOrNull()
                ?: return@launch

            glasses.takePhoto()
                .onSuccess { file -> /* 处理照片 */ }
                .onFailure { /* 处理错误 */ }
        }
    }

    fun syncAllDevices() {
        viewModelScope.launch {
            val results = deviceManager.syncAllMedia()
            // results["glasses_001"] -> 眼镜的媒体文件
            // results["pen_001"] -> 智能笔的录音文件
        }
    }
}
```

### 设备列表 UI

```kotlin
@Composable
fun DeviceListScreen() {
    val deviceManager = DeviceManager.getInstance()
    val deviceStates by deviceManager.deviceStates.collectAsState()
    val devices = remember { deviceManager.getAllDevices() }

    LazyColumn {
        items(devices) { device ->
            DeviceCard(
                device = device,
                state = deviceStates[device.deviceId]
            )
        }
    }
}

@Composable
fun DeviceCard(device: SmartDevice, state: ConnectionState?) {
    Row {
        Text(text = device.deviceInfo.deviceName)
        Text(text = device.deviceInfo.deviceType.name)
        Text(text = state?.toString() ?: "Unknown")

        // 根据设备能力显示按钮
        if (device.supportsCapability(DeviceCapability.TAKE_PHOTO)) {
            Button(onClick = { /* 拍照 */ }) {
                Text("拍照")
            }
        }
    }
}
```

---

## 迁移计划

### 阶段1：准备工作（当前不做）
- [ ] 定义 `SmartDevice` 接口
- [ ] 定义 `DeviceManager` 单例
- [ ] 定义设备类型和能力枚举

### 阶段2：眼镜设备适配（当前不做）
- [ ] 创建 `GlassesDevice` 实现 `SmartDevice`
- [ ] 迁移 `GlassesSDKManager` 调用
- [ ] 迁移 `MediaCaptureManager` 到 `DeviceManager`

### 阶段3：验证（当前不做）
- [ ] 单元测试：Mock 设备测试接口
- [ ] 集成测试：真实眼镜连接测试

### 阶段4：新外设接入（未来）
- [ ] 智能笔：`SmartPenDevice` 实现
- [ ] 其他外设：按需添加

---

## 注意事项

1. **向后兼容**：现有代码暂时不动，新接口并行开发
2. **渐进迁移**：先在新功能中使用新接口，旧功能逐步迁移
3. **测试覆盖**：新接口必须有单元测试
4. **文档更新**：每个新设备实现需要更新本文档

---

## 相关文件

- `app/src/main/java/com/glasses/app/device/SmartDevice.kt` - 设备接口
- `app/src/main/java/com/glasses/app/device/DeviceManager.kt` - 设备管理器
- `app/src/main/java/com/glasses/app/device/glasses/GlassesDevice.kt` - 眼镜实现
- `app/src/main/java/com/glasses/app/device/pen/SmartPenDevice.kt` - 智能笔实现（未来）
