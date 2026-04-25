package com.example.nearbychater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nearbychater.core.model.MemberId
import com.example.nearbychater.core.model.MemberProfile

@Composable
internal fun RenameConversationDialog(
        initialValue: String,
        onConfirm: (String) -> Unit,
        onReset: () -> Unit,
        onDismiss: () -> Unit
) {
    var text by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("设置聊天名称") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            label = { Text("聊天名称") },
                            placeholder = { Text("默认使用对方设备名") }
                    )
                    if (initialValue.isNotBlank()) {
                        TextButton(onClick = onReset) { Text("恢复默认") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("保存") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

internal fun defaultConversationTitle(
        members: List<MemberProfile>,
        memberIds: List<MemberId>
): String {
    if (memberIds.isEmpty()) return "我"
    if (members.isEmpty()) return memberIds.joinToString(", ") { it.take(6) }
    if (members.size == 1) {
        return members.first().preferredName(memberIds.first())
    }
    val names = members.map { profile -> profile.preferredName(profile.memberId) }
    return if (names.size <= 3) names.joinToString("、")
    else names.take(2).joinToString("、") + " 等${names.size}人"
}

private fun MemberProfile.preferredName(fallbackId: MemberId): String {
    return localNickname?.takeIf { it.isNotBlank() }
            ?: remoteNickname?.takeIf { it.isNotBlank() } ?: deviceModel?.takeIf { it.isNotBlank() }
                    ?: fallbackId.take(6)
}

@Composable
internal fun GroupMembersDialog(
    members: List<MemberProfile>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "群成员") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "共 ${members.size} 位成员")
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(count = members.size) { index ->
                        MemberItem(member = members[index])
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

// MemberItem: 成员列表项
@Composable
private fun MemberItem(member: MemberProfile) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 头像气泡
        Box(
            modifier = Modifier.size(40.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.localNickname?.firstOrNull()?.uppercaseChar()?.toString()
                    ?: member.remoteNickname?.firstOrNull()?.uppercaseChar()?.toString()
                    ?: member.memberId.take(1).uppercase(),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        // 成员信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.localNickname ?: member.remoteNickname ?: member.memberId.take(6),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (member.isOnline) "在线" else "离线",
                style = MaterialTheme.typography.labelSmall,
                color = if (member.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
