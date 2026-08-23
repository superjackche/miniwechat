package com.example.nearbychater

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.nearbychater.data.service.ChatForegroundService

internal fun handleForegroundService(context: Context, enabled: Boolean) {
    val serviceIntent = Intent(context, ChatForegroundService::class.java)
    if (enabled) {
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: SecurityException) {
            // Permission denied (including POST_NOTIFICATIONS/FGS permission).
        } catch (_: ForegroundServiceStartNotAllowedException) {
            // Android 14+ background-start restriction; retry from a visible activity.
        }
    } else {
        context.stopService(serviceIntent)
    }
}
