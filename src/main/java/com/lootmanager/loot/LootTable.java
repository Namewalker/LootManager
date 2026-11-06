package com.lootmanager.loot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LootTable {

    private final String name;
    private String parentName;
    private final Map<String, Map<String, Object>> items = new LinkedHashMap<String, Map<String, Object>>();
    private int minRolls = 1;
    private int maxRolls = 1;

    public LootTable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public void clearParent() {
        this.parentName = null;
    }

    public void addItem(String itemName, Map<String, Object> itemData) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
        items.put(itemName, itemData == null ? Collections.<String, Object>emptyMap() : itemData);
    }

    public Map<String, Object> getItem(String itemName) {
        return items.get(itemName);
    }

    public Map<String, Map<String, Object>> getItems() {
        return new LinkedHashMap<String, Map<String, Object>>(items);
    }

    public int getMinRolls() {
        return minRolls;
    }

    public void setMinRolls(int minRolls) {
        this.minRolls = Math.max(0, minRolls);
    }

    public int getMaxRolls() {
        return maxRolls;
    }

    public void setMaxRolls(int maxRolls) {
        this.maxRolls = Math.max(0, maxRolls);
    }

    public Map<String, Map<String, Object>> resolveItems(Map<String, LootTable> tables) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<String, Map<String, Object>>();
        if (parentName != null) {
            LootTable parent = tables.get(parentName);
            if (parent != null) {
                merged.putAll(parent.resolveItems(tables));
            }
        }
        merged.putAll(items);
        return merged;
    }

    public static void validateInheritanceGraph(Map<String, LootTable> tables) {
        Set<String> visited = new java.util.HashSet<String>();
        Set<String> visiting = new java.util.HashSet<String>();

        for (String tableName : tables.keySet()) {
            visit(tableName, tables, visited, visiting);
        }
    }

    private static void visit(String tableName,
                             Map<String, LootTable> tables,
                             Set<String> visited,
                             Set<String> visiting) {
        if (tableName == null) {
            return;
        }
        if (visiting.contains(tableName)) {
            throw new IllegalStateException("Circular inheritance detected for table: " + tableName);
        }
        if (visited.contains(tableName)) {
            return;
        }

        LootTable table = tables.get(tableName);
        if (table == null) {
            return;
        }

        visiting.add(tableName);
        if (table.getParentName() != null && tables.containsKey(table.getParentName())) {
            visit(table.getParentName(), tables, visited, visiting);
        }
        visiting.remove(tableName);
        visited.add(tableName);
    }
}
