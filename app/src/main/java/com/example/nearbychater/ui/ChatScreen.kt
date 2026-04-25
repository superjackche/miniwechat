package com.example.nearbychater.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nearbychater.core.model.Attachment
import com.example.nearbychater.core.model.ConversationId
import com.example.nearbychater.ui.state.ChatViewModel

// @Composable：标记UI构建函数 (类比Python装饰器)
// 声明式UI入口
@Composable
internal fun ChatScreen(
        modifier: Modifier = Modifier,
        conversationId: ConversationId?,
        onBack: () -> Unit,
        onOpenSettings: () -> Unit,
        onOpenLogs: () -> Unit,
        viewModel: ChatViewModel = viewModel()
) {
    // collectAsStateWithLifecycle：生命周期感知的状态收集
    // ViewModel负责持有数据，屏幕旋转不丢失 (类比MVC Controller)
    val members by viewModel.members.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnosticsBubble.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val aliases by viewModel.conversationAliases.collectAsStateWithLifecycle()
    val snapshots by viewModel.conversationSnapshots.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var composerText by rememberSaveable(activeConversationId) { mutableStateOf("") }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var previewAttachment by remember { mutableStateOf<Attachment?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var shouldScrollToLatest by remember { mutableStateOf(false) }
    val photoPickerLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let { viewModel.sendPhoto(it) }
            }

    // LaunchedEffect：启动协程副作用
    // 监听conversationId变化，触发会话选中逻辑
    LaunchedEffect(conversationId) { conversationId?.let { viewModel.selectConversation(it) } }

    val currentConversationId = activeConversationId
    val activeSnapshot = currentConversationId?.let { snapshots[it] }
    val remoteMemberIds =
            activeSnapshot?.memberIds?.filterNot { it == viewModel.selfMemberId } ?: emptyList()
    val activeMembers =
            remember(remoteMemberIds, members) {
                remoteMemberIds.mapNotNull { id -> members.firstOrNull { it.memberId == id } }
            }
    val connectedCount = remember(activeMembers) { activeMembers.count { it.isOnline } }
    val aliasTitle = aliases[currentConversationId]?.takeIf { it.isNotBlank() }
    val title = aliasTitle ?: defaultConversationTitle(activeMembers, remoteMemberIds)
    val subtitle =
            remember(remoteMemberIds, connectedCount, messages) {
                when {
                    remoteMemberIds.isEmpty() -> "本机收藏夹"
                    connectedCount == remoteMemberIds.size && connectedCount > 0 ->
                            "已连接 · ${connectedCount}人"
                    connectedCount in 1 until remoteMemberIds.size ->
                            "部分在线 · $connectedCount/${remoteMemberIds.size} 人"
                    messages.any { it.senderId != viewModel.selfMemberId } -> {
                        val recent = messages.lastOrNull { it.senderId != viewModel.selfMemberId }
                        recent?.let { "最近活跃 · ${formatMessageTimestamp(it.timestamp)}" } ?: "最近活跃"
                    }
                    else -> "等待连接"
                }
            }

    // 使用Box作为根布局，确保诊断气泡显示在最顶层
    Box(modifier = modifier.fillMaxSize().imePadding()) {
        // 使用Scaffold布局处理输入法适配和顶部导航栏
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // 将ChatTopBar作为Scaffold的topBar，自动处理状态栏内边距
                ChatTopBar(
                    title = title,
                    subtitle = subtitle,
                    onBack = onBack,
                    menuExpanded = overflowMenuExpanded,
                    onToggleMenu = { overflowMenuExpanded = !overflowMenuExpanded },
                    onDismissMenu = { overflowMenuExpanded = false },
                    canRename = currentConversationId != null,
                    onRenameConversation = {
                        overflowMenuExpanded = false
                        if (currentConversationId != null) {
                            showRenameDialog = true
                        }
                    },
                    onOpenSettings = {
                        overflowMenuExpanded = false
                        onOpenSettings()
                    },
                    onOpenLogs = {
                        overflowMenuExpanded = false
                        onOpenLogs()
                    },
                    isGroupChat = remoteMemberIds.size > 1,
                    onViewMembers = {
                        overflowMenuExpanded = false
                        showMembersDialog = true
                    }
                )
            },
            bottomBar = {
                // 输入框与键盘无缝贴合，添加平滑过渡动画
                Box {
                    MessageComposerBar(
                        modifier = Modifier,
                        text = composerText,
                        onTextChange = { composerText = it },
                        onSend = {
                            if (composerText.isNotBlank()) {
                                viewModel.sendChatMessage(composerText.trim())
                                composerText = ""
                            }
                        },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        isSending = viewModel.isSending.collectAsStateWithLifecycle().value,
                        onInputFieldClick = {
                            // 点击输入框时设置滚动标志
                            shouldScrollToLatest = true
                        }
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0) // 禁用Scaffold默认的窗口内边距，手动控制
        ) { innerPadding ->
            // 主内容区域 - 优化布局确保状态栏空间预留
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(innerPadding) // 使用Scaffold的内边距，确保内容不被底部栏遮挡
                ) {
                    // 顶部导航栏已作为Scaffold的topBar，这里不再需要额外的顶部空间
                    HorizontalDivider(color = Color(0x1F000000))
                    // 使用weight(1f)确保MessageList占据剩余空间，并在输入法弹出时自适应调整
                    MessageList(
                        modifier = Modifier.weight(1f),
                        messages = messages,
                        members = members,
                        selfId = viewModel.selfMemberId,
                        onCancel = { viewModel.cancelMessage(it) },
                        onAttachmentClick = { previewAttachment = it },
                        onRetry = { viewModel.retryMessage(it) },
                        shouldScrollToLatest = shouldScrollToLatest,
                        onScrollComplete = { shouldScrollToLatest = false }
                    )
                    HorizontalDivider(color = Color(0x1F000000))
                }

                // 图片预览对话框
                previewAttachment?.let { attachment ->
                    PhotoPreviewDialog(
                        attachment = attachment,
                        onDismiss = { previewAttachment = null },
                        onSave = {
                            val success = saveAttachmentToGallery(context, attachment)
                            Toast.makeText(
                                    context,
                                    if (success) "已保存到相册" else "保存失败",
                                    Toast.LENGTH_SHORT
                            )
                                    .show()
                            if (success) {
                                previewAttachment = null
                            }
                        }
                    )
                }

                // 重命名对话框
        if (showRenameDialog && currentConversationId != null) {
            RenameConversationDialog(
                initialValue = aliasTitle.orEmpty(),
                onConfirm = { name ->
                    if (name.isBlank()) {
                        viewModel.clearConversationAlias(currentConversationId)
                    } else {
                        viewModel.setConversationAlias(currentConversationId, name)
                    }
                    showRenameDialog = false
                },
                onReset = {
                    viewModel.clearConversationAlias(currentConversationId)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }
        // 群成员列表对话框
        if (showMembersDialog && currentConversationId != null) {
            GroupMembersDialog(
                members = activeMembers,
                onDismiss = { showMembersDialog = false }
            )
        }
            }
        }

        // 诊断气泡 - 移动到Scaffold外部，确保显示在最顶层，并避开顶部导航栏
        DiagnosticsBubble(
            state = diagnostics,
            onDismiss = { viewModel.dismissDiagnosticsBubble() },
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    )
                    .padding(top = 72.dp) // 增加顶部内边距，避开顶部导航栏（约56dp高度 + 16dp间距）
                    .zIndex(Float.MAX_VALUE) // 确保诊断气泡显示在最上层
        )
    }
}
