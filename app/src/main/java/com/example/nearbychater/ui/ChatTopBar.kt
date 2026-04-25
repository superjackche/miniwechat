package com.example.nearbychater.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatTopBar(
        title: String,
        subtitle: String,
        onBack: () -> Unit,
        menuExpanded: Boolean,
        onToggleMenu: () -> Unit,
        onDismissMenu: () -> Unit,
        canRename: Boolean,
        onRenameConversation: () -> Unit,
        onOpenSettings: () -> Unit,
        onOpenLogs: () -> Unit,
        isGroupChat: Boolean = false,
        onViewMembers: () -> Unit = {}
) {
    Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 2.dp
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)) // 添加状态栏顶部内边距
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = onToggleMenu) {
                    Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                    if (canRename) {
                        DropdownMenuItem(text = { Text("重命名聊天") }, onClick = onRenameConversation)
                    }
                    if (isGroupChat) {
                        DropdownMenuItem(text = { Text("查看成员") }, onClick = onViewMembers)
                    }
                    DropdownMenuItem(text = { Text("设置") }, onClick = onOpenSettings)
                    DropdownMenuItem(text = { Text("开发者日志") }, onClick = onOpenLogs)
                }
            }
        }
    }
}
