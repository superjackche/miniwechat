package com.example.nearbychater

import android.Manifest
import com.example.nearbychater.core.model.ChatMessage
import com.example.nearbychater.core.model.MeshEnvelope
import com.example.nearbychater.core.model.MessageStatus
import org.junit.Assert.*
import org.junit.Test

class PermissionAndModelTest {
    @Test fun requiredPermissionsAreUniqueAndCoverNearbyTransport() {
        val permissions = requiredPermissions().toSet()
        assertEquals(permissions.size, requiredPermissions().size)
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_SCAN) || permissions.contains(Manifest.permission.BLUETOOTH))
    }

    @Test fun retryableFailureRestoresQueuedDeliveryState() {
        val failed = ChatMessage("id", "conversation", "local", "body", status = MessageStatus.FAILED)
        val retried = failed.copy(status = MessageStatus.QUEUED)
        assertEquals(MessageStatus.QUEUED, retried.status)
        assertEquals(failed.id, retried.id)
        assertTrue(retried.shouldRelay)
    }

    @Test fun meshEnvelopeCanAdvanceHopWithoutMutatingMessage() {
        val message = ChatMessage("id", "c", "sender", "body")
        val envelope = MeshEnvelope("c", message, "origin", hopCount = 1, participants = setOf("origin"))
        val forwarded = envelope.copy(hopCount = envelope.hopCount + 1, participants = envelope.participants + "relay")
        assertEquals(2, forwarded.hopCount)
        assertEquals(message, forwarded.message)
        assertTrue(forwarded.participants.contains("relay"))
    }
}
