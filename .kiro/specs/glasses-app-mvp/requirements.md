# 需求文档

## 介绍

青橙AI眼镜Android App是一款为青橙无线AR眼镜开发的配套手机控制应用。该应用通过蓝牙和WiFi与眼镜设备通信，提供设备连接管理、媒体采集控制、AI语音对话、媒体同步管理等核心功能。应用基于Android原生开发，使用Kotlin语言和Jetpack Compose UI框架，集成青橙SDK和LinkAI API，为用户提供便捷的智能眼镜控制体验。

## 术语表

- **App**: 青橙AI眼镜Android应用程序
- **Glasses**: 青橙无线AR眼镜硬件设备
- **SDK**: 青橙提供的Android SDK (LIB_GLASSES_SDK-release.aar)
- **LinkAI**: LinkAI平台提供的AI服务API
- **ASR**: 自动语音识别 (Automatic Speech Recognition)，将语音转换为文字
- **LLM**: 大语言模型 (Large Language Model)，用于对话生成
- **TTS**: 文字转语音 (Text-to-Speech)，将文字转换为语音
- **Session**: 会话，用户与AI助手的一次完整对话
- **Conversation**: 对话记录，包含多轮用户和AI的消息交互
- **PCM**: 脉冲编码调制 (Pulse Code Modulation)，一种音频编码格式
- **WAV**: 波形音频文件格式 (Waveform Audio File Format)
- **OTA**: 空中下载技术 (Over-The-Air)，用于固件升级
- **BLE**: 低功耗蓝牙 (Bluetooth Low Energy)
- **WiFi_Module**: 眼镜设备的WiFi模块，用于媒体文件传输
- **Battery_Level**: 眼镜设备的电池电量百分比 (0-100)
- **Connection_State**: 蓝牙连接状态 (已连接/未连接/连接中)
- **Media_File**: 媒体文件，包括照片、视频、录音
- **Sync_Progress**: 同步进度百分比 (0-100)

## 需求

### 需求 1: 设备连接管理

**用户故事:** 作为用户，我希望能够扫描、连接和管理眼镜设备，以便使用眼镜的各项功能。

#### 验收标准

1. WHEN 用户打开App, THE App SHALL 自动检查蓝牙权限和位置权限
2. IF 蓝牙权限或位置权限未授予, THEN THE App SHALL 显示权限请求对话框并说明权限用途
3. WHEN 用户授予所有必要权限, THE App SHALL 开始扫描周围的Glasses设备
4. WHEN 扫描到Glasses设备, THE App SHALL 在设备列表中显示设备名称和MAC地址
5. WHEN 用户点击设备列表中的某个设备, THE App SHALL 发起蓝牙连接请求
6. WHEN 蓝牙连接成功, THE App SHALL 更新Connection_State为已连接并跳转到首页
7. WHEN 蓝牙连接失败, THE App SHALL 显示错误提示并提供重试选项
8. WHILE Glasses已连接, THE App SHALL 每30秒查询一次Battery_Level
9. WHEN Battery_Level发生变化, THE App SHALL 在首页更新电量显示
10. WHEN 用户在我的页点击断开连接, THE App SHALL 断开蓝牙连接并更新Connection_State为未连接
11. IF 蓝牙连接意外断开, THEN THE App SHALL 显示断开提示并尝试自动重连最多3次

### 需求 2: 媒体采集控制

**用户故事:** 作为用户，我希望通过App控制眼镜进行拍照、录像和录音，以便记录我看到和听到的内容。

#### 验收标准

1. WHILE Glasses已连接, THE App SHALL 在首页显示拍照、录像、录音和智能识图按钮
2. WHEN 用户点击拍照按钮, THE App SHALL 通过SDK发送拍照指令到Glasses
3. WHEN Glasses完成拍照, THE App SHALL 显示拍照成功提示
4. WHEN 用户点击录像按钮且Glasses未在录像, THE App SHALL 发送开始录像指令并将按钮状态改为停止录像
5. WHEN 用户点击停止录像按钮, THE App SHALL 发送停止录像指令并将按钮状态改为开始录像
6. WHEN 用户点击录音按钮且Glasses未在录音, THE App SHALL 发送开始录音指令并将按钮状态改为停止录音
7. WHEN 用户点击停止录音按钮, THE App SHALL 发送停止录音指令并将按钮状态改为开始录音
8. WHEN 用户点击智能识图按钮, THE App SHALL 发送智能识图指令到Glasses
9. IF 媒体采集指令发送失败, THEN THE App SHALL 显示错误提示并记录日志
10. WHILE Glasses正在录像或录音, THE App SHALL 显示录制时长计时器

