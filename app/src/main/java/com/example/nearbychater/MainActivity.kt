package com.example.nearbychater

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nearbychater.ui.theme.NearbyChaterTheme

class MainActivity : ComponentActivity() {
    private var backCallback: OnBackInvokedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logHighRefreshRateSupport()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerBackInvokedCallback()
        }
        setContent { NearbyChaterTheme { NearbyChaterApp() } }
    }

    override fun onDestroy() {
        unregisterBackInvokedCallback()
        super.onDestroy()
    }

    private fun registerBackInvokedCallback() {
        val callback = OnBackInvokedCallback {
            Log.d("BackInvoked", "onBackInvoked called")
        }
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_SYSTEM_NAVIGATION_OBSERVER,
            callback
        )
        backCallback = callback
    }

    private fun unregisterBackInvokedCallback() {
        backCallback?.let(onBackInvokedDispatcher::unregisterOnBackInvokedCallback)
        backCallback = null
    }
}
