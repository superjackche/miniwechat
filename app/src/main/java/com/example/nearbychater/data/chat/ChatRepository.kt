package com.example.nearbychater.data.chat

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.nearbychater.core.logging.LogManager
import com.example.nearbychater.core.model.Attachment
import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.ConversationId
import com.example.nearbychater.core.model.ConversationSnapshot
import com.example.nearbychater.core.model.ConversationSummary
import com.example.nearbychater.core.model.DiagnosticsEvent
import com.example.nearbychater.core.model.MemberId
import com.example.nearbychater.core.model.MemberProfile
import com.example.nearbychater.core.model.MeshEnvelope
import com.example.nearbychater.core.model.MessageStatus
import com.example.nearbychater.core.model.MessageType
import com.example.nearbychater.data.nearby.EndpointInfo
import com.example.nearbychater.data.nearby.NearbyChatService
import com.example.nearbychater.data.nearby.NearbyEvent
import com.example.nearbychater.data.storage.ChatDao
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Coordinates cached conversations, Nearby mesh events, and offline queue flushing. */
class ChatRepository(
        private val context: Context,
        private val nearbyChatService: NearbyChatService,
        private val logManager: LogManager,
        private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        private val chatDao: ChatDao = ChatDao(context),
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // 本机设备ID，根据ANDROID_ID生成，唯一标识这台设备
    private val localMemberId: MemberId = deviceId(context)
    private val conversationIdentity = ConversationIdentity(localMemberId)
    private val summaryBuilder = ConversationSummaryBuilder(localMemberId, conversationIdentity)
    private val outboundQueue = OutboundMessageQueue()
    private val dedupePolicy = PacketDedupePolicy()
    private val notificationPresenter = NotificationPresenter(context)

    // _conversations: 所有会话的内存缓存
    // Map<会话ID, 会话快照>，会话快照包含消息列表和成员列表
    // MutableStateFlow是热流，有新数据时自动通知订阅者（UI层）
    private val _conversations =
            MutableStateFlow<Map<ConversationId, ConversationSnapshot>>(emptyMap())

    // _members: 所有成员的内存缓存
    // 存储每个成员的昵称、在线状态、最后上线时间等信息
    private val _members = MutableStateFlow<Map<MemberId, MemberProfile>>(emptyMap())

    // _diagnostics: 诊断事件流
    // SharedFlow类似广播，可以有多个订阅者
    // extraBufferCapacity=32表示最多缓存32个未消费的事件
    private val _diagnostics = MutableSharedFlow<DiagnosticsEvent>(extraBufferCapacity = 32)

    // flushJob: 消息刷新循环的Job
    // Job用于管理协程的生命周期，可以取消
    private val flushJob: Job

    // 公开的诊断事件流，UI层可以订阅
    // asSharedFlow()转换成只读的SharedFlow
    val diagnostics = _diagnostics.asSharedFlow()

    // 公开的会话数据，UI层订阅这个Flow就能实时获取会话更新
    val conversations: StateFlow<Map<ConversationId, ConversationSnapshot>> = _conversations

    // 公开的成员列表
    // map{}把Map转换成List，并按memberId排序
    // stateIn把Flow转换成StateFlow（热流），参数：
    // - externalScope: 协程作用域
    // - SharingStarted.Eagerly: 立即开始收集
    // - emptyList(): 初始值
    val members: StateFlow<List<MemberProfile>> =
            _members
                    .map { it.values.sortedBy { profile -> profile.memberId } }
                    .stateIn(externalScope, SharingStarted.Eagerly, emptyList())

    // conversationSummaries: 会话摘要列表（用于会话列表UI）
    // combine()合并多个Flow，当任一Flow更新时都会重新计算
    // buildSummaries()根据会话和成员信息生成摘要
    // 摘要包含：标题、预览、未读数、头像种子等
    val conversationSummaries: StateFlow<List<ConversationSummary>> =
            combine(_conversations, _members) { convMap, memberMap ->
                        summaryBuilder.build(convMap, memberMap)
                    }
                    .stateIn(externalScope, SharingStarted.Eagerly, emptyList())

    // selfMemberId: 本机设备ID的公开访问器
    // get()表示这是计算属性，每次访问都返回localMemberId
    val selfMemberId: MemberId
        get() = localMemberId

    // init块在对象创建时执行，初始化Repository
    init {
        // 启动协程加载数据库中的状态
        externalScope.launch {
            reloadState() // 从数据库加载会话和成员
            outboundQueue.triggerFlush() // 触发消息队列刷新
        }
        // 开始监听Nearby服务的事件（成员上线/下线、消息接收等）
        observeNearbyEvents()
        // 启动Nearby服务，开始广播自己并发现附近设备
        // EndpointInfo包含自己的设备ID和昵称（设备型号）
        nearbyChatService.start(
                EndpointInfo(memberId = localMemberId, nickname = BuildNickname.local())
        )
        // 启动消息刷新循环
        // 这个协程会一直运行，等待flushTrigger的信号
        flushJob = externalScope.launch { outboundQueue.runFlushLoop { flushQueuedMessages() } }
    }

    // 获取指定会话的消息列表
    // 返回StateFlow，UI层订阅后会自动更新
    // filter过滤掉ACK消息（确认消息不需要显示在UI上）
    fun conversationMessages(conversationId: ConversationId): StateFlow<List<ChatMessage>> =
            _conversations
                    .map { conversations ->
                        conversations[conversationId]?.messages?.filter {
                            it.type != MessageType.ACK
                        }
                                ?: emptyList()
                    }
                    .stateIn(externalScope, SharingStarted.Eagerly, emptyList())

    // 发送消息的核心函数
    // suspend表示这是挂起函数，可能会暂停协程但不阻塞线程
    suspend fun sendMessage(
            conversationId: ConversationId,
            content: String,
            attachment: Attachment? = null // 可选的附件（图片等）
    ) {
        // 第1步：创建消息对象
        // 初始状态是QUEUED（排队中）
        val message = 
                ChatMessage(
                        conversationId = conversationId,
                        senderId = localMemberId,
                        content = content,
                        status = MessageStatus.QUEUED,
                        attachment = attachment
                )
        // 第2步：加入待发送队列
        // 即使现在网络不通，消息也会保存，等网络恢复后自动发送
        outboundQueue.enqueue(message)

        // 第3步：准备会话成员信息
        val memberIds = knownMemberIds(conversationId)

        // 第4步：持久化到数据库
        // onDb{}在IO线程执行数据库操作
        onDb {
            chatDao.ensureConversation(conversationId, resolveConversationKey(memberIds), memberIds)
            chatDao.insertOrUpdateMessage(message)
        }

        // 第5步：更新内存缓存，UI立即显示（状态改为SENDING）
        upsertMessageLocally(message.copy(status = MessageStatus.SENDING))

        // 第6步：立即尝试发送消息，而不是等待flushLoop
        // 优化消息发送延迟，实现快速发送
        attemptSend(message)
    }

    public suspend fun updateLocalNickname(memberId: MemberId, nickname: String) {
        val current = _members.value[memberId]
        val updated = (current ?: MemberProfile(memberId = memberId)).copy(localNickname = nickname)
        onDb { chatDao.upsertMember(updated) }
        upsertMemberLocally(updated)
    }

    private fun conversationIdFromMembers(memberIds: Set<MemberId>): ConversationId {
        return conversationIdentity.conversationIdFromMembers(memberIds)
    }

    fun conversationIdFor(remoteMemberId: MemberId): ConversationId =
            conversationIdFromMembers(setOf(remoteMemberId))

    fun conversationIdForMembers(remoteMemberIds: Set<MemberId>): ConversationId =
            conversationIdFromMembers(remoteMemberIds)

    fun ensureConversationMembers(remoteMemberIds: Set<MemberId>): ConversationId {
        val filtered = remoteMemberIds.filterNot { it == localMemberId }.toSet()
        val conversationId = conversationIdFromMembers(filtered)
        val memberSet = filtered + localMemberId
        externalScope.launch {
            onDb {
                chatDao.ensureConversation(
                        conversationId,
                        resolveConversationKey(memberSet),
                        memberSet
                )
            }
            ensureConversationLocally(conversationId, memberSet)
        }
        return conversationId
    }

    suspend fun refresh() {
        nearbyChatService.refreshDiscovery()
        // 不调用reloadState()，避免频繁覆盖内存中的消息
        // 消息通过observeNearbyEvents()的实时流处理
        outboundQueue.triggerFlush()
    }

    fun isMemberConnected(memberId: MemberId): Boolean =
            nearbyChatService.isMemberConnected(memberId)

    public suspend fun cancelMessage(conversationId: ConversationId, messageId: String) {
        outboundQueue.remove(messageId)
        onDb {
            chatDao.updateMessageStatus(
                    conversationId,
                    messageId,
                    MessageStatus.CANCELLED,
                    shouldRelay = false
            )
        }
        updateMessageLocally(conversationId, messageId) { current ->
            current.copy(status = MessageStatus.CANCELLED, shouldRelay = false)
        }
    }

    public suspend fun retryMessage(conversationId: ConversationId, messageId: String) {
        val snapshot = _conversations.value[conversationId] ?: return
        val message = snapshot.messages.find { it.id == messageId } ?: return
        val queuedMessage = message.copy(status = MessageStatus.QUEUED)
        outboundQueue.enqueue(queuedMessage)
        onDb { chatDao.updateMessageStatus(conversationId, messageId, MessageStatus.QUEUED) }
        updateMessageLocally(conversationId, messageId) { it.copy(status = MessageStatus.QUEUED) }
        outboundQueue.triggerFlush()
    }

    public suspend fun deleteConversation(conversationId: ConversationId) {
        onDb { chatDao.deleteConversation(conversationId) }
        _conversations.update { it - conversationId }
    }

    private suspend fun attemptSend(message: ChatMessage) {
        when (val target = resolveConversationTarget(message.conversationId)) {
            ConversationTarget.Unknown -> return
            ConversationTarget.Self -> {
                onDb {
                    chatDao.updateMessageStatus(
                            message.conversationId,
                            message.id,
                            MessageStatus.SENT,
                            shouldRelay = false
                    )
                }
                updateMessageLocally(message.conversationId, message.id) { current ->
                    current.copy(status = MessageStatus.SENT, shouldRelay = false)
                }
                outboundQueue.remove(message.id) // 确保移除消息
            }
            is ConversationTarget.Remote -> {
                val envelope =
                        MeshEnvelope(
                                conversationId = message.conversationId,
                                message = message.copy(status = MessageStatus.SENT),
                                originId = localMemberId,
                                hopCount = 0
                        )
                // 实际调用 nearbyChatService 发送消息
                val success =
                        nearbyChatService.broadcast(
                                conversationId = message.conversationId,
                                message = envelope,
                                targetMembers = target.memberIds
                        )
                val nextStatus = if (success) MessageStatus.SENT else MessageStatus.FAILED
                onDb { chatDao.updateMessageStatus(message.conversationId, message.id, nextStatus) }
                updateMessageLocally(message.conversationId, message.id) { current ->
                    current.copy(status = nextStatus)
                }
                outboundQueue.remove(message.id) // 无论成功与否都移除
            }
        }
    }

    private suspend fun reloadState() {
        val (membersFromDb, conversationsFromDb) = onDb { chatDao.readMembers() to chatDao.readConversations() }
        // 合并数据库状态和内存状态，避免丢失未保存的消息
        _members.value = (_members.value + membersFromDb)
        _conversations.update { current ->
            conversationsFromDb.mapValues { (convId, dbSnapshot) ->
                val currentSnapshot = current[convId]
                if (currentSnapshot != null) {
                    // 合并消息：保留内存中的消息（可能未持久化），同时加入数据库中的消息
                    val mergedMessages = (currentSnapshot.messages + dbSnapshot.messages)
                        .distinctBy { it.id }
                        .sortedBy { it.timestamp }
                    dbSnapshot.copy(messages = mergedMessages)
                } else {
                    dbSnapshot
                }
            }.let { dbConversations ->
                // 保留内存中的其他会话
                current + dbConversations
            }
        }
    }

    // observeNearbyEvents: 监听Nearby服务的事件
    // 相当于设置事件监听器，当有新事件时自动响应
    private fun observeNearbyEvents() {
        externalScope.launch {
            // nearbyChatService.events()返回一个Flow
            // collect{}持续监听这个Flow，类似while(true){等待事件}
            nearbyChatService.events().collect { event ->
                // when是Kotlin的switch，is类似 instanceof
                when (event) {
                    // 成员上线: 更新成员状态，创建会话，尝试发送离线消息
                    is NearbyEvent.MemberOnline ->
                            handleMemberOnline(event.memberId, event.nickname)
                    // 成员下线: 标记为离线状态
                    is NearbyEvent.MemberOffline -> handleMemberOffline(event.memberId)
                    // 收到消息: 处理来自其他设备的消息
                    is NearbyEvent.MessageReceived -> handleRemoteMessage(event.envelope)
                    // 错误事件: 记录诊断信息
                    is NearbyEvent.Error -> trackDiagnostics(event.diagnosticsEvent)
                }
            }
        }
    }

    private suspend fun handleMemberOnline(memberId: MemberId, nickname: String?) {
        val now = System.currentTimeMillis()
        val profile = _members.value[memberId]
        val updated =
                (profile ?: MemberProfile(memberId = memberId)).copy(
                        remoteNickname = nickname ?: profile?.remoteNickname,
                        isOnline = true,
                        lastSeenAt = now
                )
        val conversationId = conversationIdFor(memberId)
        val memberIds = setOf(localMemberId, memberId)
        onDb {
            chatDao.updateMemberOnlineState(memberId, nickname, true, now)
            chatDao.ensureConversation(conversationId, resolveConversationKey(memberIds), memberIds)
        }
        upsertMemberLocally(updated)
        ensureConversationLocally(conversationId, memberIds)
        outboundQueue.triggerFlush()
    }

    private suspend fun handleMemberOffline(memberId: MemberId) {
        val now = System.currentTimeMillis()
        val profile = _members.value[memberId]
        val updated = (profile ?: MemberProfile(memberId = memberId)).copy(
            isOnline = false,
            lastSeenAt = now
        )
        // Persist the transition even when the in-memory profile was not loaded yet.
        onDb { chatDao.updateMemberOnlineState(memberId, null, false, now) }
        upsertMemberLocally(updated)
    }

    // handleRemoteMessage: 处理收到的远程消息
    // 这是P2P网络的关键函数，处理各种消息类型
    private suspend fun handleRemoteMessage(envelope: MeshEnvelope) {
        // 第1步：检查是否已处理过该消息
        // P2P网络中，同一消息可能由多个节点转发，需要去重
        if (!dedupePolicy.shouldProcess(envelope.packetId)) {
            return // 已处理过，直接忽略
        }

        val message = envelope.message

        // 情凵1：如果是ACK消息（确认回复）
        if (message.type == MessageType.ACK) {
            // ACK消息的content字段存储的是原消息ID
            val targetMessageId = message.content
            // 把原消息状态改为DELIVERED（已送达）
            onDb {
                chatDao.updateMessageStatus(
                        message.conversationId,
                        targetMessageId,
                        MessageStatus.DELIVERED
                )
            }
            updateMessageLocally(message.conversationId, targetMessageId) { current ->
                current.copy(status = MessageStatus.DELIVERED)
            }
            return
        }

        // 情凵2：如果是自己发的消息（回环收到）
        // 这种情况在网状网络中很常见，消息会经过其他节点转发回来
        if (message.senderId == localMemberId) {
            // 标记为SENT，证明消息已成功在网络中传播
            onDb {
                chatDao.updateMessageStatus(message.conversationId, message.id, MessageStatus.SENT)
            }
            updateMessageLocally(message.conversationId, message.id) { current ->
                current.copy(status = MessageStatus.SENT)
            }
            return
        }

        // 第3步：来自其他设备的正常消息
        val delivered = message.copy(status = MessageStatus.SENT)
        // 获取消息参与者列表，如果envelope中没有，就使用已知的成员列表
        val remoteParticipants =
                envelope.participants.takeIf { it.isNotEmpty() }
                        ?: (knownMemberIds(message.conversationId) - localMemberId)
        val memberIds = (remoteParticipants + message.senderId).toSet()
        val fullMemberSet = memberIds + localMemberId

        // 立即更新内存，不等待数据库操作
        ensureConversationLocally(message.conversationId, memberIds)
        upsertMessageLocally(delivered)

        // 异步保存到数据库（不阻塞消息处理）
        onDb {
            chatDao.ensureConversation(
                    message.conversationId,
                    resolveConversationKey(fullMemberSet),
                    fullMemberSet
            )
            chatDao.insertOrUpdateMessage(delivered)
        }

        // 发送ACK确认
        // 告诉发送者：我收到了
        val ack =
                ChatMessage(
                        conversationId = message.conversationId,
                        senderId = localMemberId,
                        content = message.id, // ACK的content是原消息ID
                        type = MessageType.ACK,
                        status = MessageStatus.SENT
                )
        attemptSend(ack)

        // 后台时显示通知
        // 如果应用不在前台，就发送系统通知
        notificationPresenter.showIfBackground(message, _members.value)
    }

    private suspend fun trackDiagnostics(event: DiagnosticsEvent) {
        logManager.log(event)
        _diagnostics.emit(event)
    }

    private suspend fun flushQueuedMessages() {
        outboundQueue.queuedMessages(_conversations.value.values).forEach { pending ->
            outboundQueue.enqueue(pending)
            attemptSend(pending)
        }
    }

    private fun upsertMessageLocally(message: ChatMessage) {
        _conversations.update { conversations ->
            val snapshot =
                    conversations[message.conversationId]
                            ?: ConversationSnapshot(conversationId = message.conversationId)
            val updatedMessages = snapshot.messages.filterNot { it.id == message.id } + message
            val updatedMemberIds = snapshot.memberIds + message.senderId + localMemberId
            val updatedSnapshot =
                    snapshot.copy(
                            messages = updatedMessages.sortedBy { it.timestamp },
                            memberIds = updatedMemberIds,
                            conversationKey = conversationIdentity.conversationKey(updatedMemberIds)
                    )
            conversations + (message.conversationId to updatedSnapshot)
        }
    }

    private fun updateMessageLocally(
            conversationId: ConversationId,
            messageId: String,
            transform: (ChatMessage) -> ChatMessage
    ) {
        _conversations.update { conversations ->
            val snapshot = conversations[conversationId] ?: return@update conversations
            val updatedMessages =
                    snapshot.messages.map { if (it.id == messageId) transform(it) else it }
            conversations + (conversationId to snapshot.copy(messages = updatedMessages))
        }
    }

    private fun ensureConversationLocally(
            conversationId: ConversationId,
            memberIds: Set<MemberId>
    ) {
        _conversations.update { conversations ->
            val existing = conversations[conversationId]
            val mergedMembers = (existing?.memberIds ?: emptySet()) + memberIds + localMemberId
            val snapshot =
                    (existing ?: ConversationSnapshot(conversationId = conversationId)).copy(
                            memberIds = mergedMembers,
                            conversationKey = conversationIdentity.conversationKey(mergedMembers)
                    )
            conversations + (conversationId to snapshot)
        }
    }

    private fun upsertMemberLocally(profile: MemberProfile) {
        _members.update { it + (profile.memberId to profile) }
    }

    private fun knownMemberIds(conversationId: ConversationId): Set<MemberId> {
        return (_conversations.value[conversationId]?.memberIds ?: emptySet()) + localMemberId
    }

    private suspend fun <T> onDb(block: () -> T): T = withContext(ioDispatcher) { block() }

    private fun deviceId(context: Context): MemberId {
        val androidId =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId ?: UUID.randomUUID().toString()
    }

    private fun resolveConversationKey(memberIds: Set<MemberId>): String =
            conversationIdentity.conversationKey(memberIds)

    private fun resolveConversationTarget(conversationId: ConversationId): ConversationTarget {
        return conversationIdentity.targetFor(_conversations.value[conversationId])
    }

    private object BuildNickname {
        fun local(): String = Build.MODEL ?: "Android"
    }

    suspend fun setConversationPinned(conversationId: ConversationId, pinned: Boolean) {
        onDb { chatDao.setConversationPinned(conversationId, pinned) }
        _conversations.update { conversations ->
            val snapshot = conversations[conversationId] ?: return@update conversations
            conversations + (conversationId to snapshot.copy(isPinned = pinned))
        }
    }
}
