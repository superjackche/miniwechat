package com.example.nearbychater.data.chat

import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.MessageStatus
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel

internal class OutboundMessageQueue {
    private val pendingSends = ConcurrentHashMap<String, ChatMessage>()
    private val flushTrigger = Channel<Unit>(Channel.CONFLATED)

    fun enqueue(message: ChatMessage) {
        pendingSends[message.id] = message
    }

    fun remove(messageId: String) {
        pendingSends.remove(messageId)
    }

    fun triggerFlush() {
        flushTrigger.trySend(Unit)
    }

    suspend fun runFlushLoop(flushQueuedMessages: suspend () -> Unit) {
        for (ignored in flushTrigger) {
            flushQueuedMessages()
        }
    }

    fun queuedMessages(conversations: Collection<ConversationSnapshot>): List<ChatMessage> {
        return conversations.flatMap { snapshot ->
            snapshot.messages.filter {
                it.status == MessageStatus.QUEUED || it.status == MessageStatus.FAILED
            }
        }
    }
}
