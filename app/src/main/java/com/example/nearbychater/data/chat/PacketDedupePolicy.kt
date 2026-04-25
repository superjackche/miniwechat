package com.example.nearbychater.data.chat

import java.util.Collections

internal class PacketDedupePolicy(private val maxEntries: Int = 1000) {
    private val seenPacketIds =
            Collections.synchronizedSet(
                    Collections.newSetFromMap(
                            object : LinkedHashMap<String, Boolean>(maxEntries, 0.75f, true) {
                                override fun removeEldestEntry(
                                        eldest: MutableMap.MutableEntry<String, Boolean>?
                                ): Boolean {
                                    return size > maxEntries
                                }
                            }
                    )
            )

    fun shouldProcess(packetId: String): Boolean {
        return seenPacketIds.add(packetId)
    }
}
