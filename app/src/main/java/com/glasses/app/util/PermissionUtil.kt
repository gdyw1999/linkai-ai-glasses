package com.glasses.app.util

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

/**
 * 权限管理工具类
 * 复用自官方demo
 */

fun requestBluetoothPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback
) {
    XXPermissions.with(activity)
        .permission(Permission.BLUETOOTH_SCAN)
        .permission(Permission.BLUETOOTH_CONNECT)
        .permission(Permission.BLUETOOTH_ADVERTISE)
        .permission(Permission.ACCESS_FINE_LOCATION)
        .request(requestCallback)
}

fun hasBluetooth(activity: FragmentActivity): Boolean {
    val permissions = mutableListOf<String>()
    permissions.add(Permission.BLUETOOTH_SCAN)
    permissions.add(Permission.BLUETOOTH_CONNECT)
    permissions.add(Permission.BLUETOOTH_ADVERTISE)
    return XXPermissions.isGranted(activity, permissions)
}

fun hasLocationPermission(activity: FragmentActivity): Boolean {
    return XXPermissions.isGranted(activity, Permission.ACCESS_FINE_LOCATION)
}

fun requestLocationPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity)
        .permission(Permission.ACCESS_COARSE_LOCATION)
        .permission(Permission.ACCESS_FINE_LOCATION)
        .request(requestCallback)
}

fun requestAllPermission(
    activity: FragmentActivity,
    callback: OnPermissionCallback
) {
    XXPermissions.with(activity)
        .permission(Permission.READ_MEDIA_IMAGES)
        .permission(Permission.READ_MEDIA_AUDIO)
        .permission(Permission.READ_MEDIA_VIDEO)
        .request(callback)
}

fun requestRecordAudioPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity)
        .permission(Permission.RECORD_AUDIO)
        .request(requestCallback)
}
