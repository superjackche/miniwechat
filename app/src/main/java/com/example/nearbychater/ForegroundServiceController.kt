package com.example.nearbychater

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.nearbychater.data.service.ChatForegroundService

internal fun handleForegroundService(context: Context, enabled: Boolean) {
    val serviceIntent = Intent(context, ChatForegroundService::class.java)
    if (enabled) {
        ContextCompat.startForegroundService(context, serviceIntent)
    } else {
        context.stopService(serviceIntent)
    }
}
