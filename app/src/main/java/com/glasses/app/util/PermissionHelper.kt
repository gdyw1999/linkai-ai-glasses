package com.glasses.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 * 处理蓝牙、位置、存储、麦克风等权限请求
 */
class PermissionHelper(val activity: ComponentActivity) {

    companion object {
        /**
         * 获取所需的蓝牙权限列表
         */
        fun getBluetoothPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }

        /**
         * 获取所需的存储权限列表
         */
        fun getStoragePermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

        /**
         * 获取所需的麦克风权限
         */
        fun getAudioPermissions(): Array<String> {
            return arrayOf(Manifest.permission.RECORD_AUDIO)
        }

        /**
         * 获取所有必需的权限
         */
        fun getAllRequiredPermissions(): Array<String> {
            return getBluetoothPermissions() + getStoragePermissions() + getAudioPermissions()
        }

        /**
         * 检查是否已授予所有蓝牙权限
         */
        fun hasBluetoothPermissions(context: Context): Boolean {
            return getBluetoothPermissions().all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * 检查是否已授予所有存储权限
         */
        fun hasStoragePermissions(context: Context): Boolean {
            return getStoragePermissions().all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * 检查是否已授予麦克风权限
         */
        fun hasAudioPermissions(context: Context): Boolean {
            return getAudioPermissions().all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * 检查是否已授予所有必需权限
         */
        fun hasAllRequiredPermissions(context: Context): Boolean {
            return hasBluetoothPermissions(context) && 
                   hasStoragePermissions(context) && 
                   hasAudioPermissions(context)
        }
    }

    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: ((List<String>) -> Unit)? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys.toList()
        
        if (deniedPermissions.isEmpty()) {
            onPermissionsGranted?.invoke()
        } else {
            onPermissionsDenied?.invoke(deniedPermissions)
        }
    }

    /**
     * 请求蓝牙权限
     */
    fun requestBluetoothPermissions(
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit = {}
    ) {
        if (hasBluetoothPermissions(activity)) {
            onGranted()
        } else {
            onPermissionsGranted = onGranted
            onPermissionsDenied = onDenied
            permissionLauncher.launch(getBluetoothPermissions())
        }
    }

    /**
     * 请求存储权限
     */
    fun requestStoragePermissions(
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit = {}
    ) {
        if (hasStoragePermissions(activity)) {
            onGranted()
        } else {
            onPermissionsGranted = onGranted
            onPermissionsDenied = onDenied
            permissionLauncher.launch(getStoragePermissions())
        }
    }

    /**
     * 请求麦克风权限
     */
    fun requestAudioPermissions(
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit = {}
    ) {
        if (hasAudioPermissions(activity)) {
            onGranted()
        } else {
            onPermissionsGranted = onGranted
            onPermissionsDenied = onDenied
            permissionLauncher.launch(getAudioPermissions())
        }
    }

    /**
     * 请求所有必需权限
     */
    fun requestAllPermissions(
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit = {}
    ) {
        if (hasAllRequiredPermissions(activity)) {
            onGranted()
        } else {
            onPermissionsGranted = onGranted
            onPermissionsDenied = onDenied
            permissionLauncher.launch(getAllRequiredPermissions())
        }
    }
}
