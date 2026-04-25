package com.example.nearbychater.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nearbychater.core.model.MemberProfile

@Composable
internal fun AddContactDialog(
        selfId: String,
        onDismiss: () -> Unit,
        onConfirm: (List<String>) -> Unit
) {
    var memberInput by remember { mutableStateOf("") }
    val parsedMembers = remember(selfId, memberInput) { parseMemberIds(memberInput, selfId) }
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "添加联系人") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "输入一个或多个设备 ID，使用逗号、空格或换行分隔。")
                    OutlinedTextField(
                            value = memberInput,
                            onValueChange = { memberInput = it },
                            singleLine = false,
                            maxLines = 4,
                            label = { Text("成员 ID 列表") },
                            placeholder = { Text("例如：abc123, def456") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onConfirm(parsedMembers) },
                        enabled = parsedMembers.isNotEmpty()
                ) { Text("创建会话") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

internal fun parseMemberIds(raw: String, selfId: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val delimiters = charArrayOf(',', ';', ' ', '\n')
    return raw.split(*delimiters)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != selfId }
            .distinct()
}

// CreateGroupDialog: 创建群聊对话框
// 支持选择多个成员
@Composable
internal fun CreateGroupDialog(
        members: List<MemberProfile>,
        selfId: String,
        onDismiss: () -> Unit,
        onConfirm: (List<String>) -> Unit
) {
    val availableMembers = remember(members, selfId) { members.filter { it.memberId != selfId } }
    // 选中的成员ID集合
    var selectedMemberIds by remember { mutableStateOf(emptySet<String>()) }
    
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "创建群聊") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "选择要添加到群聊的成员：")
                    // 成员选择列表
                    LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(count = availableMembers.size) { index ->
                            val member = availableMembers[index]
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedMemberIds = 
                                        if (selectedMemberIds.contains(member.memberId)) {
                                            selectedMemberIds - member.memberId
                                        } else {
                                            selectedMemberIds + member.memberId
                                        }
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 头像气泡
                                AvatarBubble(seed = member.memberId, title = member.memberId)
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
                                // 复选框
                                Checkbox(
                                    checked = selectedMemberIds.contains(member.memberId),
                                    onCheckedChange = { _ ->
                                        selectedMemberIds = 
                                            if (selectedMemberIds.contains(member.memberId)) {
                                                selectedMemberIds - member.memberId
                                            } else {
                                                selectedMemberIds + member.memberId
                                            }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onConfirm(selectedMemberIds.toList()) },
                        enabled = selectedMemberIds.isNotEmpty()
                ) {
                    Text("创建群聊")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
    )
}