### 需求 3: AI语音对话

**用户故事:** 作为用户，我希望通过语音与AI助手对话，以便获取信息和帮助。

#### 验收标准

1. WHEN 用户进入AI对话页, THE App SHALL 显示当前会话的历史消息
2. WHEN 用户按住说话按钮, THE App SHALL 开始录音并显示录音动画
3. WHEN 用户松开说话按钮, THE App SHALL 停止录音并将音频文件上传到LinkAI ASR接口
4. WHEN ASR接口返回识别结果, THE App SHALL 在对话页显示用户消息文字
5. WHEN 用户消息显示后, THE App SHALL 将文字和Session ID发送到LinkAI LLM接口
6. WHEN LLM接口返回AI回复, THE App SHALL 在对话页显示AI消息文字
7. WHEN AI消息显示后, THE App SHALL 将AI回复文字发送到LinkAI TTS接口
8. WHEN TTS接口返回语音文件, THE App SHALL 播放语音并在消息旁显示播放按钮
9. WHEN 用户点击消息旁的播放按钮, THE App SHALL 播放该消息的语音
10. WHEN 用户点击新建会话按钮, THE App SHALL 创建新的Conversation并清空对话页
11. WHEN 用户点击历史记录按钮, THE App SHALL 显示所有历史Conversation列表
12. WHEN 用户点击历史Conversation, THE App SHALL 加载该Conversation的所有消息并显示
13. IF ASR、LLM或TTS接口调用失败, THEN THE App SHALL 显示相应错误提示
14. WHEN Glasses按键触发AI对话事件, THE App SHALL 自动开始录音并执行完整对话流程

### 需求 4: 媒体同步管理

**用户故事:** 作为用户，我希望将眼镜上的照片、视频和录音同步到手机，以便查看和管理这些文件。

#### 验收标准

1. WHEN 用户进入相册页, THE App SHALL 显示已同步的Media_File列表
2. WHEN 用户点击同步按钮, THE App SHALL 通过SDK初始化WiFi_Module连接
3. WHEN WiFi_Module连接成功, THE App SHALL 开始扫描Glasses上的未同步Media_File
4. WHEN 扫描完成, THE App SHALL 显示待同步文件数量并开始逐个下载
5. WHILE 文件下载中, THE App SHALL 实时更新Sync_Progress
6. WHEN 单个文件下载完成, THE App SHALL 保存文件到本地存储并更新相册列表
7. WHEN 所有文件下载完成, THE App SHALL 显示同步完成提示
8. IF 下载过程中WiFi连接断开, THEN THE App SHALL 暂停下载并提示用户检查网络
9. WHEN 用户点击相册中的图片, THE App SHALL 全屏显示图片
10. WHEN 用户点击相册中的视频, THE App SHALL 使用视频播放器播放视频
11. WHEN 用户点击相册中的录音, THE App SHALL 使用音频播放器播放录音
12. WHEN 用户选择媒体类型筛选, THE App SHALL 只显示对应类型的Media_File

### 需求 5: 会话数据管理

**用户故事:** 作为用户，我希望我的对话记录能够被保存和管理，以便随时查看历史对话。

#### 验收标准

1. WHEN 用户发送消息或收到AI回复, THE App SHALL 将消息保存到本地数据库
2. WHEN 保存消息时, THE App SHALL 记录消息ID、Conversation ID、角色、内容、音频路径和时间戳
3. WHEN 用户创建新会话, THE App SHALL 在数据库中创建新的Conversation记录
4. WHEN 用户打开历史记录页, THE App SHALL 按更新时间倒序显示所有Conversation
5. WHEN 用户点击某个Conversation, THE App SHALL 从数据库加载该Conversation的所有消息
6. WHEN 用户左滑删除Conversation, THE App SHALL 从数据库删除该Conversation及其所有消息
7. THE App SHALL 为消息表的conversation_id字段创建索引以优化查询性能
8. THE App SHALL 为会话表的updated_at字段创建索引以优化排序性能

