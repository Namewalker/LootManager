package com.lootmanager;

import com.lootmanager.command.LootCommandExecutor;
import com.lootmanager.loot.LootTableRegistry;
import com.lootmanager.region.RegionManager;
import com.lootmanager.scan.ScanQueue;
import org.bukkit.plugin.java.JavaPlugin;

public final class LootManagerPlugin extends JavaPlugin {

    private LootTableRegistry tableRegistry;
    private RegionManager regionManager;
    private ScanQueue scanQueue;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tableRegistry = new LootTableRegistry();
        tableRegistry.loadFromConfig(getConfig());

        regionManager = new RegionManager();
        regionManager.loadFromConfig(getConfig());

        scanQueue = new ScanQueue();

        if (getCommand("loot") != null) {
            getCommand("loot").setExecutor(new LootCommandExecutor(this));
        }

        getLogger().info("LootManager enabled.");
    }

    @Override
    public void onDisable() {
        if (tableRegistry != null) {
            tableRegistry.saveToConfig(getConfig());
        }
        if (regionManager != null) {
            regionManager.saveToConfig(getConfig());
        }
        saveConfig();
        getLogger().info("LootManager disabled.");
    }

    public LootTableRegistry getTableRegistry() {
        return tableRegistry;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public ScanQueue getScanQueue() {
        return scanQueue;
    }
}
