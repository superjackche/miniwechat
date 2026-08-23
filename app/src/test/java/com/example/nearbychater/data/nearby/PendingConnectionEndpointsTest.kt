package com.example.nearbychater.data.nearby

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingConnectionEndpointsTest {
    @Test
    fun duplicateDiscoveryIsAcceptedOnlyOnce() {
        val state = PendingConnectionEndpoints()

        assertTrue(state.tryAdd("endpoint"))
        assertFalse(state.tryAdd("endpoint"))
    }

    @Test
    fun failedRequestCanBeRetried() {
        val state = PendingConnectionEndpoints()

        assertTrue(state.tryAdd("endpoint"))
        state.remove("endpoint")
        assertTrue(state.tryAdd("endpoint"))
    }

    @Test
    fun concurrentDiscoveryHasSingleWinner() {
        val state = PendingConnectionEndpoints()
        val executor = Executors.newFixedThreadPool(8)
        val ready = CountDownLatch(1)
        val done = CountDownLatch(8)
        val results = mutableListOf<Boolean>()
        repeat(8) {
            executor.execute {
                ready.await()
                synchronized(results) { results += state.tryAdd("endpoint") }
                done.countDown()
            }
        }
        ready.countDown()
        done.await()
        executor.shutdown()

        assertTrue(results.count { it } == 1)
    }
}
