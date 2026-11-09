package com.lootmanager.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerRegistryTest {

    @Test
    void shouldTrackAndPersistContainersByWorld() {
        YamlConfiguration yaml = new YamlConfiguration();
        ContainerRegistry registry = new ContainerRegistry(yaml);

        registry.markWorldBuilt("Amberlight", true);
        registry.addContainer("Amberlight", 100, 64, 200, "CHEST");
        registry.addContainer("Amberlight", 101, 64, 201, "TRAPPED_CHEST");

        assertTrue(registry.contains("Amberlight", 100, 64, 200));
        assertTrue(registry.isWorldBuilt("Amberlight"));
        assertEquals(2, registry.getContainers("Amberlight").size());

        registry.removeContainer("Amberlight", 100, 64, 200);
        assertFalse(registry.contains("Amberlight", 100, 64, 200));

        YamlConfiguration saved = new YamlConfiguration();
        registry.saveToConfig(saved);
        assertTrue(saved.contains("worlds.Amberlight.containers"));
        assertEquals("TRAPPED_CHEST", saved.getString("worlds.Amberlight.containers.1.type"));
    }

    @Test
    void shouldPersistWorldRegistryBuildStateForOneTimePerWorldCache() {
        YamlConfiguration yaml = new YamlConfiguration();
        ContainerRegistry registry = new ContainerRegistry(yaml);

        registry.markWorldBuilt("Nexus", true);
        assertTrue(registry.isWorldBuilt("Nexus"));

        registry.addContainer("Nexus", 5, 65, 7, "CHEST");
        assertTrue(registry.isWorldBuilt("Nexus"));

        YamlConfiguration saved = new YamlConfiguration();
        registry.saveToConfig(saved);
        assertTrue(saved.getBoolean("worlds.Nexus.built", false));
        assertEquals(1, registry.getContainerCount("Nexus"));
    }

    @Test
    void addingContainerDoesNotMarkAnIncompleteWorldRegistryAsBuilt() {
        ContainerRegistry registry = new ContainerRegistry(new YamlConfiguration());

        registry.addContainer("Nexus", 5, 65, 7, "CHEST");

        assertFalse(registry.isWorldBuilt("Nexus"));
    }

    @Test
    void removingLastContainerPreservesBuiltRegistryState() {
        ContainerRegistry registry = new ContainerRegistry(new YamlConfiguration());
        registry.markWorldBuilt("Nexus", true);
        registry.addContainer("Nexus", 5, 65, 7, "CHEST");

        registry.removeContainer("Nexus", 5, 65, 7);

        assertTrue(registry.isWorldBuilt("Nexus"));
        assertEquals(0, registry.getContainerCount("Nexus"));
    }
}
