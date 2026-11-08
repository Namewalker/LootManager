package com.lootmanager.loot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LootGeneratorTest {

    @Test
    void shouldHonorTableAndItemRollsAndAmountBounds() {
        LootTable table = new LootTable("test");
        table.setMinRolls(2);
        table.setMaxRolls(2);
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("material", "GOLD_INGOT");
        entry.put("chance", 1.0D);
        entry.put("weight", 10);
        entry.put("min-rolls", 2);
        entry.put("max-rolls", 2);
        entry.put("amount-min", 3);
        entry.put("amount-max", 3);
        table.addItem("gold", entry);

        Map<String, LootTable> tables = new LinkedHashMap<String, LootTable>();
        tables.put("test", table);
        LootGenerator generator = new LootGenerator(new Random(5L));
        java.util.List<ItemStack> generated = generator.generate(table, tables);

        assertEquals(4, generated.size());
        for (ItemStack item : generated) {
            assertEquals(Material.GOLD_INGOT, item.getType());
            assertEquals(3, item.getAmount());
        }
    }

    @Test
    void shouldSkipEntriesWithZeroChance() {
        LootTable table = new LootTable("test");
        table.setMinRolls(1);
        table.setMaxRolls(1);
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("material", "DIAMOND");
        entry.put("chance", 0.0D);
        entry.put("weight", 100);
        table.addItem("diamond", entry);

        Map<String, LootTable> tables = new LinkedHashMap<String, LootTable>();
        tables.put("test", table);
        assertTrue(new LootGenerator(new Random(5L)).generate(table, tables).isEmpty());
    }
}
