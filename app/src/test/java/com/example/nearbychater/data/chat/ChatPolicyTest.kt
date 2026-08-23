package com.example.nearbychater.data.chat

import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.MemberProfile
import com.example.nearbychater.core.model.MessageStatus
import org.junit.Assert.*
import org.junit.Test

class ChatPolicyTest {
    @Test fun dedupePolicyRejectsDuplicatesAndEvictsOldest() {
        val policy = PacketDedupePolicy(maxEntries = 2)
        assertTrue(policy.shouldProcess("a"))
        assertTrue(policy.shouldProcess("b"))
        assertTrue(policy.shouldProcess("a"))
        // Access-order cache keeps a as most recently used, so b is evicted.
        assertTrue(policy.shouldProcess("c"))
        assertTrue(policy.shouldProcess("b"))
        assertFalse(policy.shouldProcess("a"))
    }

    @Test fun identityExcludesLocalMemberAndSortsMembers() {
        val identity = ConversationIdentity("local")
        assertEquals("alice:bob", identity.conversationKey(setOf("bob", "local", "alice")))
        assertEquals(SELF_SUMMARY_ID, identity.conversationIdFromMembers(setOf("local")))
        assertEquals(ConversationTarget.Self, identity.targetFor(ConversationSnapshot("id", memberIds = setOf("local"))))
        assertEquals(ConversationTarget.Remote(setOf("alice")), identity.targetFor(ConversationSnapshot("id", memberIds = setOf("local", "alice"))))
        assertEquals(ConversationTarget.Unknown, identity.targetFor(null))
    }

    @Test fun summaryBuilderFormatsPreviewUnreadAndPinnedOrdering() {
        val identity = ConversationIdentity("me")
        val builder = ConversationSummaryBuilder("me", identity)
        val old = ChatMessage(id = "1", conversationId = "c1", senderId = "them", content = "old", timestamp = 1, status = MessageStatus.QUEUED)
        val newer = ChatMessage(id = "2", conversationId = "c1", senderId = "me", content = "x".repeat(45), timestamp = 2, status = MessageStatus.SENT)
        val snapshots = mapOf(
            "c1" to ConversationSnapshot("c1", listOf(old, newer), setOf("me", "them"), isPinned = true),
            "self" to ConversationSnapshot("self", emptyList(), setOf("me"))
        )
        val result = builder.build(snapshots, mapOf("them" to MemberProfile("them", remoteNickname = "Bob")))
        assertEquals(2, result.size)
        assertTrue(result[0].isSelf)
        assertEquals("Bob", result[1].title)
        assertEquals("x".repeat(40) + "...", result[1].preview)
        assertEquals(1, result[1].unreadCount)
        assertTrue(result[1].isPinned)
    }
}
