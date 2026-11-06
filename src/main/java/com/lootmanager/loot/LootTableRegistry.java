package com.lootmanager.loot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LootTableRegistry {

    private final Map<String, LootTable> tables = new LinkedHashMap<String, LootTable>();

    public LootTable createTable(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty.");
        }
        if (tables.containsKey(name)) {
            return tables.get(name);
        }
        LootTable table = new LootTable(name);
        tables.put(name, table);
        return table;
    }

    public void deleteTable(String name) {
        tables.remove(name);
    }

    public void renameTable(String oldName, String newName) {
        if (!tables.containsKey(oldName)) {
            throw new IllegalArgumentException("Table does not exist: " + oldName);
        }
        LootTable table = tables.remove(oldName);
        tables.put(newName, table);
    }

    public LootTable getTable(String name) {
        return tables.get(name);
    }

    public List<String> listTables() {
        List<String> names = new ArrayList<String>(tables.keySet());
        Collections.sort(names);
        return names;
    }

    public Map<String, LootTable> getTables() {
        return new LinkedHashMap<String, LootTable>(tables);
    }

    public void validateTables() {
        LootTable.validateInheritanceGraph(tables);
    }

    public void loadFromConfig(FileConfiguration config) {
        ConfigurationSection tablesSection = config.getConfigurationSection("tables");
        if (tablesSection == null) {
            return;
        }

        for (String tableName : tablesSection.getKeys(false)) {
            ConfigurationSection tableSection = tablesSection.getConfigurationSection(tableName);
            if (tableSection == null) {
                continue;
            }
            LootTable table = new LootTable(tableName);
            if (tableSection.contains("inherits")) {
                table.setParentName(tableSection.getString("inherits"));
            }
            if (tableSection.contains("settings.min-rolls")) {
                table.setMinRolls(tableSection.getInt("settings.min-rolls"));
            }
            if (tableSection.contains("settings.max-rolls")) {
                table.setMaxRolls(tableSection.getInt("settings.max-rolls"));
            }

            ConfigurationSection itemsSection = tableSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemName : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemName);
                    Map<String, Object> itemData = itemSection == null ? new LinkedHashMap<String, Object>() : itemSection.getValues(false);
                    table.addItem(itemName, itemData);
                }
            }
            tables.put(tableName, table);
        }
    }

    public void saveToConfig(FileConfiguration config) {
        config.set("tables", null);
        for (Map.Entry<String, LootTable> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            LootTable table = entry.getValue();
            config.set("tables." + tableName + ".inherits", table.getParentName());
            config.set("tables." + tableName + ".settings.min-rolls", table.getMinRolls());
            config.set("tables." + tableName + ".settings.max-rolls", table.getMaxRolls());

            Map<String, Map<String, Object>> items = table.getItems();
            for (Map.Entry<String, Map<String, Object>> itemEntry : items.entrySet()) {
                config.set("tables." + tableName + ".items." + itemEntry.getKey(), itemEntry.getValue());
            }
        }
    }
}
