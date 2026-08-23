package com.example.nearbychater

import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.MessageStatus
import com.example.nearbychater.data.chat.ConversationIdentity
import com.example.nearbychater.data.chat.ConversationTarget
import com.example.nearbychater.data.chat.OutboundMessageQueue
import com.example.nearbychater.data.chat.PacketDedupePolicy
import org.junit.Assert.*
import org.junit.Test

class ChatDomainTest {
    @Test fun conversationIdentityIsStableAndExcludesSelf() {
        val identity = ConversationIdentity("self")
        assertEquals("alice:bob", identity.conversationIdFromMembers(setOf("bob", "self", "alice")))
        assertEquals("__self__", identity.conversationKey(setOf("self")))
        assertEquals(ConversationTarget.Remote(setOf("alice")), identity.targetFor(ConversationSnapshot("alice", memberIds = setOf("self", "alice"))))
    }

    @Test fun queueReturnsOnlyRetryableMessages() {
        val queue = OutboundMessageQueue()
        val queued = ChatMessage(id = "q", conversationId = "c", senderId = "self", content = "queued")
        val sent = queued.copy(id = "s", status = MessageStatus.SENT)
        val failed = queued.copy(id = "f", status = MessageStatus.FAILED)
        val snapshot = ConversationSnapshot("c", messages = listOf(queued, sent, failed))
        queue.enqueue(queued)
        assertEquals(listOf(queued, failed), queue.queuedMessages(listOf(snapshot)))
        queue.remove(queued.id)
    }

    @Test fun packetDedupeAcceptsOnceAndEvictsOldEntries() {
        val policy = PacketDedupePolicy(maxEntries = 2)
        assertTrue(policy.shouldProcess("a"))
        assertFalse(policy.shouldProcess("a"))
        policy.shouldProcess("b"); policy.shouldProcess("c")
        assertTrue(policy.shouldProcess("a"))
    }
}
