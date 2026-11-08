package com.lootmanager.loot;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LootTable {

    private final String name;
    private String parentName;
    private final Map<String, Map<String, Object>> items = new LinkedHashMap<String, Map<String, Object>>();
    private int minRolls = 1;
    private int maxRolls = 1;
    private long lastEdited = System.currentTimeMillis();

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
        this.parentName = parentName == null || parentName.trim().isEmpty() ? null : parentName;
        touch();
    }

    public void clearParent() {
        this.parentName = null;
        touch();
    }

    public void addItem(String itemName, Map<String, Object> itemData) {
        if (itemName == null || !itemName.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Item key must contain only letters, numbers, underscores, or hyphens.");
        }
        items.put(itemName, itemData == null
                ? Collections.<String, Object>emptyMap()
                : new LinkedHashMap<String, Object>(itemData));
        touch();
    }

    public void removeItem(String itemName) {
        if (items.remove(itemName) != null) {
            touch();
        }
    }

    public void clearItems() {
        if (!items.isEmpty()) {
            items.clear();
            touch();
        }
    }

    public Map<String, Object> getItem(String itemName) {
        Map<String, Object> item = items.get(itemName);
        return item == null ? null : new LinkedHashMap<String, Object>(item);
    }

    public Map<String, Map<String, Object>> getItems() {
        Map<String, Map<String, Object>> copy = new LinkedHashMap<String, Map<String, Object>>();
        for (Map.Entry<String, Map<String, Object>> entry : items.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<String, Object>(entry.getValue()));
        }
        return copy;
    }

    public int getMinRolls() {
        return minRolls;
    }

    public void setMinRolls(int minRolls) {
        this.minRolls = Math.min(100, Math.max(0, minRolls));
        if (this.maxRolls < this.minRolls) {
            this.maxRolls = this.minRolls;
        }
        touch();
    }

    public int getMaxRolls() {
        return maxRolls;
    }

    public void setMaxRolls(int maxRolls) {
        this.maxRolls = Math.min(100, Math.max(0, maxRolls));
        if (this.minRolls > this.maxRolls) {
            this.minRolls = this.maxRolls;
        }
        touch();
    }

    public long getLastEdited() {
        return lastEdited;
    }

    public void setLastEdited(long lastEdited) {
        this.lastEdited = Math.max(0L, lastEdited);
    }

    private void touch() {
        lastEdited = System.currentTimeMillis();
    }

    public Map<String, Map<String, Object>> resolveItems(Map<String, LootTable> tables) {
        return resolveItems(tables, new HashSet<String>());
    }

    private Map<String, Map<String, Object>> resolveItems(Map<String, LootTable> tables, Set<String> visiting) {
        if (!visiting.add(name)) {
            throw new IllegalStateException("Circular inheritance detected for table: " + name);
        }
        Map<String, Map<String, Object>> merged = new LinkedHashMap<String, Map<String, Object>>();
        if (parentName != null) {
            LootTable parent = tables.get(parentName);
            if (parent == null) {
                throw new IllegalStateException("Unknown parent table '" + parentName + "' for '" + name + "'.");
            }
            merged.putAll(parent.resolveItems(tables, visiting));
        }
        merged.putAll(items);
        visiting.remove(name);
        return merged;
    }

    public static void validateInheritanceGraph(Map<String, LootTable> tables) {
        Set<String> visited = new HashSet<String>();
        Set<String> visiting = new HashSet<String>();

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
        if (table.getParentName() != null && !tables.containsKey(table.getParentName())) {
            throw new IllegalStateException("Unknown parent table '" + table.getParentName() + "' for '" + tableName + "'.");
        }
        if (table.getParentName() != null) {
            visit(table.getParentName(), tables, visited, visiting);
        }
        visiting.remove(tableName);
        visited.add(tableName);
    }
}
