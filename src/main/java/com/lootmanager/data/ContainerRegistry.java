package com.lootmanager.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContainerRegistry {

    private YamlConfiguration config;

    public ContainerRegistry(YamlConfiguration config) {
        this.config = config;
    }

    public void loadFromConfig(YamlConfiguration config) {
        this.config = config;
    }

    public void saveToConfig(YamlConfiguration target) {
        if (target == null) {
            return;
        }
        if (config == null) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection("worlds");
        if (section == null) {
            target.set("worlds", null);
            return;
        }
        target.set("worlds", null);
        target.createSection("worlds", copySection(section));
    }

    public void addContainer(String world, int x, int y, int z, String type) {
        if (world == null || world.trim().isEmpty()) {
            throw new IllegalArgumentException("World name cannot be empty.");
        }
        String encodedWorld = encodeWorld(world);
        ConfigurationSection worldSection = config.getConfigurationSection("worlds." + encodedWorld);
        if (worldSection == null) {
            worldSection = config.createSection("worlds." + encodedWorld);
        }
        ConfigurationSection containerSection = worldSection.getConfigurationSection("containers");
        if (containerSection == null) {
            containerSection = worldSection.createSection("containers");
        }
        for (String key : containerSection.getKeys(false)) {
            if (matches(containerSection.getConfigurationSection(key), x, y, z)) {
                return;
            }
        }
        String nextKey = nextIndex(containerSection);
        containerSection.set(nextKey + ".x", x);
        containerSection.set(nextKey + ".y", y);
        containerSection.set(nextKey + ".z", z);
        containerSection.set(nextKey + ".type", type == null ? "UNKNOWN" : type);
    }

    public void removeContainer(String world, int x, int y, int z) {
        String encodedWorld = encodeWorld(world);
        ConfigurationSection worldSection = config.getConfigurationSection("worlds." + encodedWorld);
        if (worldSection == null) {
            return;
        }
        ConfigurationSection containerSection = worldSection.getConfigurationSection("containers");
        if (containerSection == null) {
            return;
        }
        for (String key : new ArrayList<String>(containerSection.getKeys(false))) {
            ConfigurationSection entry = containerSection.getConfigurationSection(key);
            if (matches(entry, x, y, z)) {
                containerSection.set(key, null);
            }
        }
        if (containerSection.getKeys(false).isEmpty()) {
            worldSection.set("containers", null);
        }
    }

    public boolean contains(String world, int x, int y, int z) {
        return getContainer(world, x, y, z) != null;
    }

    public boolean isWorldBuilt(String world) {
        if (world == null || world.trim().isEmpty()) {
            return false;
        }
        return config.getBoolean("worlds." + encodeWorld(world) + ".built", false);
    }

    public List<Map<String, Object>> getContainers(String world) {
        return getContainersInternal(encodeWorld(world));
    }

    public int getContainerCount(String world) {
        return getContainers(world).size();
    }

    public void markWorldBuilt(String world, boolean built) {
        if (world == null || world.trim().isEmpty()) {
            return;
        }
        config.set("worlds." + encodeWorld(world) + ".built", built);
    }

    public void clearWorld(String world) {
        if (world == null || world.trim().isEmpty()) {
            return;
        }
        config.set("worlds." + encodeWorld(world), null);
    }

    private Map<String, Object> getContainer(String world, int x, int y, int z) {
        for (Map<String, Object> entry : getContainers(world)) {
            if (matches(entry, x, y, z)) {
                return entry;
            }
        }
        return null;
    }

    private List<Map<String, Object>> getContainersInternal(String encodedWorld) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (config == null) {
            return result;
        }
        ConfigurationSection section = config.getConfigurationSection("worlds." + encodedWorld + ".containers");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                Map<String, Object> copied = new LinkedHashMap<String, Object>();
                for (String itemKey : entry.getKeys(false)) {
                    copied.put(itemKey, entry.get(itemKey));
                }
                result.add(copied);
            }
            return result;
        }
        List<?> entries = config.getList("worlds." + encodedWorld + ".containers");
        if (entries == null) {
            return result;
        }
        for (Object entry : entries) {
            if (!(entry instanceof Map)) {
                continue;
            }
            Map<?, ?> source = (Map<?, ?>) entry;
            Map<String, Object> copied = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> item : source.entrySet()) {
                copied.put(String.valueOf(item.getKey()), item.getValue());
            }
            result.add(copied);
        }
        return result;
    }

    private boolean matches(ConfigurationSection entry, int x, int y, int z) {
        if (entry == null) {
            return false;
        }
        return entry.getInt("x", Integer.MIN_VALUE) == x
                && entry.getInt("y", Integer.MIN_VALUE) == y
                && entry.getInt("z", Integer.MIN_VALUE) == z;
    }

    private boolean matches(Map<String, Object> entry, int x, int y, int z) {
        if (entry == null) {
            return false;
        }
        Object xValue = entry.get("x");
        Object yValue = entry.get("y");
        Object zValue = entry.get("z");
        if (xValue == null || yValue == null || zValue == null) {
            return false;
        }
        return Integer.parseInt(String.valueOf(xValue)) == x
                && Integer.parseInt(String.valueOf(yValue)) == y
                && Integer.parseInt(String.valueOf(zValue)) == z;
    }

    private String nextIndex(ConfigurationSection containerSection) {
        int next = 0;
        for (String key : containerSection.getKeys(false)) {
            try {
                next = Math.max(next, Integer.parseInt(key) + 1);
            } catch (NumberFormatException ignored) {
                // Ignore non-numeric keys.
            }
        }
        return Integer.toString(next);
    }

    private Map<String, Object> copySection(ConfigurationSection section) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                result.put(key, copySection((ConfigurationSection) value));
            } else if (value instanceof List) {
                result.put(key, copyList((List<?>) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private List<Object> copyList(List<?> list) {
        List<Object> result = new ArrayList<Object>();
        for (Object value : list) {
            if (value instanceof ConfigurationSection) {
                result.add(copySection((ConfigurationSection) value));
            } else if (value instanceof List) {
                result.add(copyList((List<?>) value));
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private String encodeWorld(String world) {
        return world.replace(".", "%2E").replace("%", "%25").replace("\\", "%5C");
    }
}
