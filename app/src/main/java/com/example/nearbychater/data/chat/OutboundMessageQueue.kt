package com.example.nearbychater.data.chat

import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.MessageStatus
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel

/** In-memory durable-until-confirmed queue with bounded exponential retry metadata. */
internal class OutboundMessageQueue(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val baseDelayMs: Long = 1_000L,
    private val maxDelayMs: Long = 60_000L,
    private val maxAttempts: Int = 5
) {
    data class RetryMetadata(val attempts: Int = 0, val nextRetryAt: Long = 0L)
    private val pendingSends = ConcurrentHashMap<String, ChatMessage>()
    private val retryMetadata = ConcurrentHashMap<String, RetryMetadata>()
    private val flushTrigger = Channel<Unit>(Channel.CONFLATED)

    fun enqueue(message: ChatMessage) { pendingSends[message.id] = message }
    fun remove(messageId: String) { pendingSends.remove(messageId); retryMetadata.remove(messageId) }
    fun triggerFlush() { flushTrigger.trySend(Unit) }
    suspend fun runFlushLoop(flushQueuedMessages: suspend () -> Unit) { for (ignored in flushTrigger) flushQueuedMessages() }
    fun queuedMessages(conversations: Collection<ConversationSnapshot>): List<ChatMessage> =
        conversations.flatMap { it.messages.filter { m ->
            m.status == MessageStatus.QUEUED || (m.status == MessageStatus.FAILED && isRetryDue(m.id))
        }}
    fun recordFailure(messageId: String): RetryMetadata {
        val previous = retryMetadata[messageId] ?: RetryMetadata()
        val attempts = previous.attempts + 1
        val delay = (baseDelayMs * (1L shl (attempts - 1).coerceAtMost(30))).coerceAtMost(maxDelayMs)
        val metadata = RetryMetadata(attempts, clock() + delay)
        retryMetadata[messageId] = metadata
        return metadata
    }
    fun retryNow(messageId: String) { retryMetadata[messageId] = RetryMetadata(); triggerFlush() }
    fun isRetryDue(messageId: String): Boolean = (retryMetadata[messageId]?.nextRetryAt ?: 0L) <= clock()
    fun attempts(messageId: String): Int = retryMetadata[messageId]?.attempts ?: 0
    fun canRetry(messageId: String): Boolean = attempts(messageId) < maxAttempts
}
