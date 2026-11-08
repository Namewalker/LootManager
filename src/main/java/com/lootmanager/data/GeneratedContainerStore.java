package com.lootmanager.data;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

public class GeneratedContainerStore {

    private YamlConfiguration data;

    public GeneratedContainerStore(YamlConfiguration data) {
        this.data = data;
    }

    public void reload(YamlConfiguration data) {
        this.data = data;
    }

    public boolean contains(String world, int x, int y, int z) {
        return data.contains(path(world, x, y, z));
    }

    public void markGenerated(String world, int x, int y, int z, String table) {
        String path = path(world, x, y, z);
        data.set(path + ".table", table);
        data.set(path + ".generation-id", UUID.randomUUID().toString());
        data.set(path + ".last-fill", System.currentTimeMillis());
    }

    public long getLastFill(String world, int x, int y, int z) {
        return data.getLong(path(world, x, y, z) + ".last-fill", 0L);
    }

    public String getTable(String world, int x, int y, int z) {
        return data.getString(path(world, x, y, z) + ".table");
    }

    public boolean remove(String world, int x, int y, int z) {
        if (!contains(world, x, y, z)) {
            return false;
        }
        data.set(path(world, x, y, z), null);
        return true;
    }

    private String path(String world, int x, int y, int z) {
        String encodedWorld = world.replace("%", "%25").replace(".", "%2E").replace("\\", "%5C");
        return "generated." + encodedWorld + ":" + x + ":" + y + ":" + z;
    }
}