package com.lootmanager.data;

import com.lootmanager.LootManagerPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class GeneratedContainerListener implements Listener {

    private final LootManagerPlugin plugin;
    private final GeneratedContainerStore generated;

    public GeneratedContainerListener(LootManagerPlugin plugin, GeneratedContainerStore generated) {
        this.plugin = plugin;
        this.generated = generated;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        boolean removed = clearTracking(block);
        if (isChest(block.getType())) {
            for (int[] offset : horizontalOffsets()) {
                Block neighbor = block.getRelative(offset[0], 0, offset[1]);
                if (isChest(neighbor.getType())) {
                    removed |= clearTracking(neighbor);
                }
            }
        }
        if (removed) {
            plugin.saveGeneratedData();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        boolean removed = clearTracking(block);
        if (isChest(block.getType())) {
            for (int[] offset : horizontalOffsets()) {
                Block neighbor = block.getRelative(offset[0], 0, offset[1]);
                if (isChest(neighbor.getType())) {
                    removed |= clearTracking(neighbor);
                }
            }
        }
        if (removed) {
            plugin.saveGeneratedData();
        }
    }

    private boolean clearTracking(Block block) {
        return generated.remove(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private boolean isChest(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }

    private int[][] horizontalOffsets() {
        return new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    }
}
