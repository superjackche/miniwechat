package com.example.nearbychater

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nearbychater.ui.state.ChatViewModel
import com.example.nearbychater.ui.state.SettingsViewModel
import com.example.nearbychater.ui.state.SettingsViewModelFactory

@Composable
fun NearbyChaterApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val app = application as NearbyChaterApplication
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            application = application,
            chatRepository = app.chatRepository
        )
    )

    val permissions = remember { requiredPermissions() }
    var hasPermissions by remember { mutableStateOf(hasAllPermissions(context, permissions)) }
    var showConnectivityWarning by remember { mutableStateOf(false) }
    val backgroundServiceEnabled by settingsViewModel.backgroundServiceEnabled.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions = result.entries.all { it.value } || hasAllPermissions(context, permissions)
    }

    LaunchedEffect(permissions, hasPermissions, backgroundServiceEnabled) {
        if (!hasPermissions && permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        } else if (hasPermissions) {
            handleForegroundService(context, backgroundServiceEnabled)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasPermissions) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasPermissions) {
                showConnectivityWarning = !checkConnectivity(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showConnectivityWarning) {
        val enableBluetoothLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { showConnectivityWarning = !checkConnectivity(context) }
        )

        ConnectivityWarningDialog(
            onDismiss = { showConnectivityWarning = false },
            onEnableBluetooth = {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
            onGoToSettings = {
                context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            },
            isBluetoothMissing = !checkBluetooth(context),
            isWifiMissing = !checkWifi(context)
        )
    }

    if (hasPermissions || permissions.isEmpty()) {
        NearbyChaterNavHost(
            modifier = Modifier.fillMaxSize(),
            chatViewModel = chatViewModel,
            settingsViewModel = settingsViewModel
        )
    } else {
        PermissionRequiredScreen(
            onRequestAgain = {
                if (permissions.isNotEmpty()) permissionLauncher.launch(permissions)
            }
        )
    }
}
