package com.example.nearbychater

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal fun requiredPermissions(): Array<String> {
    val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.NEARBY_WIFI_DEVICES
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions += Manifest.permission.BLUETOOTH_SCAN
        permissions += Manifest.permission.BLUETOOTH_CONNECT
        permissions += Manifest.permission.BLUETOOTH_ADVERTISE
    } else {
        permissions += Manifest.permission.BLUETOOTH
        permissions += Manifest.permission.BLUETOOTH_ADMIN
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }

    return permissions.distinct().toTypedArray()
}

internal fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean =
    permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
