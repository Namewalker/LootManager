package com.lootmanager.loot;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LootTableRegistryTest {

    @Test
    void shouldRoundTripTablesAndNestedItemSettings() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("tables.starter.items.bread.material", "BREAD");
        yaml.set("tables.starter.items.bread.chance", 0.75D);
        yaml.set("tables.starter.items.bread.weight", 100);
        yaml.set("tables.starter.items.bread.amount.min", 1);
        yaml.set("tables.starter.items.bread.amount.max", 3);
        yaml.set("tables.dungeon.inherits", "starter");
        yaml.set("tables.dungeon.settings.min-rolls", 2);
        yaml.set("tables.dungeon.settings.max-rolls", 6);

        LootTableRegistry registry = new LootTableRegistry();
        registry.loadFromConfig(yaml);
        registry.validateTables();

        YamlConfiguration saved = new YamlConfiguration();
        registry.saveToConfig(saved);
        assertEquals("starter", saved.getString("tables.dungeon.inherits"));
        assertEquals(2, saved.getInt("tables.dungeon.settings.min-rolls"));
        assertEquals("BREAD", saved.getString("tables.starter.items.bread.material"), saved.saveToString());
        assertEquals(3, saved.getInt("tables.starter.items.bread.amount.max"));
    }

    @Test
    void shouldUpdateInheritedReferencesWhenRenaming() {
        LootTableRegistry registry = new LootTableRegistry();
        registry.createTable("starter");
        LootTable child = registry.createTable("dungeon");
        child.setParentName("starter");

        registry.renameTable("starter", "base");

        assertNull(registry.getTable("starter"));
        assertEquals("base", registry.getTable("dungeon").getParentName());
        registry.validateTables();
    }

    @Test
    void shouldRejectMissingParentsAndCircularReferences() {
        LootTable a = new LootTable("a");
        a.setParentName("missing");
        Map<String, LootTable> tables = new LinkedHashMap<String, LootTable>();
        tables.put("a", a);
        assertThrows(IllegalStateException.class, () -> LootTable.validateInheritanceGraph(tables));

        LootTable b = new LootTable("b");
        a.setParentName("b");
        b.setParentName("a");
        tables.put("b", b);
        assertThrows(IllegalStateException.class, () -> LootTable.validateInheritanceGraph(tables));
    }
}
