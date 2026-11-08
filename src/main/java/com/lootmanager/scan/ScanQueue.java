package com.lootmanager.scan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScanQueue {

    private final Deque<ChunkPosition> chunks = new ArrayDeque<ChunkPosition>();
    private final Deque<BlockPosition> containers = new ArrayDeque<BlockPosition>();
    private final Set<String> queuedChunks = new HashSet<String>();
    private final Set<String> queuedContainers = new HashSet<String>();

    public boolean enqueueChunk(String worldName, int chunkX, int chunkZ) {
        String key = worldName + ":" + chunkX + ":" + chunkZ;
        if (!queuedChunks.add(key)) {
            return false;
        }
        chunks.add(new ChunkPosition(worldName, chunkX, chunkZ));
        return true;
    }

    public boolean enqueueContainer(String worldName, int x, int y, int z) {
        String key = worldName + ":" + x + ":" + y + ":" + z;
        if (!queuedContainers.add(key)) {
            return false;
        }
        containers.add(new BlockPosition(worldName, x, y, z));
        return true;
    }

    public int getChunkQueueSize() {
        return chunks.size();
    }

    public int getContainerQueueSize() {
        return containers.size();
    }

    public boolean hasContainerInChunk(String worldName, int chunkX, int chunkZ) {
        for (BlockPosition position : containers) {
            if (worldName.equals(position.world) && (position.x >> 4) == chunkX && (position.z >> 4) == chunkZ) {
                return true;
            }
        }
        return false;
    }

    public ChunkPosition pollChunk() {
        ChunkPosition position = chunks.poll();
        if (position != null) {
            queuedChunks.remove(position.key());
        }
        return position;
    }

    public BlockPosition pollContainer() {
        BlockPosition position = containers.poll();
        if (position != null) {
            queuedContainers.remove(position.key());
        }
        return position;
    }

    public List<ChunkPosition> drainChunks(int limit) {
        List<ChunkPosition> drained = new ArrayList<ChunkPosition>();
        while (limit-- > 0) {
            ChunkPosition position = pollChunk();
            if (position == null) {
                break;
            }
            drained.add(position);
        }
        return drained;
    }

    public List<BlockPosition> drainContainers(int limit) {
        List<BlockPosition> drained = new ArrayList<BlockPosition>();
        while (limit-- > 0) {
            BlockPosition position = pollContainer();
            if (position == null) {
                break;
            }
            drained.add(position);
        }
        return drained;
    }

    public List<ChunkPosition> snapshotChunks() {
        return Collections.unmodifiableList(new ArrayList<ChunkPosition>(chunks));
    }

    public List<BlockPosition> snapshotContainers() {
        return Collections.unmodifiableList(new ArrayList<BlockPosition>(containers));
    }

    public void clear() {
        chunks.clear();
        containers.clear();
        queuedChunks.clear();
        queuedContainers.clear();
    }

    public static final class ChunkPosition {
        public final String world;
        public final int x;
        public final int z;

        public ChunkPosition(String world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        private String key() {
            return world + ":" + x + ":" + z;
        }
    }

    public static final class BlockPosition {
        public final String world;
        public final int x;
        public final int y;
        public final int z;

        public BlockPosition(String world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private String key() {
            return world + ":" + x + ":" + y + ":" + z;
        }
    }
}
