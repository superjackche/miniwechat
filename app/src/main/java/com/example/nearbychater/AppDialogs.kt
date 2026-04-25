package com.example.nearbychater

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PermissionRequiredScreen(onRequestAgain: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "需要附近设备、蓝牙和位置信息权限才能互相发现。")
            Button(onClick = onRequestAgain, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = "重新授权")
            }
        }
    }
}

@Composable
internal fun ConnectivityWarningDialog(
    onDismiss: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onGoToSettings: () -> Unit,
    isBluetoothMissing: Boolean,
    isWifiMissing: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要开启连接功能") },
        text = {
            val missing = buildList {
                if (isBluetoothMissing) add("蓝牙")
                if (isWifiMissing) add("Wi-Fi")
            }
            Text("为了发现附近的设备，请开启: ${missing.joinToString(" 和 ")}。")
        },
        confirmButton = {
            if (isBluetoothMissing && !isWifiMissing) {
                TextButton(
                    onClick = {
                        onEnableBluetooth()
                        onDismiss()
                    }
                ) { Text("开启蓝牙") }
            } else {
                TextButton(
                    onClick = {
                        onGoToSettings()
                        onDismiss()
                    }
                ) { Text("去设置") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
