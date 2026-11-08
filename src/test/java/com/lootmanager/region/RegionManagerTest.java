package com.lootmanager.region;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegionManagerTest {

    @Test
    void shouldRoundTripRegionsUsingReadmeSchema() {
        RegionManager manager = new RegionManager();
        LootRegion region = manager.createRegion("ruins", "Amberlight", "dungeon", 30);
        region.setBounds(250, 90, 250, 100, 50, 100);
        region.setEnabled(false);
        region.setLastRefill(123456L);

        YamlConfiguration yaml = new YamlConfiguration();
        manager.saveToConfig(yaml);
        assertEquals("Amberlight", yaml.getString("regions.ruins.world"));
        assertEquals("dungeon", yaml.getString("regions.ruins.table"));
        assertEquals(100, yaml.getInt("regions.ruins.pos1.x"));
        assertEquals(250, yaml.getInt("regions.ruins.pos2.x"));

        RegionManager loaded = new RegionManager();
        loaded.loadFromConfig(yaml);
        LootRegion restored = loaded.getRegion("ruins");
        assertNotNull(restored);
        assertEquals("Amberlight", restored.getWorldName());
        assertEquals(30, restored.getRegenerationMinutes());
        assertFalse(restored.isEnabled());
        assertEquals(123456L, restored.getLastRefill());
    }
}
