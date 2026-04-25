package com.example.nearbychater.data.chat

import com.example.nearbychater.core.model.AttachmentType
import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.ConversationId
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.ConversationSummary
import com.example.nearbychater.core.model.MemberId
import com.example.nearbychater.core.model.MemberProfile
import com.example.nearbychater.core.model.MessageStatus
import java.util.Locale

internal class ConversationSummaryBuilder(
        private val localMemberId: MemberId,
        private val identity: ConversationIdentity
) {
    fun build(
            conversations: Map<ConversationId, ConversationSnapshot>,
            members: Map<MemberId, MemberProfile>
    ): List<ConversationSummary> {
        val grouped = mutableMapOf<String, ConversationSummary>()
        conversations.values.forEach { snapshot ->
            val summary = snapshot.toSummary(members)
            val key = summary.conversationKey ?: summaryKey(summary)
            val existing = grouped[key]
            if (existing == null || summary.lastTimestamp >= existing.lastTimestamp) {
                grouped[key] = summary
            }
        }

        val summaries = grouped.values.toMutableList()
        if (summaries.none { it.isSelf }) {
            summaries.add(0, selfSummary(conversations))
        }
        val (selfItems, nonSelf) = summaries.partition { it.isSelf }
        val (pinned, unpinned) = nonSelf.partition { it.isPinned }
        return selfItems.sortedByDescending { it.lastTimestamp } +
                pinned.sortedByDescending { it.lastTimestamp } +
                unpinned.sortedByDescending { it.lastTimestamp }
    }

    private fun ConversationSnapshot.toSummary(
            members: Map<MemberId, MemberProfile>
    ): ConversationSummary {
        val remoteMembers = memberIds.filterNot { it == localMemberId }
        val isSelf = remoteMembers.isEmpty()
        val lastMessage = messages.maxByOrNull { it.timestamp }
        val preview =
                lastMessage?.let { formatPreview(it) }
                        ?: if (isSelf) "本机收藏夹" else "Tap to start chatting"
        val unreadCount =
                messages.count { it.senderId != localMemberId && it.status == MessageStatus.QUEUED }
        val avatarSeed = remoteMembers.firstOrNull() ?: localMemberId
        val key = identity.conversationKey(memberIds + localMemberId)
        return ConversationSummary(
                conversationId = conversationId,
                title = if (isSelf) "我" else conversationTitle(remoteMembers, members),
                preview = preview,
                lastTimestamp = lastMessage?.timestamp ?: 0L,
                unreadCount = unreadCount,
                avatarSeed = avatarSeed,
                isSelf = isSelf,
                conversationKey = key,
                isPinned = isPinned
        )
    }

    private fun summaryKey(summary: ConversationSummary): String {
        return if (summary.isSelf) SELF_SUMMARY_ID else summary.title.lowercase(Locale.getDefault())
    }

    private fun selfSummary(conversations: Map<ConversationId, ConversationSnapshot>): ConversationSummary {
        val snapshot = conversations[identity.conversationIdFromMembers(setOf(localMemberId))]
        val lastMessage = snapshot?.messages?.maxByOrNull { it.timestamp }
        val preview = lastMessage?.let { formatPreview(it) } ?: "本机收藏夹"
        val unread =
                snapshot?.messages?.count {
                    it.senderId != localMemberId && it.status == MessageStatus.QUEUED
                }
                        ?: 0
        return ConversationSummary(
                conversationId = identity.conversationIdFromMembers(setOf(localMemberId)),
                title = "我",
                preview = preview,
                lastTimestamp = lastMessage?.timestamp ?: 0L,
                unreadCount = unread,
                avatarSeed = localMemberId,
                isSelf = true,
                conversationKey = SELF_SUMMARY_ID,
                isPinned = snapshot?.isPinned ?: false
        )
    }

    private fun conversationTitle(
            memberIds: List<MemberId>,
            members: Map<MemberId, MemberProfile>
    ): String {
        if (memberIds.isEmpty()) {
            return "我"
        }
        return memberIds.joinToString(separator = ", ") { memberId ->
            val profile = members[memberId]
            profile?.remoteNickname?.takeIf { it.isNotBlank() }
                    ?: profile?.localNickname?.takeIf { it.isNotBlank() } ?: memberId.take(6)
        }
    }

    private fun formatPreview(message: ChatMessage): String {
        return when {
            message.attachment?.type == AttachmentType.PHOTO -> "发送了图片"
            message.content.isBlank() -> "(empty message)"
            message.content.length > 40 -> message.content.take(40) + "..."
            else -> message.content
        }
    }
}
