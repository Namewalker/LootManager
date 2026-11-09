package com.lootmanager.scan;

import com.lootmanager.LootManagerPlugin;
import com.lootmanager.data.GeneratedContainerStore;
import com.lootmanager.loot.LootGenerator;
import com.lootmanager.loot.LootTable;
import com.lootmanager.region.LootRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ScanManager {

    private final LootManagerPlugin plugin;
    private final ScanQueue queue;
    private final LootGenerator generator = new LootGenerator();
    private final GeneratedContainerStore generated;
    private final Random random = new Random();
    private final List<ScanQueue.ChunkPosition> pendingUnload = new ArrayList<ScanQueue.ChunkPosition>();
    private ScanJob activeJob;
    private RegistryRebuildJob registryRebuildJob;
    private BukkitTask task;
    private long containersFound;
    private long containersFilled;
    private long containersSkipped;
    private long scansCompleted;
    private long refillsPerformed;
    private long totalScanTimeMillis;
    private long lastGeneratedSaveAt;
    private boolean generatedDataDirty;
    private final Map<String, Long> generatedItems = new HashMap<String, Long>();
    private final Deque<LootRegion> pendingRefills = new ArrayDeque<LootRegion>();

    public ScanManager(LootManagerPlugin plugin, ScanQueue queue, GeneratedContainerStore generated) {
        this.plugin = plugin;
        this.queue = queue;
        this.generated = generated;
    }

    public boolean start(World world, String tableName, int minChunkX, int maxChunkX,
                         int minChunkZ, int maxChunkZ, boolean loadedOnly, LootRegion region) {
        if (world == null) {
            throw new IllegalArgumentException("Target world is not loaded.");
        }
        if (isActive()) {
            return false;
        }
        LootTable table = plugin.getTableRegistry().getTable(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Unknown loot table: " + tableName);
        }
        try {
            plugin.getTableRegistry().validateTables();
        } catch (IllegalStateException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }
        if (!plugin.getContainerRegistry().isWorldBuilt(world.getName())) {
            throw new IllegalArgumentException("Container registry for world " + world.getName()
                    + " is not built. Run /loot registry rebuild " + world.getName() + " first.");
        }

        queue.clear();
        unloadDrainedChunks();
        activeJob = new ScanJob(world.getName(), tableName, loadedOnly, region);
        for (Map<String, Object> container : plugin.getContainerRegistry().getContainers(world.getName())) {
            Integer x = parseCoordinate(container.get("x"));
            Integer y = parseCoordinate(container.get("y"));
            Integer z = parseCoordinate(container.get("z"));
            if (x == null || y == null || z == null
                    || (x >> 4) < minChunkX || (x >> 4) > maxChunkX
                    || (z >> 4) < minChunkZ || (z >> 4) > maxChunkZ) {
                continue;
            }
            if (region != null && (x < region.getMinX() || x > region.getMaxX()
                    || y < region.getMinY() || y > region.getMaxY()
                    || z < region.getMinZ() || z > region.getMaxZ())) {
                continue;
            }
            if (loadedOnly && !world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            if (queue.enqueueContainer(world.getName(), x, y, z)) {
                activeJob.totalContainers++;
                activeJob.containersFound++;
                containersFound++;
            }
        }

        schedule();
        return true;
    }

    public boolean startRegistryRebuild(World world, CommandSender sender) {
        if (world == null) {
            throw new IllegalArgumentException("Target world is not loaded.");
        }
        if (isActive()) {
            return false;
        }
        WorldBorder border = world.getWorldBorder();
        double halfSize = border.getSize() / 2.0D;
        int minChunkX = ((int) Math.floor(border.getCenter().getX() - halfSize)) >> 4;
        int maxChunkX = ((int) Math.floor(border.getCenter().getX() + halfSize)) >> 4;
        int minChunkZ = ((int) Math.floor(border.getCenter().getZ() - halfSize)) >> 4;
        int maxChunkZ = ((int) Math.floor(border.getCenter().getZ() + halfSize)) >> 4;

        RegionChunkScanner scanner;
        try {
            scanner = new RegionChunkScanner(world.getWorldFolder(), minChunkX, maxChunkX, minChunkZ, maxChunkZ);
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Could not read saved region files for " + world.getName()
                    + ": " + exception.getMessage());
        }

        Deque<ScanQueue.ChunkPosition> loadedChunks = new ArrayDeque<ScanQueue.ChunkPosition>();
        Set<Long> loadedChunkKeys = new HashSet<Long>();
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() < minChunkX || chunk.getX() > maxChunkX
                    || chunk.getZ() < minChunkZ || chunk.getZ() > maxChunkZ) {
                continue;
            }
            loadedChunks.add(new ScanQueue.ChunkPosition(world.getName(), chunk.getX(), chunk.getZ()));
            loadedChunkKeys.add(chunkKey(chunk.getX(), chunk.getZ()));
        }

        queue.clear();
        plugin.getContainerRegistry().clearWorld(world.getName());
        plugin.getContainerRegistry().markWorldBuilt(world.getName(), false);
        plugin.saveContainerData();
        registryRebuildJob = new RegistryRebuildJob(world.getName(), sender, scanner, loadedChunks, loadedChunkKeys);
        schedule();
        return true;
    }

    public boolean isRegistryRebuilding(String worldName) {
        return registryRebuildJob != null && registryRebuildJob.worldName.equals(worldName);
    }

    public boolean isActive() {
        return activeJob != null || registryRebuildJob != null;
    }

    public boolean isPaused() {
        return (activeJob != null && activeJob.paused)
                || (registryRebuildJob != null && registryRebuildJob.paused);
    }

    public void pause() {
        if (activeJob != null) {
            activeJob.paused = true;
        }
        if (registryRebuildJob != null) {
            registryRebuildJob.paused = true;
        }
    }

    public void resume() {
        if (activeJob != null) {
            activeJob.paused = false;
        }
        if (registryRebuildJob != null) {
            registryRebuildJob.paused = false;
        }
    }

    public void cancel() {
        pendingRefills.clear();
        if (activeJob != null) {
            activeJob.cancelled = true;
            queue.clear();
        }
        if (registryRebuildJob != null) {
            registryRebuildJob.cancelled = true;
        }
    }

    public int startAllRegionRefills() {
        pendingRefills.clear();
        for (LootRegion region : plugin.getRegionManager().getRegions()) {
            if (region.isEnabled() && region.getWorldName() != null && region.getTableName() != null) {
                pendingRefills.add(region);
            }
        }
        int count = pendingRefills.size();
        if (!isActive()) {
            startNextRegionRefill();
        }
        return count;
    }

    public String getProgress() {
        if (registryRebuildJob != null) {
            return "Registry rebuild: world " + registryRebuildJob.worldName + ", existing chunks scanned: "
                    + registryRebuildJob.chunksProcessed + (registryRebuildJob.paused ? " (paused)" : "");
        }
        if (activeJob == null) {
            return "No scan is active. Completed scans: " + scansCompleted + ".";
        }
        return "World: " + activeJob.worldName + ", containers: " + activeJob.containersProcessed + "/" + activeJob.totalContainers
                + ", containers found: " + activeJob.containersFound + ", filled: " + activeJob.containersFilled
                + ", skipped: " + activeJob.containersSkipped + ", queued: "
            + queue.getContainerQueueSize() + " containers" + (activeJob.paused ? " (paused)" : "");
    }

    public String getStatistics() {
        String mostGenerated = "none";
        long mostCount = 0L;
        for (Map.Entry<String, Long> entry : generatedItems.entrySet()) {
            if (entry.getValue() > mostCount) {
                mostGenerated = entry.getKey() + " (" + entry.getValue() + ")";
                mostCount = entry.getValue();
            }
        }
        long averageMillis = scansCompleted == 0L ? 0L : totalScanTimeMillis / scansCompleted;
        return "Containers found: " + containersFound + ", filled: " + containersFilled
                + ", skipped: " + containersSkipped + ", regions active: "
                + plugin.getRegionManager().getRegions().stream().filter(LootRegion::isEnabled).count()
                + ", refills performed: " + refillsPerformed + ", most generated item: " + mostGenerated
                + ", average scan time: " + (averageMillis / 1000.0D) + "s";
    }

    public void stop() {
        pendingRefills.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
        queue.clear();
        unloadDrainedChunks();
        activeJob = null;
        if (registryRebuildJob != null) {
            String rebuildingWorld = registryRebuildJob.worldName;
            registryRebuildJob = null;
            plugin.getContainerRegistry().markWorldBuilt(rebuildingWorld, false);
            plugin.saveContainerData();
        }
        saveGeneratedDataIfDirty();
    }

    private void schedule() {
        int interval = Math.max(1, plugin.getConfig().getInt("scan.scan-interval-ticks", 1));
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                processTick();
            }
        }, 1L, interval);
    }

    private void processTick() {
        if (registryRebuildJob != null) {
            processRegistryRebuildTick();
            return;
        }
        if (activeJob == null) {
            return;
        }
        if (activeJob.cancelled) {
            finish(false);
            return;
        }
        if (activeJob.paused) {
            return;
        }

        int chunkBudget = Math.max(1, plugin.getConfig().getInt("scan.chunks-per-tick", 1));
        int containerBudget = Math.max(1, plugin.getConfig().getInt("scan.containers-per-tick", 50));
        Set<String> chunksLoadedThisTick = new HashSet<String>();
        for (int i = 0; i < containerBudget; i++) {
            ScanQueue.BlockPosition position = queue.pollContainer();
            if (position == null) {
                break;
            }
            if (prepareContainerChunk(position, chunksLoadedThisTick, chunkBudget)) {
                break;
            }
            activeJob.containersProcessed++;
            processContainer(position);
        }
        if (generatedDataDirty && System.currentTimeMillis() - lastGeneratedSaveAt >= 5000L) {
            saveGeneratedDataIfDirty();
        }
        unloadDrainedChunks();

        if (queue.getContainerQueueSize() == 0) {
            finish(true);
        }
    }

    private void processRegistryRebuildTick() {
        RegistryRebuildJob job = registryRebuildJob;
        if (job.cancelled) {
            finishRegistryRebuild(false, "Registry rebuild cancelled. The world cache remains unbuilt.");
            return;
        }
        if (job.paused) {
            return;
        }
        World world = Bukkit.getWorld(job.worldName);
        if (world == null) {
            finishRegistryRebuild(false, "Registry rebuild stopped because the world is no longer loaded.");
            return;
        }

        int chunkBudget = Math.max(1, plugin.getConfig().getInt("scan.chunks-per-tick", 1));
        try {
            for (int i = 0; i < chunkBudget; i++) {
                ScanQueue.ChunkPosition position = nextRegistryChunk(job);
                if (position == null) {
                    finishRegistryRebuild(true, "Registry rebuild complete. Indexed "
                            + plugin.getContainerRegistry().getContainerCount(job.worldName)
                            + " tracked containers across " + job.chunksProcessed + " existing chunks.");
                    return;
                }
                boolean wasLoaded = world.isChunkLoaded(position.x, position.z);
                if (!wasLoaded && !world.loadChunk(position.x, position.z, false)) {
                    continue;
                }
                if (!wasLoaded && plugin.getConfig().getBoolean("scan.unload-after-scan", true)) {
                    pendingUnload.add(position);
                }
                org.bukkit.Chunk chunk = world.getChunkAt(position.x, position.z);
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof InventoryHolder) || !isSupported(state)) {
                        continue;
                    }
                    Block block = state.getBlock();
                    plugin.getContainerRegistry().addContainer(job.worldName, block.getX(), block.getY(),
                            block.getZ(), block.getType().name());
                }
                job.chunksProcessed++;
            }
        } catch (java.io.IOException exception) {
            plugin.getLogger().warning("Registry rebuild failed for " + job.worldName + ": " + exception.getMessage());
            finishRegistryRebuild(false, "Registry rebuild failed while reading saved world chunks.");
            return;
        }
        unloadDrainedChunks();
    }

    private ScanQueue.ChunkPosition nextRegistryChunk(RegistryRebuildJob job) throws java.io.IOException {
        if (!job.loadedChunks.isEmpty()) {
            return job.loadedChunks.poll();
        }
        RegionChunkScanner.ChunkCoordinate coordinate;
        do {
            coordinate = job.scanner.nextChunk();
        } while (coordinate != null && job.loadedChunkKeys.contains(chunkKey(coordinate.x, coordinate.z)));
        return coordinate == null ? null : new ScanQueue.ChunkPosition(job.worldName, coordinate.x, coordinate.z);
    }

    private long chunkKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private void finishRegistryRebuild(boolean completed, String message) {
        RegistryRebuildJob finished = registryRebuildJob;
        if (finished == null) {
            return;
        }
        registryRebuildJob = null;
        plugin.getContainerRegistry().markWorldBuilt(finished.worldName, completed);
        plugin.saveContainerData();
        if (finished.sender != null) {
            finished.sender.sendMessage((completed ? org.bukkit.ChatColor.GREEN : org.bukkit.ChatColor.RED)
                    + message);
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        unloadDrainedChunks();
        if (completed) {
            startNextRegionRefill();
        }
    }

    private boolean prepareContainerChunk(ScanQueue.BlockPosition position, Set<String> chunksLoadedThisTick, int chunkBudget) {
        World world = Bukkit.getWorld(position.world);
        if (world == null || world.isChunkLoaded(position.x >> 4, position.z >> 4) || activeJob.loadedOnly) {
            return false;
        }
        String chunkKey = position.world + ":" + (position.x >> 4) + ":" + (position.z >> 4);
        if (!chunksLoadedThisTick.contains(chunkKey) && chunksLoadedThisTick.size() >= chunkBudget) {
            queue.enqueueContainer(position.world, position.x, position.y, position.z);
            return true;
        }
        world.getChunkAt(position.x >> 4, position.z >> 4);
        chunksLoadedThisTick.add(chunkKey);
        if (plugin.getConfig().getBoolean("scan.unload-after-scan", true)) {
            pendingUnload.add(new ScanQueue.ChunkPosition(position.world, position.x >> 4, position.z >> 4));
        }
        return false;
    }

    private Integer parseCoordinate(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isSupported(BlockState state) {
        if (state instanceof Chest) {
            return true;
        }
        if (state instanceof ShulkerBox) {
            return plugin.getConfig().getBoolean("containers.shulker-boxes", false);
        }
        return plugin.getConfig().getBoolean("containers.any-inventory-holder", false);
    }

    private boolean processContainer(ScanQueue.BlockPosition position) {
        World world = Bukkit.getWorld(position.world);
        if (world == null || !world.isChunkLoaded(position.x >> 4, position.z >> 4)) {
            activeJob.containersSkipped++;
            containersSkipped++;
            return false;
        }
        Block block = world.getBlockAt(position.x, position.y, position.z);
        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder) || !isSupported(state) || isNonCanonicalDoubleChest(block)) {
            activeJob.containersSkipped++;
            containersSkipped++;
            return false;
        }

        if (activeJob.region != null && (position.x < activeJob.region.getMinX() || position.x > activeJob.region.getMaxX()
                || position.y < activeJob.region.getMinY() || position.y > activeJob.region.getMaxY()
                || position.z < activeJob.region.getMinZ() || position.z > activeJob.region.getMaxZ())) {
            activeJob.containersSkipped++;
            containersSkipped++;
            return false;
        }

        String trackingWorld = world.getName();
        boolean previouslyGenerated = generated.contains(trackingWorld, position.x, position.y, position.z);
        if (activeJob.region == null && previouslyGenerated) {
            activeJob.containersSkipped++;
            containersSkipped++;
            return false;
        }
        Inventory inventory = ((InventoryHolder) state).getInventory();
        boolean empty = isInventoryEmpty(inventory);
        if (activeJob.region != null) {
            boolean requireEmpty = plugin.getConfig().getBoolean("refill.require-empty-chest", true)
                || !plugin.getConfig().getBoolean("refill.allow-partial-refills", false);
            boolean requireTracked = plugin.getConfig().getBoolean("refill.refill-only-generated", true);
            if ((requireTracked && !previouslyGenerated)
                || (requireEmpty && !empty)) {
                activeJob.containersSkipped++;
                containersSkipped++;
                return false;
            }
        } else if (!empty && (plugin.getConfig().getBoolean("loot.fill-empty-only", true)
                || !plugin.getConfig().getBoolean("loot.fill-partially-empty", false))) {
            activeJob.containersSkipped++;
            containersSkipped++;
            return false;
        }

        LootTable table = plugin.getTableRegistry().getTable(activeJob.tableName);
        if (table == null) {
            activeJob.containersSkipped++;
            containersSkipped++;
            return false;
        }
        List<ItemStack> items = generator.generate(table, plugin.getTableRegistry().getTables());
        List<Integer> slots = new ArrayList<Integer>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR) {
                slots.add(slot);
            }
        }
        Collections.shuffle(slots, random);
        int inserted = 0;
        for (ItemStack item : items) {
            if (slots.isEmpty()) {
                break;
            }
            inventory.setItem(slots.remove(slots.size() - 1), item);
            Long count = generatedItems.get(item.getType().name());
            generatedItems.put(item.getType().name(), count == null ? 1L : count + 1L);
            inserted++;
        }
        if (inserted == 0) {
            activeJob.containersSkipped++;
            containersSkipped++;
            return false;
        }
        generated.markGenerated(trackingWorld, position.x, position.y, position.z, activeJob.tableName);
        generatedDataDirty = true;
        activeJob.containersFilled++;
        containersFilled++;
        if (activeJob.region != null) {
            refillsPerformed++;
        }
        return true;
    }

    private boolean isNonCanonicalDoubleChest(Block block) {
        Material type = block.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
            return false;
        }
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] offset : offsets) {
            Block neighbor = block.getRelative(offset[0], 0, offset[1]);
            if (neighbor.getType() != type) {
                continue;
            }
            if (neighbor.getX() < block.getX() || (neighbor.getX() == block.getX() && neighbor.getZ() < block.getZ())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private void finish(boolean completed) {
        ScanJob finished = activeJob;
        if (completed) {
            scansCompleted++;
            totalScanTimeMillis += System.currentTimeMillis() - finished.startedAt;
            if (finished.region != null) {
                finished.region.setLastRefill(System.currentTimeMillis());
                plugin.saveRegionData();
            }
        }
        activeJob = null;
        if (task != null) {
            task.cancel();
            task = null;
        }
        queue.clear();
        unloadDrainedChunks();
        saveGeneratedDataIfDirty();
        if (completed) {
            startNextRegionRefill();
        }
    }

    private void saveGeneratedDataIfDirty() {
        if (!generatedDataDirty) {
            return;
        }
        plugin.saveGeneratedData();
        generatedDataDirty = false;
        lastGeneratedSaveAt = System.currentTimeMillis();
    }

    private void unloadDrainedChunks() {
        java.util.Iterator<ScanQueue.ChunkPosition> iterator = pendingUnload.iterator();
        while (iterator.hasNext()) {
            ScanQueue.ChunkPosition chunk = iterator.next();
            if (queue.hasContainerInChunk(chunk.world, chunk.x, chunk.z)) {
                continue;
            }
            World world = Bukkit.getWorld(chunk.world);
            if (world != null) {
                world.unloadChunkRequest(chunk.x, chunk.z);
            }
            iterator.remove();
        }
    }

    private void startNextRegionRefill() {
        while (!isActive() && !pendingRefills.isEmpty()) {
            LootRegion region = pendingRefills.poll();
            World world = Bukkit.getWorld(region.getWorldName());
            if (world == null || plugin.getTableRegistry().getTable(region.getTableName()) == null) {
                continue;
            }
            try {
                if (start(world, region.getTableName(), region.getMinX() >> 4, region.getMaxX() >> 4,
                        region.getMinZ() >> 4, region.getMaxZ() >> 4, false, region)) {
                    return;
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Could not refill region " + region.getName() + ": " + exception.getMessage());
            }
        }
    }

    private static class ScanJob {
        private final String worldName;
        private final String tableName;
        private final boolean loadedOnly;
        private final LootRegion region;
        private long totalContainers;
        private long containersProcessed;
        private long containersFound;
        private long containersFilled;
        private long containersSkipped;
        private boolean paused;
        private boolean cancelled;
        private final long startedAt = System.currentTimeMillis();

        private ScanJob(String worldName, String tableName, boolean loadedOnly, LootRegion region) {
            this.worldName = worldName;
            this.tableName = tableName;
            this.loadedOnly = loadedOnly;
            this.region = region;
        }
    }

    private static class RegistryRebuildJob {
        private final String worldName;
        private final CommandSender sender;
        private final RegionChunkScanner scanner;
        private final Deque<ScanQueue.ChunkPosition> loadedChunks;
        private final Set<Long> loadedChunkKeys;
        private long chunksProcessed;
        private boolean paused;
        private boolean cancelled;

        private RegistryRebuildJob(String worldName, CommandSender sender, RegionChunkScanner scanner,
                                   Deque<ScanQueue.ChunkPosition> loadedChunks, Set<Long> loadedChunkKeys) {
            this.worldName = worldName;
            this.sender = sender;
            this.scanner = scanner;
            this.loadedChunks = loadedChunks;
            this.loadedChunkKeys = loadedChunkKeys;
        }
    }
}
