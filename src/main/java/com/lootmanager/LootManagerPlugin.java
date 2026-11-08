package com.lootmanager;

import com.lootmanager.command.LootCommandExecutor;
import com.lootmanager.command.LootTabCompleter;
import com.lootmanager.data.GeneratedContainerStore;
import com.lootmanager.data.GeneratedContainerListener;
import com.lootmanager.editor.LootEditor;
import com.lootmanager.loot.LootTableRegistry;
import com.lootmanager.region.RegionManager;
import com.lootmanager.region.SelectionListener;
import com.lootmanager.region.SelectionManager;
import com.lootmanager.scan.ScanQueue;
import com.lootmanager.scan.ScanManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class LootManagerPlugin extends JavaPlugin {

    private LootTableRegistry tableRegistry;
    private RegionManager regionManager;
    private ScanQueue scanQueue;
    private ScanManager scanManager;
    private SelectionManager selectionManager;
    private GeneratedContainerStore generatedContainerStore;
    private LootEditor lootEditor;
    private File lootFile;
    private File regionsFile;
    private File dataFile;
    private File messagesFile;
    private YamlConfiguration lootConfig;
    private YamlConfiguration regionsConfig;
    private YamlConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lootFile = new File(getDataFolder(), "loot.yml");
        regionsFile = new File(getDataFolder(), "regions.yml");
        dataFile = new File(getDataFolder(), "data.yml");
        messagesFile = new File(getDataFolder(), "messages.yml");
        createYamlFile(lootFile);
        createYamlFile(regionsFile);
        createYamlFile(dataFile);
        createYamlFile(messagesFile);
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        tableRegistry = new LootTableRegistry();
        tableRegistry.loadFromConfig(lootConfig);
        try {
            tableRegistry.validateTables();
        } catch (IllegalStateException exception) {
            getLogger().severe("Invalid loot table inheritance: " + exception.getMessage());
        }

        regionManager = new RegionManager();
        regionManager.loadFromConfig(regionsConfig);

        scanQueue = new ScanQueue();
        selectionManager = new SelectionManager();
        generatedContainerStore = new GeneratedContainerStore(dataConfig);
        scanManager = new ScanManager(this, scanQueue, generatedContainerStore);
        lootEditor = new LootEditor(this);

        getServer().getPluginManager().registerEvents(new SelectionListener(this, selectionManager), this);
        getServer().getPluginManager().registerEvents(lootEditor, this);
        getServer().getPluginManager().registerEvents(new GeneratedContainerListener(this, generatedContainerStore), this);

        if (getCommand("loot") != null) {
            getCommand("loot").setExecutor(new LootCommandExecutor(this));
            getCommand("loot").setTabCompleter(new LootTabCompleter(this));
        }

        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                checkRegionRegeneration();
            }
        }, 1200L, 1200L);

        getLogger().info("LootManager enabled.");
    }

    @Override
    public void onDisable() {
        if (tableRegistry != null) {
            tableRegistry.saveToConfig(lootConfig);
            saveYaml(lootConfig, lootFile);
        }
        if (regionManager != null) {
            regionManager.saveToConfig(regionsConfig);
            saveYaml(regionsConfig, regionsFile);
        }
        if (scanManager != null) {
            scanManager.stop();
        }
        if (dataConfig != null) {
            saveGeneratedData();
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

    public ScanManager getScanManager() {
        return scanManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public GeneratedContainerStore getGeneratedContainerStore() {
        return generatedContainerStore;
    }

    public void tableRegistryValidate() {
        tableRegistry.validateTables();
    }

    public void openLootEditor(org.bukkit.entity.Player player, String tableName) {
        lootEditor.openTable(player, tableName, 0);
    }

    public YamlConfiguration getLootConfig() {
        return lootConfig;
    }

    public YamlConfiguration getRegionsConfig() {
        return regionsConfig;
    }

    public YamlConfiguration getDataConfig() {
        return dataConfig;
    }

    public void saveLootData() {
        tableRegistry.saveToConfig(lootConfig);
        saveYaml(lootConfig, lootFile);
    }

    public void saveRegionData() {
        regionManager.saveToConfig(regionsConfig);
        saveYaml(regionsConfig, regionsFile);
    }

    public void saveGeneratedData() {
        saveYaml(dataConfig, dataFile);
    }

    public void reloadPluginData() {
        reloadConfig();
        YamlConfiguration newLootConfig = YamlConfiguration.loadConfiguration(lootFile);
        YamlConfiguration newRegionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
        YamlConfiguration newDataConfig = YamlConfiguration.loadConfiguration(dataFile);
        LootTableRegistry newTableRegistry = new LootTableRegistry();
        newTableRegistry.loadFromConfig(newLootConfig);
        newTableRegistry.validateTables();
        RegionManager newRegionManager = new RegionManager();
        newRegionManager.loadFromConfig(newRegionsConfig);

        if (scanManager != null) {
            scanManager.stop();
        }
        lootConfig = newLootConfig;
        regionsConfig = newRegionsConfig;
        dataConfig = newDataConfig;
        tableRegistry = newTableRegistry;
        regionManager = newRegionManager;
        generatedContainerStore.reload(dataConfig);
    }

    private void checkRegionRegeneration() {
        if (scanManager.isActive() || !getConfig().getBoolean("refill.enabled", true)) {
            return;
        }
        long now = System.currentTimeMillis();
        for (com.lootmanager.region.LootRegion region : regionManager.getRegions()) {
            if (!region.isEnabled() || region.getWorldName() == null || region.getTableName() == null) {
                continue;
            }
            long interval = region.getRegenerationMinutes() * 60L * 1000L;
            if (now - region.getLastRefill() < interval) {
                continue;
            }
            World world = Bukkit.getWorld(region.getWorldName());
            if (world == null || tableRegistry.getTable(region.getTableName()) == null) {
                continue;
            }
            try {
                if (scanManager.start(world, region.getTableName(), region.getMinX() >> 4, region.getMaxX() >> 4,
                        region.getMinZ() >> 4, region.getMaxZ() >> 4, false, region)) {
                    break;
                }
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Could not regenerate region " + region.getName() + ": " + exception.getMessage());
            }
        }
    }

    private void createYamlFile(File file) {
        if (file.exists()) {
            return;
        }
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IOException("Could not create plugin data directory");
            }
            if (getResource(file.getName()) != null) {
                saveResource(file.getName(), false);
            } else if (!file.createNewFile()) {
                throw new IOException("Could not create " + file.getName());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize " + file.getName(), exception);
        }
    }

    private void saveYaml(YamlConfiguration yaml, File file) {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            getLogger().severe("Could not save " + file.getName() + ": " + exception.getMessage());
        }
    }
}
