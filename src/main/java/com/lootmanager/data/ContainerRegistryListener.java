package com.lootmanager.data;

import com.lootmanager.LootManagerPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;

public class ContainerRegistryListener implements Listener {

    private final LootManagerPlugin plugin;
    private final ContainerRegistry registry;

    public ContainerRegistryListener(LootManagerPlugin plugin, ContainerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        String world = block.getWorld().getName();
        if ((!registry.isWorldBuilt(world) && !plugin.getScanManager().isRegistryRebuilding(world))
            || !isTrackedContainer(block)) {
            return;
        }
        registry.addContainer(world, block.getX(), block.getY(), block.getZ(), block.getType().name());
        plugin.saveContainerData();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String world = block.getWorld().getName();
        if ((!registry.isWorldBuilt(world) && !plugin.getScanManager().isRegistryRebuilding(world))
            || !isTrackedContainer(block)) {
            return;
        }
        registry.removeContainer(world, block.getX(), block.getY(), block.getZ());
        plugin.saveContainerData();
    }

    private boolean isTrackedContainer(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
            return true;
        }
        BlockState state = block.getState();
        if (state instanceof org.bukkit.block.ShulkerBox) {
            return plugin.getConfig().getBoolean("containers.shulker-boxes", false);
        }
        return state instanceof InventoryHolder && plugin.getConfig().getBoolean("containers.any-inventory-holder", false);
    }
}
