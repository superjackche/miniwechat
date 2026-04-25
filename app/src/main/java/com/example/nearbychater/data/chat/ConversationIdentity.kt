package com.example.nearbychater.data.chat

import com.example.nearbychater.core.model.ConversationId
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.MemberId

internal const val SELF_SUMMARY_ID = "__self__"

internal class ConversationIdentity(private val localMemberId: MemberId) {
    fun conversationIdFromMembers(memberIds: Set<MemberId>): ConversationId {
        val remoteMembers = memberIds.filterNot { it == localMemberId }.sorted()
        return if (remoteMembers.isEmpty()) SELF_SUMMARY_ID else remoteMembers.joinToString(":")
    }

    fun conversationKey(memberIds: Set<MemberId>): String {
        val remoteMembers = memberIds.filterNot { it == localMemberId }.sorted()
        return if (remoteMembers.isEmpty()) SELF_SUMMARY_ID else remoteMembers.joinToString(":")
    }

    fun targetFor(snapshot: ConversationSnapshot?): ConversationTarget {
        snapshot ?: return ConversationTarget.Unknown
        val remoteMembers = snapshot.memberIds.filterNot { it == localMemberId }.toSet()
        return if (remoteMembers.isEmpty()) ConversationTarget.Self
        else ConversationTarget.Remote(remoteMembers)
    }
}

internal sealed interface ConversationTarget {
    data object Unknown : ConversationTarget
    data object Self : ConversationTarget
    data class Remote(val memberIds: Set<MemberId>) : ConversationTarget
}
