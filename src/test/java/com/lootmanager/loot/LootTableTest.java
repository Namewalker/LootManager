package com.lootmanager.loot;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LootTableTest {

    @Test
    void shouldMergeInheritedItems() {
        LootTable starter = new LootTable("starter");
        starter.addItem("bread", createItem(1.0, 10, 1, 2));
        starter.addItem("coal", createItem(0.5, 20, 1, 3));

        LootTable dungeon = new LootTable("dungeon");
        dungeon.setParentName("starter");
        dungeon.addItem("gold", createItem(0.25, 8, 1, 1));
        dungeon.addItem("diamond", createItem(0.1, 3, 1, 1));

        Map<String, LootTable> tables = new LinkedHashMap<>();
        tables.put("starter", starter);
        tables.put("dungeon", dungeon);

        Map<String, Map<String, Object>> merged = dungeon.resolveItems(tables);

        assertTrue(merged.containsKey("bread"));
        assertTrue(merged.containsKey("coal"));
        assertTrue(merged.containsKey("gold"));
        assertTrue(merged.containsKey("diamond"));
        assertEquals(4, merged.size());
    }

    @Test
    void shouldRejectCircularInheritance() {
        LootTable a = new LootTable("A");
        LootTable b = new LootTable("B");
        a.setParentName("B");
        b.setParentName("A");

        Map<String, LootTable> tables = new LinkedHashMap<>();
        tables.put("A", a);
        tables.put("B", b);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> LootTable.validateInheritanceGraph(tables));

        assertTrue(exception.getMessage().contains("Circular inheritance"));
    }

    private Map<String, Object> createItem(double chance, int weight, int minAmount, int maxAmount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("chance", chance);
        item.put("weight", weight);
        item.put("amount-min", minAmount);
        item.put("amount-max", maxAmount);
        return item;
    }
}
