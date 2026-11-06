package com.lootmanager.region;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RegionManager {

    private final Map<String, LootRegion> regions = new LinkedHashMap<String, LootRegion>();

    public LootRegion createRegion(String name, String worldName, String tableName, int regenerationMinutes) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Region name cannot be empty.");
        }
        LootRegion region = new LootRegion(name);
        region.setWorldName(worldName);
        region.setTableName(tableName);
        region.setRegenerationMinutes(regenerationMinutes);
        regions.put(name, region);
        return region;
    }

    public LootRegion getRegion(String name) {
        return regions.get(name);
    }

    public List<String> listRegions() {
        List<String> names = new ArrayList<String>(regions.keySet());
        Collections.sort(names);
        return names;
    }

    public void enableRegion(String name) {
        LootRegion region = regions.get(name);
        if (region != null) {
            region.setEnabled(true);
        }
    }

    public void disableRegion(String name) {
        LootRegion region = regions.get(name);
        if (region != null) {
            region.setEnabled(false);
        }
    }

    public void setTable(String name, String tableName) {
        LootRegion region = regions.get(name);
        if (region != null) {
            region.setTableName(tableName);
        }
    }

    public void setRegenerationMinutes(String name, int minutes) {
        LootRegion region = regions.get(name);
        if (region != null) {
            region.setRegenerationMinutes(minutes);
        }
    }

    public void loadFromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("regions-config");
        if (section == null) {
            return;
        }
        for (String regionName : section.getKeys(false)) {
            ConfigurationSection regionSection = section.getConfigurationSection(regionName);
            if (regionSection == null) {
                continue;
            }
            LootRegion region = new LootRegion(regionName);
            region.setWorldName(regionSection.getString("world"));
            region.setTableName(regionSection.getString("table"));
            region.setRegenerationMinutes(regionSection.getInt("regen-minutes", 60));
            region.setEnabled(regionSection.getBoolean("enabled", true));
            region.setMinX(regionSection.getInt("pos1.x", 0));
            region.setMinY(regionSection.getInt("pos1.y", 0));
            region.setMinZ(regionSection.getInt("pos1.z", 0));
            region.setMaxX(regionSection.getInt("pos2.x", 0));
            region.setMaxY(regionSection.getInt("pos2.y", 0));
            region.setMaxZ(regionSection.getInt("pos2.z", 0));
            regions.put(regionName, region);
        }
    }

    public void saveToConfig(FileConfiguration config) {
        config.set("regions-config", null);
        for (Map.Entry<String, LootRegion> entry : regions.entrySet()) {
            LootRegion region = entry.getValue();
            config.set("regions-config." + entry.getKey() + ".world", region.getWorldName());
            config.set("regions-config." + entry.getKey() + ".table", region.getTableName());
            config.set("regions-config." + entry.getKey() + ".regen-minutes", region.getRegenerationMinutes());
            config.set("regions-config." + entry.getKey() + ".enabled", region.isEnabled());
            config.set("regions-config." + entry.getKey() + ".pos1.x", region.getMinX());
            config.set("regions-config." + entry.getKey() + ".pos1.y", region.getMinY());
            config.set("regions-config." + entry.getKey() + ".pos1.z", region.getMinZ());
            config.set("regions-config." + entry.getKey() + ".pos2.x", region.getMaxX());
            config.set("regions-config." + entry.getKey() + ".pos2.y", region.getMaxY());
            config.set("regions-config." + entry.getKey() + ".pos2.z", region.getMaxZ());
        }
    }
}
