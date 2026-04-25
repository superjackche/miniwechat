package com.example.nearbychater.data.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.nearbychater.MainActivity
import com.example.nearbychater.R
import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.MemberId
import com.example.nearbychater.core.model.MemberProfile
import com.example.nearbychater.core.model.MessageType

private const val MESSAGE_CHANNEL_ID = "NearbyChater_messages"

internal class NotificationPresenter(private val context: Context) {
    fun showIfBackground(
            message: ChatMessage,
            members: Map<MemberId, MemberProfile>
    ) {
        if (!isAppInForeground()) {
            show(message, members)
        }
    }

    private fun show(
            message: ChatMessage,
            members: Map<MemberId, MemberProfile>
    ) {
        val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                            MESSAGE_CHANNEL_ID,
                            "Messages",
                            NotificationManager.IMPORTANCE_HIGH
                    )
            notificationManager.createNotificationChannel(channel)
        }

        val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
        val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val senderProfile = members[message.senderId]
        val senderName =
                senderProfile?.let {
                    it.localNickname?.takeIf { nickname -> nickname.isNotBlank() }
                            ?: it.remoteNickname?.takeIf { nickname -> nickname.isNotBlank() }
                            ?: it.deviceModel?.takeIf { model -> model.isNotBlank() }
                }
                        ?: message.senderId.take(6)

        val contentText =
                when (message.type) {
                    MessageType.IMAGE -> "[图片]"
                    else -> message.content
                }

        val notification =
                NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("${senderName} 发来消息")
                        .setContentText(contentText)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()

        notificationManager.notify(message.id.hashCode(), notification)
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get()
                .lifecycle
                .currentState
                .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    }
}
