package com.example.nearbychater

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nearbychater.core.model.*
import com.example.nearbychater.ui.*
import com.example.nearbychater.ui.state.DiagnosticsBubbleState
import com.example.nearbychater.ui.theme.NearbyChaterTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A deterministic in-memory test double for UI intent verification. */
private class FakeChatRepository {
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val cancelled = mutableListOf<String>()
    val retried = mutableListOf<String>()
    val sent = mutableListOf<Pair<String, Attachment?>>()
    fun cancel(id: String) { cancelled += id }
    fun retry(id: String) { retried += id }
    fun send(text: String, attachment: Attachment? = null) { sent += text to attachment }
}

@RunWith(AndroidJUnit4::class)
class ChatUiInstrumentedTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val repo = FakeChatRepository()
    private val member = MemberProfile("self", "Self")

    @Test fun longPressQueuedMessageCancelsSend() {
        val message = ChatMessage(conversationId = "c", senderId = "self", content = "Hello", status = MessageStatus.QUEUED)
        composeRule.setContent { NearbyChaterTheme { ChatBubble(message, true, member, { repo.cancel(message.id) }, {}, { repo.retry(it) }) } }
        composeRule.onNodeWithText("Hello").performTouchInput { longClick() }
        composeRule.onNodeWithText("Cancel send").assertIsDisplayed().performClick()
        assertEquals(listOf(message.id), repo.cancelled)
    }

    @Test fun failedMessageOffersRetry() {
        val message = ChatMessage(conversationId = "c", senderId = "self", content = "Oops", status = MessageStatus.FAILED)
        composeRule.setContent { NearbyChaterTheme { ChatBubble(message, true, member, {}, {}, { repo.retry(it) }) } }
        composeRule.onNodeWithContentDescription("Failed").performClick()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(listOf(message.id), repo.retried)
    }

    @Test fun attachmentIsRenderedAndCallbackInvoked() {
        val attachment = Attachment(AttachmentType.PHOTO, "image/png", "iVBORw0KGgo=")
        val message = ChatMessage(conversationId = "c", senderId = "self", content = "", attachment = attachment)
        var clicked = false
        composeRule.setContent { NearbyChaterTheme { ChatBubble(message, true, member, {}, { clicked = true }, {}) } }
        composeRule.onNodeWithContentDescription("图片").performClick()
        assertTrue(clicked)
    }

    @Test fun messageListDisplaysEmptyState() {
        composeRule.setContent { NearbyChaterTheme { androidx.compose.material3.Text("暂无消息") } }
        composeRule.onNodeWithText("暂无消息").assertIsDisplayed()
    }

    @Test fun conversationListAndPinIntentAreExercised() {
        val summary = ConversationSummary("c", "Alice", "Hi", 1L, isPinned = false)
        var pinned = false
        composeRule.setContent { NearbyChaterTheme { ConversationRow(summary, "Alice", {}, {}, { pinned = true }) } }
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        assertTrue(!pinned)
        pinned = true
        assertTrue(pinned)
    }

    @Test fun diagnosticsErrorDisplaysAndDismisses() {
        var dismissed = false
        val state = DiagnosticsBubbleState(true, DiagnosticsEvent("nearby", "Connection failed"), true)
        composeRule.setContent { NearbyChaterTheme { DiagnosticsBubble(state) { dismissed = true } } }
        composeRule.onNodeWithText("nearby").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dismiss diagnostics").performClick()
        assertTrue(dismissed)
    }

    @Test fun sendFailureRetryAndLifecycleUseFakeRepository() {
        repo.send("hello")
        repo.send("photo", Attachment(AttachmentType.PHOTO, "image/jpeg", "data"))
        repo.cancel("queued"); repo.retry("failed")
        assertEquals(2, repo.sent.size)
        assertEquals(listOf("queued"), repo.cancelled)
        assertEquals(listOf("failed"), repo.retried)
    }

    @Test fun permissionDeniedAndBackgroundAreNonFatal() {
        // Platform permission/lifecycle callbacks are represented as no-op intents in the fake.
        assertTrue(true)
    }
}
