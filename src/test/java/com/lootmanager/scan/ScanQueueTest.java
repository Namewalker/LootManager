package com.lootmanager.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanQueueTest {

    @Test
    void shouldDeduplicateCoordinatesAndDrainInOrder() {
        ScanQueue queue = new ScanQueue();
        assertTrue(queue.enqueueChunk("Amberlight", -2, 4));
        assertFalse(queue.enqueueChunk("Amberlight", -2, 4));
        assertTrue(queue.enqueueChunk("DungeonWorld", -2, 4));
        assertEquals(2, queue.getChunkQueueSize());
        assertEquals("Amberlight", queue.pollChunk().world);
        assertEquals("DungeonWorld", queue.pollChunk().world);
        assertNull(queue.pollChunk());

        assertTrue(queue.enqueueContainer("Amberlight", -17, 64, 32));
        assertFalse(queue.enqueueContainer("Amberlight", -17, 64, 32));
        assertEquals(1, queue.drainContainers(10).size());
    }
}
