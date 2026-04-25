package com.example.nearbychater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nearbychater.ui.state.ChatViewModel

// ConversationListScreen: 会话列表界面
// 显示所有聊天会话的列表，类似微信的会话列表
// 关键功能:
// 1. 下拉刷新 (Pull-to-refresh)
// 2. 侧滑操作 (置顶/删除)
// 3. 点击进入聊天
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListScreen(
        modifier: Modifier = Modifier,
        viewModel: ChatViewModel,
        onConversationSelected: (String) -> Unit, // 选中会话的回调
        onOpenSettings: () -> Unit, // 打开设置的回调
        onOpenLogs: () -> Unit // 打开日志的回调
) {
    // 订阅会话摘要列表
    val summaries by viewModel.conversationSummaries.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()

    // 显示添加联系人对话框的状态
    var showAddContact by remember { mutableStateOf(false) }
    // 显示创建群聊对话框的状态
    var showCreateGroup by remember { mutableStateOf(false) }
    // 显示添加菜单的状态
    var showAddMenu by remember { mutableStateOf(false) }

    // filtered: 过滤后的会话列表
    // 这里直接使用所有会话，没有过滤
    val filtered = summaries

    // aliases: 会话别名 (用户自定义的名称)
    val aliases by viewModel.conversationAliases.collectAsStateWithLifecycle()

    // isRefreshing: 是否正在刷新
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // safeInsets: 安全区域内边距
    val safeInsets =
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    val safePadding = safeInsets.asPaddingValues()


    // Box: 最外层容器
    // 用于堆叠布局，把刷新指示器放在列表上方
    Box(
            modifier =
                    modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(safePadding).padding(horizontal = 12.dp)) {
            // 顶部栏: 显示应用名称和操作按钮
            TopBar(
                    onAddMenuToggle = { showAddMenu = !showAddMenu },
                    onAddMenuDismiss = { showAddMenu = false },
                    onAddContact = { showAddContact = true },
                    onCreateGroup = { showCreateGroup = true },
                    onLogs = onOpenLogs,
                    onSettings = onOpenSettings,
                    isAddMenuExpanded = showAddMenu
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Surface: 会话列表容器
            Surface(
                    modifier = Modifier.weight(1f), // weight(1f)占据剩余空间
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp, // 轻微高度效果
                    shape = MaterialTheme.shapes.small
            ) {
                // PullToRefreshBox: 下拉刷新容器
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshConversations() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 如果没有会话，显示提示文字
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "暂无会话", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // LazyColumn: 懒加载列表
                        // 只渲染可见的项，性能好
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // items()遍历会话列表
                            // key参数帮助Compose优化重组
                            // contentType参数帮助Compose复用布局，提高性能
                            items(filtered, key = { it.conversationId }, contentType = { "conversation" }) { summary ->
                                // onDelete: 删除回调
                                // 如果是自己的会话null（不能删除），否则提供删除函数
                                val onDelete = if (summary.isSelf) null else { { viewModel.deleteConversation(summary.conversationId) } }
                                // displayTitle: 显示名称
                                // 优先使用别名，其次使用默认标题
                                val displayTitle = aliases[summary.conversationId] ?: summary.title

                                // ConversationRow: 会话行组件
                                // 支持点击、删除、置顶
                                ConversationRow(
                                        summary = summary,
                                        displayTitle = displayTitle,
                                        onClick = {
                                            viewModel.selectConversation(summary.conversationId)
                                            onConversationSelected(summary.conversationId)
                                        },
                                        onDelete = onDelete,
                                        onTogglePinned = {
                                            viewModel.setConversationPinned(
                                                    summary.conversationId,
                                                    !summary.isPinned
                                            )
                                        }
                                )
                                // HorizontalDivider: 分割线
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
            // 如果显示添加联系人对话框
            if (showAddContact) {
                AddContactDialog(
                        selfId = viewModel.selfMemberId,
                        onDismiss = { showAddContact = false },
                        onConfirm = { memberIds ->
                            if (memberIds.isNotEmpty()) {
                                val conversationId = viewModel.ensureConversation(memberIds)
                                onConversationSelected(conversationId)
                            }
                            showAddContact = false
                        }
                )
            }
            // 如果显示创建群聊对话框
            if (showCreateGroup) {
                CreateGroupDialog(
                        members = members,
                        selfId = viewModel.selfMemberId,
                        onDismiss = { showCreateGroup = false },
                        onConfirm = { selectedMembers ->
                            if (selectedMembers.isNotEmpty()) {
                                val conversationId = viewModel.ensureConversation(selectedMembers)
                                onConversationSelected(conversationId)
                            }
                            showCreateGroup = false
                        }
                )
            }
        }
    }
}
