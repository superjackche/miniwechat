package com.example.nearbychater

import com.example.nearbychater.core.model.DiagnosticsEvent
import com.example.nearbychater.data.nearby.NearbyEvent
import org.junit.Assert.*
import org.junit.Test

class NearbyEventTest {
    @Test fun eventsPreserveMemberAndDiagnosticInformation() {
        val online = NearbyEvent.MemberOnline("peer", "Nearby")
        val offline = NearbyEvent.MemberOffline("peer")
        val error = NearbyEvent.Error(DiagnosticsEvent("decode", "invalid payload"))
        assertEquals("peer", online.memberId); assertEquals("Nearby", online.nickname)
        assertEquals("peer", offline.memberId)
        assertEquals("decode", error.diagnosticsEvent.code)
    }
}
