package com.lootmanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ResourceDefaultsTest {

    @Test
    void shouldPackageAllSeparateYamlStoresAndThrottleDefaults() throws Exception {
        YamlConfiguration config = readYaml("/config.yml");
        assertEquals(1, config.getInt("scan.chunks-per-tick"));
        assertEquals(50, config.getInt("scan.containers-per-tick"));
        assertEquals(1, config.getInt("scan.scan-interval-ticks"));

        assertNotNull(getClass().getResource("/loot.yml"));
        assertNotNull(getClass().getResource("/regions.yml"));
        assertNotNull(getClass().getResource("/data.yml"));
        assertNotNull(getClass().getResource("/messages.yml"));

        YamlConfiguration descriptor = readYaml("/plugin.yml");
        assertEquals("1.1.2", descriptor.getString("version"));
        assertEquals("com.lootmanager.LootManagerPlugin", descriptor.getString("main"));
    }

    private YamlConfiguration readYaml(String resource) throws Exception {
        InputStream stream = getClass().getResourceAsStream(resource);
        assertNotNull(stream, "Missing resource " + resource);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }
}
