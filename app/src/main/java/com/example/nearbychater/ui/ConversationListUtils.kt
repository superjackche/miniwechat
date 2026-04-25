package com.example.nearbychater.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatConversationTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)

}