### 需求 6: 音频格式转换

**用户故事:** 作为系统，我需要将眼镜录制的PCM音频转换为WAV格式，以便上传到LinkAI API。

#### 验收标准

1. WHEN App从Glasses接收到PCM音频数据, THE App SHALL 验证音频参数为16kHz采样率、16位深度、单声道
2. WHEN 音频参数验证通过, THE App SHALL 为PCM数据添加WAV文件头
3. WHEN WAV文件头添加完成, THE App SHALL 将完整的WAV文件保存到临时目录
4. WHEN WAV文件保存成功, THE App SHALL 将文件路径传递给ASR接口调用模块
5. WHEN ASR接口调用完成, THE App SHALL 删除临时WAV文件以释放存储空间
6. IF PCM音频参数不匹配, THEN THE App SHALL 记录错误日志并提示用户录音失败

### 需求 7: 用户设置和信息

**用户故事:** 作为用户，我希望能够查看常见问题、关于信息和检查更新，以便了解App的使用方法和版本信息。

#### 验收标准

1. WHEN 用户进入我的页, THE App SHALL 显示常见问题、关于和检查更新选项
2. WHEN 用户点击常见问题, THE App SHALL 显示常见问题列表和解答
3. WHEN 用户点击关于, THE App SHALL 显示App版本号、SDK版本和开发者信息
4. WHEN 用户点击检查更新, THE App SHALL 检查是否有新版本可用
5. IF 有新版本可用, THEN THE App SHALL 显示更新提示和更新内容
6. WHEN 用户确认更新, THE App SHALL 跳转到应用商店下载页面

### 需求 8: 错误处理和日志

**用户故事:** 作为开发者，我希望系统能够妥善处理错误并记录日志，以便排查问题和优化用户体验。

#### 验收标准

1. WHEN 任何SDK调用失败, THE App SHALL 记录错误日志包含时间戳、错误类型和错误详情
2. WHEN 任何网络请求失败, THE App SHALL 记录请求URL、参数、响应码和错误信息
3. WHEN 蓝牙连接失败, THE App SHALL 显示用户友好的错误提示而非技术错误信息
4. WHEN 网络不可用, THE App SHALL 提示用户检查网络连接
5. WHEN LinkAI API返回错误, THE App SHALL 根据错误码显示对应的中文提示
6. THE App SHALL 将日志文件保存到应用私有目录
7. WHEN 日志文件大小超过10MB, THE App SHALL 自动清理最旧的日志

### 需求 9: 权限管理

**用户故事:** 作为用户，我希望App能够清晰地说明所需权限的用途，并在必要时引导我授予权限。

#### 验收标准

1. WHEN App首次启动, THE App SHALL 请求蓝牙扫描权限并说明用于发现眼镜设备
2. WHEN App首次启动, THE App SHALL 请求位置权限并说明这是Android蓝牙扫描的系统要求
3. WHEN App首次启动, THE App SHALL 请求存储权限并说明用于保存同步的媒体文件
4. WHEN App首次启动, THE App SHALL 请求麦克风权限并说明用于语音对话功能
5. IF 用户拒绝必要权限, THEN THE App SHALL 显示权限说明对话框并提供跳转设置的按钮
6. WHEN 用户在设置中授予权限后返回App, THE App SHALL 自动检测权限状态并继续操作
7. THE App SHALL 仅在需要使用相关功能时才请求对应权限

### 需求 10: 性能和稳定性

**用户故事:** 作为用户，我希望App运行流畅稳定，不会出现卡顿或崩溃。

#### 验收标准

1. WHEN 相册页加载超过100个Media_File, THE App SHALL 使用分页加载以避免内存溢出
2. WHEN 显示图片缩略图, THE App SHALL 使用图片加载库进行内存优化和缓存
3. WHEN 后台同步Media_File, THE App SHALL 使用后台服务而非阻塞主线程
4. WHEN 播放音频或视频, THE App SHALL 在播放完成或用户离开页面时释放播放器资源
5. THE App SHALL 在主线程执行时间不超过16ms以保持60fps流畅度
6. WHEN App进入后台, THE App SHALL 暂停非必要的后台任务以节省电量
7. THE App SHALL 捕获所有未处理异常并记录日志而非直接崩溃
