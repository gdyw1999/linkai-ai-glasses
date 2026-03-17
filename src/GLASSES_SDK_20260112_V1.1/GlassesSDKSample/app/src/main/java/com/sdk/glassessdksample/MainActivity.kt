package com.sdk.glassessdksample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.ILargeDataResponse
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.resp.GlassModelControlResponse
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyListener
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp
import com.oudmon.ble.base.communication.utils.ByteUtil
import com.oudmon.wifi.GlassesControl
import com.oudmon.wifi.bean.GlassAlbumEntity
import com.sdk.glassessdksample.databinding.AcitivytMainBinding
import com.sdk.glassessdksample.ui.BluetoothUtils
import com.sdk.glassessdksample.ui.DeviceBindActivity
import com.sdk.glassessdksample.ui.MyApplication
import com.sdk.glassessdksample.ui.hasBluetooth
import com.sdk.glassessdksample.ui.requestAllPermission
import com.sdk.glassessdksample.ui.requestBluetoothPermission
import com.sdk.glassessdksample.ui.requestLocationPermission
import com.sdk.glassessdksample.ui.setOnClickListener
import com.sdk.glassessdksample.ui.startKtxActivity
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val TAG = "HeyCyanSDK"
    private lateinit var binding: AcitivytMainBinding
    private val deviceNotifyListener by lazy { MyDeviceNotifyListener() }


    private val requestedPermissions = buildList {
        add(Manifest.permission.INTERNET)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.CHANGE_WIFI_STATE)
        add(Manifest.permission.ACCESS_NETWORK_STATE)
        add(Manifest.permission.CHANGE_NETWORK_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    private val requestPermissionLaunch = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { it ->
        if (it.all { it.value }) {

            GlassesControl.getInstance(MyApplication.getInstance)?.importAlbum()
        } else {
            Log.i("sdk", "Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AcitivytMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    inner class PermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            } else {
                startKtxActivity<DeviceBindActivity>()
            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if (never) {
                XXPermissions.startPermissionActivity(this@MainActivity, permissions);
            }
        }

    }


    fun initListener() {
        GlassesControl.getInstance(MyApplication.getInstance())
            ?.initGlasses(MyApplication.getInstance().getAlbumDirFile().absolutePath)
        GlassesControl.getInstance(MyApplication.getInstance())
            ?.setWifiDownloadListener(object : GlassesControl.WifiFilesDownloadListener {
                override fun eisEnd(fileName: String, filePath: String) {
                    Log.i(TAG, "eisEnd fileName: $fileName filePath: $filePath")
                }

                override fun eisError(fileName: String, sourcePath: String, errorInfo: String) {
                    Log.i(
                        TAG,
                        "eisEnd fileName: $fileName filePath: $sourcePath errorInfo: $errorInfo"
                    )
                }

                override fun fileCount(index: Int, total: Int) {
                    Log.i(TAG, "fileCount index: $index total: $total")
                }

                override fun fileDownloadComplete() {
                    Log.i(TAG, "fileDownloadComplete")
                }

                override fun fileDownloadError(fileType: Int, errorType: Int) {
                    Log.i(TAG, "fileDownloadError fileType: $fileType errorType: $errorType")
                }

                override fun fileProgress(fileName: String, progress: Int) {
                    Log.i(TAG, "fileProgress fileName: $fileName progress: $progress")
                }

                override fun fileWasDownloadSuccessfully(entity: GlassAlbumEntity) {
                    Log.i(TAG, "fileWasDownloadSuccessfully entity: $entity")
                }

                override fun onGlassesControlSuccess() {
                    Log.i(TAG, "onGlassesControlSuccess")
                }

                override fun onGlassesFail(errorCode: Int) {
                    Log.i(TAG, "onGlassesFail errorCode: $errorCode")
                }

                override fun recordingToPcm(fileName: String, filePath: String, duration: Int) {
                    Log.i(
                        TAG,
                        "recordingToPcm fileName: $fileName filePath: $filePath duration: $duration"
                    )
                }

                override fun recordingToPcmError(fileName: String, errorInfo: String) {
                    Log.i(TAG, "recordingToPcmError fileName: $fileName errorInfo: $errorInfo")
                }

                override fun voiceFromGlasses(pcmData: ByteArray) {
                    Log.i(TAG, ByteUtil.bytesToString(pcmData))
                    writeToFile1(
                        pcmData,
                        MyApplication.getInstance().getAlbumDirFile().absolutePath,
                        "test.pcm"
                    )
                }

                override fun voiceFromGlassesStatus(status: Int) {
                    Log.i(TAG, "voiceFromGlassesStatus$status")
                }

                override fun wifiSpeed(wifiSpeed: String) {
                    Log.i(TAG, "wifiSpeed wifiSpeed: $wifiSpeed")
                }


            })
    }

    override fun onResume() {
        super.onResume()
        try {
            if (!BluetoothUtils.isEnabledBluetooth(this)) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            this, Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                }
                startActivityForResult(intent, 300)
            }
        } catch (e: Exception) {
        }
        if (!hasBluetooth(this)) {
            requestBluetoothPermission(this, BluetoothPermissionCallback())
        }

        requestAllPermission(this, OnPermissionCallback { permissions, all -> })
    }

    inner class BluetoothPermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if (never) {
                XXPermissions.startPermissionActivity(this@MainActivity, permissions)
            }
        }

    }

    fun createFile(path: String, fileName: String): Boolean {
        val file = File("$path/$fileName")
        return if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            true
        } else {
            //
            false
        }
    }

    fun writeToFile1(data: ByteArray, path: String, fileName: String) {
        val file = File("$path/$fileName")
        if (!file.parentFile?.exists()!!) {
            file.parentFile?.mkdirs()
        }
        if (!file.exists()) {
            createFile(path, fileName)
        }
        var bos: BufferedOutputStream? = null
        try {
            val fileOutputStream = FileOutputStream(file, true)
            bos = BufferedOutputStream(fileOutputStream)
            bos.write(data)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (bos != null) {
                try {
                    bos.close()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
            }
        }
    }

    private fun initView() {
        requestAllPermission(this, OnPermissionCallback { permissions, all -> })
        MyApplication.getInstance.createDirs(MyApplication.getInstance.getAlbumDirFile().absolutePath)
        initListener()
        setOnClickListener(
            binding.btnScan,
            binding.btnConnect,
            binding.btnDisconnect,
            binding.btnAddListener,
            binding.btnSetTime,
            binding.btnVersion,
            binding.btnCamera,
            binding.btnVideo,
            binding.btnRecord,
            binding.btnThumbnail,
            binding.btnBt,
            binding.btnBattery,
            binding.btnVolume,
            binding.btnMediaCount,
            binding.btnSyncPicture,
            binding.stopVoice
        ) {
            when (this) {
                binding.btnScan -> {
                    requestLocationPermission(this@MainActivity, PermissionCallback())
                }

                binding.btnConnect -> {
                    BleOperateManager.getInstance()
                        .connectDirectly(DeviceManager.getInstance().deviceAddress)
                }

                binding.btnDisconnect -> {
                    BleOperateManager.getInstance().unBindDevice()
                }

                binding.btnAddListener -> {
                    LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                }

                binding.btnSetTime -> {
                    Log.i("setTime", "setTime" + BleOperateManager.getInstance().isConnected)
                    LargeDataHandler.getInstance().syncTime { _, _ -> }
                }

                binding.btnVersion -> {
                    LargeDataHandler.getInstance().syncDeviceInfo { _, response ->
                        if (response != null) {
                            //wifi firmware version
                            response.wifiFirmwareVersion
                            //wifi hardware version
                            response.wifiHardwareVersion
                            //bluetooth hardware version
                            response.hardwareVersion
                            //bluetooth  firmware version
                            response.firmwareVersion
                        }
                    }
                }

                binding.btnCamera -> {
                    LargeDataHandler.getInstance().glassesControl(
                        byteArrayOf(0x02, 0x01, 0x01)
                    ) { _, it ->
                        if (it.dataType == 1) {
                            if (it.errorCode == 0 || it.errorCode == 0xff) {
                                when (it.workTypeIng) {
                                    2 -> {
                                        // Glasses are recording
                                    }

                                    4 -> {
                                        // Glasses are in transfer mode
                                    }

                                    5 -> {
                                        // Glasses are in OTA mode
                                    }

                                    1, 6 -> {
                                        // Glasses are in photo mode
                                    }

                                    7 -> {
                                        // Glasses are in AI conversation
                                    }

                                    8 -> {
                                        // Glasses are in audio recording mode
                                    }

                                    0xff -> {
                                        //成功
                                    }
                                }
                            }
                        } else {
                            // Execution starts and ends
                        }
                    }
                }

                binding.btnVideo -> {
                    //videoStart true to start recording false to stop recording
                    val videoStart = true
                    val value = if (videoStart) 0x02 else 0x03
                    LargeDataHandler.getInstance().glassesControl(
                        byteArrayOf(0x02, 0x01, value.toByte())
                    ) { _, it ->
                        if (it.dataType == 1) {
                            if (it.errorCode == 0) {
                                when (it.workTypeIng) {
                                    2 -> {
                                        // Glasses are recording
                                    }

                                    4 -> {
                                        // Glasses are in transfer mode
                                    }

                                    5 -> {
                                        // Glasses are in OTA mode
                                    }

                                    1, 6 -> {
                                        // Glasses are in photo mode
                                    }

                                    7 -> {
                                        // Glasses are in AI conversation
                                    }

                                    8 -> {
                                        // Glasses are in audio recording mode
                                    }
                                }
                            } else {
                                // Execution starts and ends
                            }
                        }
                    }
                }

                binding.btnRecord -> {
                    //recordStart  true 开始录制   false 停止录制
                    val recordStart = true
                    val value = if (recordStart) 0x08 else 0x0c
                    LargeDataHandler.getInstance().glassesControl(
                        byteArrayOf(0x02, 0x01, value.toByte())
                    ) { _, it ->
                        if (it.dataType == 1) {
                            if (it.errorCode == 0) {
                                when (it.workTypeIng) {
                                    2 -> {
                                        // Glasses are recording
                                    }

                                    4 -> {
                                        // Glasses are in transfer mode
                                    }

                                    5 -> {
                                        // Glasses are in OTA mode
                                    }

                                    1, 6 -> {
                                        // Glasses are in photo mode
                                    }

                                    7 -> {
                                        // Glasses are in AI conversation
                                    }

                                    8 -> {
                                        // Glasses are in audio recording mode
                                    }
                                }
                            } else {
                                // Execution starts and ends
                            }
                        }
                    }
                }

                binding.btnThumbnail -> {
                    //thumbnailSize  0..6
                    val thumbnailSize = 0x02
                    LargeDataHandler.getInstance().glassesControl(
                        byteArrayOf(
                            0x02, 0x01, 0x06, thumbnailSize.toByte(), thumbnailSize.toByte(), 0x02
                        )
                    ) { _, it ->
                        if (it.dataType == 1) {
                            if (it.errorCode == 0) {
                                when (it.workTypeIng) {
                                    2 -> {
                                        // Glasses are recording
                                    }

                                    4 -> {
                                        // Glasses are in transfer mode
                                    }

                                    5 -> {
                                        // Glasses are in OTA mode
                                    }

                                    1, 6 -> {
                                        // Glasses are in photo mode
                                    }

                                    7 -> {
                                        // Glasses are in AI conversation
                                    }

                                    8 -> {
                                        // Glasses are in audio recording mode
                                    }
                                }
                            } else {
                                // Execution starts and ends
                            }
                        }
                    }
                }

                binding.btnBt -> {
                    //BT scan
                    LargeDataHandler.getInstance().openBT()
                    BleOperateManager.getInstance().classicBluetoothStartScan()

                }

                binding.btnBattery -> {
                    //Add power monitoring
                    LargeDataHandler.getInstance().addBatteryCallBack("init") { _, response ->

                    }
                    //power
                    LargeDataHandler.getInstance().syncBattery()
                }

                binding.btnVolume -> {
                    //Reading the volume control
                    LargeDataHandler.getInstance().getVolumeControl { _, response ->
                        if (response != null) {
                            //Glasses volume Music minimum value Maximum value Current value
                            response.minVolumeMusic
                            response.maxVolumeMusic
                            response.currVolumeMusic
                            //Glasses Phone Phone Minimum Maximum Current Value
                            response.minVolumeCall
                            response.maxVolumeCall
                            response.currVolumeCall
                            // Glasses system minimum value maximum value current value
                            response.minVolumeSystem
                            response.maxVolumeSystem
                            response.currVolumeSystem
                            //Glasses current mode
                            response.currVolumeType
                        }
                    }
                }

                binding.btnMediaCount -> {
                    LargeDataHandler.getInstance()
                        .glassesControl(byteArrayOf(0x02, 0x04)) { _, it ->
                            if (it.dataType == 4) {
                                val mediaCount = it.imageCount + it.videoCount + it.recordCount
                                if (mediaCount > 0) {
                                    //How many media have not been uploaded?
                                } else {
                                    //empty
                                }
                            }
                        }
                }
                binding.stopVoice->{
                    LargeDataHandler.getInstance().glassesControl(
                        byteArrayOf(0x02, 0x01, 0x0b)
                    ) { cmdType, response -> }
                }

                binding.btnSyncPicture -> {
                    //Multimedia for glasses
                    //Requires Wi-Fi permission first
                    requestPermissionLaunch.launch(requestedPermissions)
                }
            }
        }
    }

    inner class MyDeviceNotifyListener : GlassesDeviceNotifyListener() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
            when (response.loadData[6].toInt()) {
                //Glasses battery reporting
                0x05 -> {
                    //Current power
                    val battery = response.loadData[7].toInt()
                    //Is it charging?
                    val changing = response.loadData[8].toInt()
                }
                //Glasses through quick recognition
                0x02 -> {
                    if (response.loadData.size > 9 && response.loadData[9].toInt() == 0x02) {
                        //To set the recognition intent: eg Please help me see what is in front of me, the content of the picture
                    }
                    //Get image thumbnail
                    LargeDataHandler.getInstance().getPictureThumbnails { cmdType, success, data ->
                        //Please save the data into the path, jpg picture
                    }
                }

                0x03 -> {
                    if (response.loadData[7].toInt() == 1) {
                        //The glasses activate the microphone and start speaking
                    }
                }
                //ota upgrade
                0x04 -> {
                    try {
                        val download = response.loadData[7].toInt()
                        val soc = response.loadData[8].toInt()
                        val nor = response.loadData[9].toInt()
                        //download Firmware download progress soc download progress nor upgrade progress
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                0x0c -> {
                    //The glasses trigger a pause event and voice broadcast
                    if (response.loadData[7].toInt() == 1) {
                        //to do
                    }
                }

                0x0d -> {
                    //Unbind APP event
                    if (response.loadData[7].toInt() == 1) {
                        //to do
                    }
                }
                //Glasses low memory event
                0x0e -> {

                }
                //Translation pause event
                0x10 -> {

                }
                //Glasses volume change event
                0x12 -> {
                    //Music volume
                    //Minimum volume
                    response.loadData[8].toInt()
                    //Maximum volume
                    response.loadData[9].toInt()
                    //Current volume
                    response.loadData[10].toInt()

                    //Incoming call volume
                    //Minimum volume
                    response.loadData[12].toInt()
                    //Maximum volume
                    response.loadData[13].toInt()
                    //Current volume
                    response.loadData[14].toInt()

                    //Glass system volume
                    //Minimum volume
                    response.loadData[16].toInt()
                    //Maximum volume
                    response.loadData[17].toInt()
                    //Current volume
                    response.loadData[18].toInt()

                    //Current volume mode
                    response.loadData[19].toInt()

                }
            }
        }
    }
}