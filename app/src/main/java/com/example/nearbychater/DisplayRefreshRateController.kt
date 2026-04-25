package com.example.nearbychater

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log

private const val TARGET_REFRESH_RATE_HZ = 120.0f

internal fun Context.logHighRefreshRateSupport() {
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    displayManager.displays.forEach { display ->
        val supportedModes = display.supportedModes
        if (supportedModes.isEmpty()) return@forEach

        val highestMode = supportedModes.maxBy { it.refreshRate }
        val targetMode = supportedModes.firstOrNull { it.refreshRate >= TARGET_REFRESH_RATE_HZ } ?: highestMode
        val supportedRates = supportedModes.joinToString { mode -> "${mode.refreshRate}Hz" }

        Log.d("RefreshRate", "Display ${display.displayId} supports refresh rates: $supportedRates")
        Log.d("RefreshRate", "Selected refresh rate: ${targetMode.refreshRate}Hz")
    }
}
