package com.lootmanager.scan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class ScanQueue {

    private final Queue<String> chunkQueue = new ArrayDeque<String>();
    private final Queue<String> containerQueue = new ArrayDeque<String>();

    public void enqueueChunk(String worldName, int chunkX, int chunkZ) {
        chunkQueue.add(worldName + ":" + chunkX + ":" + chunkZ);
    }

    public void enqueueContainer(String worldName, int x, int y, int z) {
        containerQueue.add(worldName + ":" + x + ":" + y + ":" + z);
    }

    public int getChunkQueueSize() {
        return chunkQueue.size();
    }

    public int getContainerQueueSize() {
        return containerQueue.size();
    }

    public List<String> drainChunks(int limit) {
        List<String> drained = new ArrayList<String>();
        for (int i = 0; i < limit && !chunkQueue.isEmpty(); i++) {
            drained.add(chunkQueue.poll());
        }
        return drained;
    }

    public List<String> drainContainers(int limit) {
        List<String> drained = new ArrayList<String>();
        for (int i = 0; i < limit && !containerQueue.isEmpty(); i++) {
            drained.add(containerQueue.poll());
        }
        return drained;
    }

    public List<String> snapshotChunks() {
        return Collections.unmodifiableList(new ArrayList<String>(chunkQueue));
    }

    public List<String> snapshotContainers() {
        return Collections.unmodifiableList(new ArrayList<String>(containerQueue));
    }
}
