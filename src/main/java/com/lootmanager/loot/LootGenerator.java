package com.lootmanager.loot;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LootGenerator {

    private final Random random;

    public LootGenerator() {
        this(new Random());
    }

    public LootGenerator(Random random) {
        this.random = random;
    }

    public List<ItemStack> generate(LootTable table, Map<String, LootTable> allTables) {
        List<ItemStack> generated = new ArrayList<ItemStack>();
        Map<String, Map<String, Object>> entries = table.resolveItems(allTables);
        if (entries.isEmpty()) {
            return generated;
        }

        int rolls = table.getMinRolls();
        if (table.getMaxRolls() > table.getMinRolls()) {
            rolls += random.nextInt(table.getMaxRolls() - table.getMinRolls() + 1);
        }

        for (int roll = 0; roll < rolls; roll++) {
            Map<String, Object> selected = selectEntry(entries);
            if (selected == null) {
                continue;
            }
            int minimumItemRolls = Math.max(0, readInt(selected.get("min-rolls"), 1));
            int maximumItemRolls = Math.max(minimumItemRolls, readInt(selected.get("max-rolls"), minimumItemRolls));
            maximumItemRolls = Math.min(maximumItemRolls, 64);
            int itemRolls = minimumItemRolls + (maximumItemRolls > minimumItemRolls
                    ? random.nextInt(maximumItemRolls - minimumItemRolls + 1) : 0);
            for (int itemRoll = 0; itemRoll < itemRolls; itemRoll++) {
                ItemStack item = createItem(selected);
                if (item != null) {
                    generated.add(item);
                }
            }
        }
        return generated;
    }

    private Map<String, Object> selectEntry(Map<String, Map<String, Object>> entries) {
        List<Map<String, Object>> eligible = new ArrayList<Map<String, Object>>();
        long totalWeight = 0L;
        for (Map<String, Object> entry : entries.values()) {
            double chance = readDouble(entry.get("chance"), 1.0D);
            if (chance > 1.0D) {
                chance /= 100.0D;
            }
            chance = Math.max(0.0D, Math.min(1.0D, chance));
            if (random.nextDouble() > chance) {
                continue;
            }
            int weight = readInt(entry.get("weight"), 1);
            if (weight <= 0) {
                continue;
            }
            eligible.add(entry);
            totalWeight += weight;
        }
        if (totalWeight <= 0L) {
            return null;
        }

        long choice = (long) (random.nextDouble() * totalWeight);
        for (Map<String, Object> entry : eligible) {
            choice -= readInt(entry.get("weight"), 1);
            if (choice < 0L) {
                return entry;
            }
        }
        return eligible.get(eligible.size() - 1);
    }

    private ItemStack createItem(Map<String, Object> data) {
        Object serialized = data.get("item-stack");
        if (serialized instanceof ItemStack) {
            ItemStack item = new ItemStack((ItemStack) serialized);
            int min = readInt(data.containsKey("amount-min") ? data.get("amount-min") : nested(data, "amount", "min"), item.getAmount());
            int max = readInt(data.containsKey("amount-max") ? data.get("amount-max") : nested(data, "amount", "max"), min);
            if (max < min) {
                int swap = min;
                min = max;
                max = swap;
            }
            max = Math.min(Math.max(1, max), item.getType().getMaxStackSize());
            min = Math.min(Math.max(1, min), max);
            int amount = min + (max > min ? random.nextInt(max - min + 1) : 0);
            item.setAmount(amount);
            return item;
        }
        String materialName = String.valueOf(data.get("material"));
        Material material = Material.matchMaterial(materialName);
        if (material == null || material == Material.AIR) {
            return null;
        }

        int min = readInt(data.containsKey("amount-min") ? data.get("amount-min") : nested(data, "amount", "min"), 1);
        int max = readInt(data.containsKey("amount-max") ? data.get("amount-max") : nested(data, "amount", "max"), min);
        if (max < min) {
            int swap = min;
            min = max;
            max = swap;
        }
        max = Math.min(Math.max(1, max), material.getMaxStackSize());
        min = Math.min(Math.max(1, min), max);
        int amount = min + (max > min ? random.nextInt(max - min + 1) : 0);
        ItemStack item = new ItemStack(material, amount);

        Object lore = data.get("lore");
        Object enchantments = data.get("enchants");
        boolean hasCustomMeta = data.containsKey("name") || lore instanceof List || enchantments instanceof Map;
        ItemMeta meta = hasCustomMeta ? item.getItemMeta() : null;
        if (meta != null) {
            Object displayName = data.get("name");
            if (displayName != null) {
                meta.setDisplayName(String.valueOf(displayName).replace('&', '\u00a7'));
            }
            if (lore instanceof List) {
                List<String> lines = new ArrayList<String>();
                for (Object line : (List<?>) lore) {
                    lines.add(String.valueOf(line).replace('&', '\u00a7'));
                }
                meta.setLore(lines);
            }
            if (enchantments instanceof Map) {
                for (Map.Entry<?, ?> enchantment : ((Map<?, ?>) enchantments).entrySet()) {
                    Enchantment type = Enchantment.getByName(String.valueOf(enchantment.getKey()).toUpperCase(java.util.Locale.ROOT));
                    if (type != null) {
                        meta.addEnchant(type, readInt(enchantment.getValue(), 1), true);
                    }
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Object nested(Map<String, Object> data, String parent, String child) {
        Object value = data.get(parent);
        return value instanceof Map ? ((Map<?, ?>) value).get(child) : null;
    }

    private int readInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double readDouble(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}