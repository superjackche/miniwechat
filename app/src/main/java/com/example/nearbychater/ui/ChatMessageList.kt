package com.example.nearbychater.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.nearbychater.core.model.Attachment
import com.example.nearbychater.core.model.AttachmentType
import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.MemberProfile
import com.example.nearbychater.core.model.MessageStatus
import com.example.nearbychater.ui.theme.BubbleGray
import com.example.nearbychater.ui.theme.BubbleGrayDark
import com.example.nearbychater.ui.theme.SentBubbleDark
import com.example.nearbychater.ui.theme.SentBubbleLight

@Composable
internal fun MessageList(
        modifier: Modifier = Modifier,
        messages: List<ChatMessage>,
        members: List<MemberProfile>,
        selfId: String,
        onCancel: (String) -> Unit,
        onAttachmentClick: (Attachment) -> Unit,
        onRetry: (String) -> Unit,
        shouldScrollToLatest: Boolean = false,
        onScrollComplete: () -> Unit = {}
) {
    // 将消息列表按时间升序排列，最新消息在列表末尾（底部）
    val sortedMessages = messages.sortedBy { it.timestamp }
    val listState = rememberLazyListState()
    // 自动滚动逻辑：当有新消息且当前位于底部时，自动滚动至最新
    LaunchedEffect(messages.size) {
        if (sortedMessages.isNotEmpty()) {
            // 阈值判断：是否接近底部
            val isNearBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == sortedMessages.size - 1
            if (isNearBottom) {
                listState.scrollToItem(sortedMessages.size - 1)
            }
        }
    }

    // 初始加载时滚动到底部，显示最新消息
    LaunchedEffect(Unit) {
        if (sortedMessages.isNotEmpty()) {
            listState.scrollToItem(sortedMessages.size - 1)
        }
    }

    // 处理输入框点击事件，滚动到最新消息
    LaunchedEffect(shouldScrollToLatest) {
        if (shouldScrollToLatest && sortedMessages.isNotEmpty()) {
            listState.animateScrollToItem(sortedMessages.size - 1)
            onScrollComplete()
        }
    }

    // LazyColumn：按需渲染列表项，优化长列表性能 (类比生成器)
    // 仅渲染可见区域，避免OOM
    // 优化：使用imePadding确保输入法弹出时内容不被遮挡
    LazyColumn(
            modifier = 
                    modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
    ) {
        itemsIndexed(
                items = sortedMessages,
                // key：唯一标识符，优化Compose重组性能 (类似数据库主键)
                key = { _, item -> item.id },
                // contentType：复用视图类型，提升滚动流畅度
                contentType = { _, item -> if (item.senderId == selfId) 1 else 2 }
        ) { index, message ->
            val profile = members.firstOrNull { it.memberId == message.senderId }
            ChatBubble(
                    message = message,
                    isOwn = message.senderId == selfId,
                    profile = profile,
                    onCancel = { onCancel(message.id) },
                    onAttachmentClick = onAttachmentClick,
                    onRetry = onRetry
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatBubble(
        message: ChatMessage,
        isOwn: Boolean,
        profile: MemberProfile?,
        onCancel: () -> Unit,
        onAttachmentClick: (Attachment) -> Unit,
        onRetry: (String) -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bubbleColor =
            when {
                isOwn && isDarkTheme -> SentBubbleDark
                isOwn -> SentBubbleLight
                isDarkTheme -> BubbleGrayDark
                else -> BubbleGray // 使用BubbleGray颜色而不是MaterialTheme.colorScheme.surface
            }
    val bubbleContentColor =
            when {
                isOwn && isDarkTheme -> Color.White
                isOwn -> Color.Black
                else -> MaterialTheme.colorScheme.onSurface
            }
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
    ) {
        Column(
                horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.8f) // 限制气泡最大宽度为屏幕宽度的80%
        ) {
            if (!isOwn && profile != null) {
                Text(
                        text = profile.localNickname ?: profile.memberId.take(6),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                        color = bubbleColor,
                        contentColor = bubbleContentColor,
                        shape = MaterialTheme.shapes.large
                ) {
                    Column(
                            modifier =
                                    Modifier.clip(MaterialTheme.shapes.large)
                                            .combinedClickable(
                                                    onClick = { showActions = false },
                                                    onLongClick = { showActions = true }
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                            .widthIn(max = 260.dp)
                    ) {
                        if (message.attachment?.type == AttachmentType.PHOTO) {
                            PhotoAttachmentView(
                                    attachment = message.attachment,
                                    onClick = { onAttachmentClick(message.attachment) }
                            )
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        if (message.content.isNotBlank()) {
                            Text(text = message.content, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = formatMessageTimestamp(message.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (isOwn) {
                    StatusIcon(status = message.status, onRetry = { onRetry(message.id) })
                }
            }
            // 长按显示操作菜单
            // AnimatedVisibility：处理显示/隐藏过渡动画
            if (showActions) {
                AnimatedVisibility(visible = showActions) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                                onClick = {
                                    onCancel()
                                    showActions = false
                                }
                        ) { Text("Cancel send") }
                        TextButton(onClick = { showActions = false }) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(
        status: MessageStatus,
        onRetry: (() -> Unit)? = null
) {
    when (status) {
        MessageStatus.QUEUED -> {
            Icon(
                    imageVector = Icons.Rounded.Pending,
                    contentDescription = "Queued",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
            )
        }
        MessageStatus.SENDING ->
                CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                )
        MessageStatus.SENT ->
                Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Sent",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                )
        MessageStatus.DELIVERED ->
                Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = "Delivered",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                )
        // 发送失败状态：显示红色感叹号图标，点击触发重新发送确认对话框
        MessageStatus.FAILED -> {
            var showRetryDialog by remember { mutableStateOf(false) }

            Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp).clickable { showRetryDialog = true }
            )

            if (showRetryDialog && onRetry != null) {
                AlertDialog(
                        onDismissRequest = { showRetryDialog = false },
                        title = { Text("消息发送失败") },
                        text = { Text("是否要重新发送这条消息？") },
                        confirmButton = {
                            TextButton(
                                    onClick = {
                                        onRetry()
                                        showRetryDialog = false
                                    }
                            ) { Text("重试") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRetryDialog = false }) { Text("取消") }
                        }
                )
            }
        }
        MessageStatus.CANCELLED ->
                Icon(
                        imageVector = Icons.Default.Close, // 使用Close图标
                        contentDescription = "Cancelled",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                )
    }
}
