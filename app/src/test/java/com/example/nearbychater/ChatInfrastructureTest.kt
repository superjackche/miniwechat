package com.example.nearbychater.data.chat

import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.MeshEnvelope
import com.example.nearbychater.core.model.MessageStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ChatInfrastructureTest {
    private fun message(id: String, status: MessageStatus = MessageStatus.QUEUED) = ChatMessage(id, "c", "sender", id, status = status)

    @Test fun queueSelectsQueuedAndFailedOnly() {
        val queue = OutboundMessageQueue()
        val snapshots = listOf(ConversationSnapshot("c", listOf(message("q"), message("f", MessageStatus.FAILED), message("s", MessageStatus.SENT))))
        assertEquals(setOf("q", "f"), queue.queuedMessages(snapshots).map { it.id }.toSet())
        queue.enqueue(message("q")); queue.remove("q"); queue.triggerFlush()
    }

    @Test fun flushTriggerWakesLoopWithoutNearby() = runBlocking {
        val queue = OutboundMessageQueue(); val signal = Channel<Unit>(1)
        val job = async { queue.runFlushLoop { signal.send(Unit) } }
        queue.triggerFlush(); withTimeout(1000) { signal.receive() }; job.cancel()
    }

    @Test fun identityExcludesLocalAndSortsMembers() {
        val identity = ConversationIdentity("me")
        assertEquals("a:b", identity.conversationIdFromMembers(setOf("b", "me", "a")))
        assertEquals(SELF_SUMMARY_ID, identity.conversationKey(setOf("me")))
        assertEquals(ConversationTarget.Remote(setOf("a", "b")), identity.targetFor(ConversationSnapshot("x", memberIds = setOf("me", "b", "a"))))
        assertEquals(ConversationTarget.Self, identity.targetFor(ConversationSnapshot("x", memberIds = setOf("me"))))
    }

    @Test fun dedupeAcceptsOnceAndEvictsOldest() {
        val policy = PacketDedupePolicy(2)
        assertTrue(policy.shouldProcess("1")); assertFalse(policy.shouldProcess("1"))
        assertTrue(policy.shouldProcess("2")); assertTrue(policy.shouldProcess("3")); assertTrue(policy.shouldProcess("1"))
    }

    @Test fun meshEnvelopeDefaultsSupportBoundedForwarding() {
        val envelope = MeshEnvelope("c", message("m"), "origin")
        assertEquals(0, envelope.hopCount); assertEquals(MeshEnvelope.DEFAULT_MAX_HOPS, envelope.maxHops)
        assertTrue(envelope.hopCount < envelope.maxHops)
        assertFalse(envelope.participants.contains("origin"))
    }
}
