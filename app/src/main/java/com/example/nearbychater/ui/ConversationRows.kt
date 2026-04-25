package com.example.nearbychater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nearbychater.core.model.ConversationSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationRow(
        summary: ConversationSummary,
        displayTitle: String,
        onClick: () -> Unit,
        onDelete: (() -> Unit)?, // 为null表示不能删除
        onTogglePinned: () -> Unit
) {
    // dismissState: 侧滑状态
    // rememberSwipeToDismissBoxState创建侧滑状态管理器
    // confirmValueChange在侧滑完成时调用
    val dismissState =
            rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            // StartToEnd: 向右滑 -> 置顶/取消置顶
                            SwipeToDismissBoxValue.StartToEnd -> {
                                onTogglePinned()
                                false // 返回false表示不消失，只执行操作
                            }
                            // EndToStart: 向左滑 -> 删除
                            SwipeToDismissBoxValue.EndToStart -> {
                                if (onDelete != null) {
                                    onDelete()
                                }
                                false
                            }
                            else -> true
                        }
                    }
            )

    // showPinBackground: 是否显示置顶背景
    // derivedStateOf{}创建计算状态，当依赖状态变化时自动重算
    val showPinBackground by remember {
        derivedStateOf {
            dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd ||
                    dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
        }
    }

    // showDeleteBackground: 是否显示删除背景
    val showDeleteBackground by remember {
        derivedStateOf {
            dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart ||
                    dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
        }
    }

    Box(modifier = Modifier.padding(horizontal = 4.dp).clip(MaterialTheme.shapes.small)) {
        // SwipeToDismissBox: 支持侧滑的容器
        SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.clip(MaterialTheme.shapes.small),
                enableDismissFromStartToEnd = true, // 启用向右滑
                enableDismissFromEndToStart = onDelete != null, // 只有能删除的才启用向左滑
                // backgroundContent: 背景内容(侧滑时显示)
                backgroundContent = {
                    when {
                        showPinBackground -> PinBackground(isPinned = summary.isPinned)
                        showDeleteBackground -> DeleteBackground()
                    }
                },
                // content: 主内容(会话行内容)
                content = {
                    ConversationRowContent(
                            summary = summary,
                            displayTitle = displayTitle,
                            onClick = onClick
                    )
                }
        )
    }
}

@Composable
internal fun PinBackground(isPinned: Boolean) {
    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary
            )
            Text(
                    text = if (isPinned) "取消置顶" else "置顶",
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun DeleteBackground() {
    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError
            )
            Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun ConversationRowContent(
        summary: ConversationSummary,
        displayTitle: String,
        onClick: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBubble(seed = summary.avatarSeed, title = summary.title)
        Spacer(modifier = Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                )
                if (summary.isPinned) {
                    Text(
                            text = "置顶",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                    Modifier.clip(MaterialTheme.shapes.small)
                                            .background(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.12f
                                                    )
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                    text = summary.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                            if (summary.isSelf) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
        Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                    text = formatConversationTimestamp(summary.lastTimestamp).takeIf { it.isNotEmpty() }
                                    ?: if (summary.isSelf) "置顶" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.unreadCount > 0) {
                UnreadBadge(count = summary.unreadCount)
            }
        }
    }
}

@Composable
internal fun AvatarBubble(seed: String, title: String) {
    val initials = title.firstOrNull()?.uppercaseChar()?.toString() ?: seed.takeLast(2)
    Box(
            modifier =
                    Modifier.size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = initials,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
internal fun UnreadBadge(count: Int) {
    Box(
            modifier =
                    Modifier.clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
                text = if (count > 99) "99+" else count.toString(),
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelSmall
        )
    }
}
