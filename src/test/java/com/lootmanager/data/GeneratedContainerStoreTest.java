package com.lootmanager.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedContainerStoreTest {

    @Test
    void shouldTrackAndRemoveWorldCoordinatesWithDotsInWorldName() {
        YamlConfiguration yaml = new YamlConfiguration();
        GeneratedContainerStore store = new GeneratedContainerStore(yaml);

        store.markGenerated("custom.world", -12, 64, 30, "dungeon");

        assertTrue(store.contains("custom.world", -12, 64, 30));
        assertEquals("dungeon", store.getTable("custom.world", -12, 64, 30));
        assertTrue(store.getLastFill("custom.world", -12, 64, 30) > 0L);
        assertTrue(store.remove("custom.world", -12, 64, 30));
        assertFalse(store.contains("custom.world", -12, 64, 30));
    }
}
