package com.example.nearbychater

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager

internal fun checkConnectivity(context: Context): Boolean = checkBluetooth(context) && checkWifi(context)

internal fun checkBluetooth(context: Context): Boolean {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return bluetoothManager?.adapter?.isEnabled == true
}

internal fun checkWifi(context: Context): Boolean {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    return wifiManager?.isWifiEnabled == true
}
