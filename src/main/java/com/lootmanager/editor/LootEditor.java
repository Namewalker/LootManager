package com.lootmanager.editor;

import com.lootmanager.LootManagerPlugin;
import com.lootmanager.loot.LootTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LootEditor implements Listener {

    private static final String MAIN_PREFIX = ChatColor.DARK_GREEN + "Loot: ";
    private static final String SETTINGS_PREFIX = ChatColor.DARK_BLUE + "Item: ";
    private final LootManagerPlugin plugin;

    public LootEditor(LootManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public void openTable(Player player, String tableName, int page) {
        LootTable table = plugin.getTableRegistry().getTable(tableName);
        if (table == null) {
            player.sendMessage(ChatColor.RED + "Unknown loot table: " + tableName);
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, MAIN_PREFIX + tableName + " §8[" + page + "]");
        List<String> keys = new ArrayList<String>(table.getItems().keySet());
        int start = Math.max(0, page) * 44;
        for (int index = start; index < Math.min(keys.size(), start + 44); index++) {
            String key = keys.get(index);
            Map<String, Object> data = table.getItem(key);
            ItemStack icon = iconFor(data);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
                lore.add(ChatColor.GRAY + "Key: " + key);
                lore.add(ChatColor.YELLOW + "Click to edit settings");
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inventory.setItem(index - start, icon);
        }
        inventory.setItem(45, button(Material.ARROW, ChatColor.YELLOW + "Previous page"));
        inventory.setItem(46, button(Material.REDSTONE, ChatColor.RED + "Minimum rolls - 1"));
        inventory.setItem(47, button(Material.EMERALD, ChatColor.GREEN + "Minimum rolls + 1"));
        inventory.setItem(48, button(Material.PAPER, ChatColor.YELLOW + "Minimum rolls: " + table.getMinRolls()));
        inventory.setItem(49, button(Material.HOPPER, ChatColor.GREEN + "Add held item"
            + ChatColor.GRAY + " | hold the item in your main hand"));
        inventory.setItem(50, button(Material.PAPER, ChatColor.YELLOW + "Maximum rolls: " + table.getMaxRolls()));
        inventory.setItem(51, button(Material.REDSTONE, ChatColor.RED + "Maximum rolls - 1"));
        inventory.setItem(52, button(Material.EMERALD, ChatColor.GREEN + "Maximum rolls + 1"));
        inventory.setItem(53, button(Material.BARRIER, ChatColor.RED + "Close"));
        if (page > 0) {
            inventory.setItem(45, button(Material.ARROW, ChatColor.YELLOW + "Previous page"));
        }
        if (keys.size() > start + 44) {
            inventory.setItem(44, button(Material.ARROW, ChatColor.YELLOW + "Next page"));
        }
        player.openInventory(inventory);
    }

    private void openSettings(Player player, String tableName, String itemKey) {
        LootTable table = plugin.getTableRegistry().getTable(tableName);
        Map<String, Object> data = table == null ? null : table.getItem(itemKey);
        if (data == null) {
            openTable(player, tableName, 0);
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 36, SETTINGS_PREFIX + tableName + ":" + itemKey);
        inventory.setItem(4, iconFor(data));
        inventory.setItem(10, button(Material.REDSTONE, ChatColor.RED + "Chance - 5%"));
        inventory.setItem(11, button(Material.PAPER, ChatColor.YELLOW + "Chance: " + percent(data.get("chance"))));
        inventory.setItem(12, button(Material.EMERALD, ChatColor.GREEN + "Chance + 5%"));
        inventory.setItem(13, button(Material.PAPER, ChatColor.YELLOW + "Weight: " + integer(data.get("weight"), 1)));
        inventory.setItem(14, button(Material.REDSTONE, ChatColor.RED + "Weight - 1"));
        inventory.setItem(15, button(Material.EMERALD, ChatColor.GREEN + "Weight + 1"));
        inventory.setItem(16, button(Material.PAPER, ChatColor.YELLOW + "Min amount: " + amount(data, "min", "amount-min", 1)));
        inventory.setItem(18, button(Material.REDSTONE, ChatColor.RED + "Min amount - 1"));
        inventory.setItem(19, button(Material.EMERALD, ChatColor.GREEN + "Min amount + 1"));
        inventory.setItem(20, button(Material.PAPER, ChatColor.YELLOW + "Max amount: " + amount(data, "max", "amount-max", 1)));
        inventory.setItem(22, button(Material.REDSTONE, ChatColor.RED + "Max amount - 1"));
        inventory.setItem(23, button(Material.EMERALD, ChatColor.GREEN + "Max amount + 1"));
        inventory.setItem(26, button(Material.BARRIER, ChatColor.RED + "Remove item"));
        inventory.setItem(28, button(Material.REDSTONE, ChatColor.RED + "Minimum rolls - 1"));
        inventory.setItem(29, button(Material.PAPER, ChatColor.YELLOW + "Minimum rolls: " + integer(data.get("min-rolls"), 1)));
        inventory.setItem(30, button(Material.EMERALD, ChatColor.GREEN + "Minimum rolls + 1"));
        inventory.setItem(31, button(Material.REDSTONE, ChatColor.RED + "Maximum rolls - 1"));
        inventory.setItem(32, button(Material.PAPER, ChatColor.YELLOW + "Maximum rolls: " + integer(data.get("max-rolls"), 1)));
        inventory.setItem(33, button(Material.EMERALD, ChatColor.GREEN + "Maximum rolls + 1"));
        inventory.setItem(35, button(Material.ARROW, ChatColor.YELLOW + "Back to table"));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(MAIN_PREFIX) && !title.startsWith(SETTINGS_PREFIX)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (title.startsWith(MAIN_PREFIX)) {
            String pageAndTable = title.substring(MAIN_PREFIX.length());
            int marker = pageAndTable.lastIndexOf(" §8[");
            int page = 0;
            String tableName = pageAndTable;
            if (marker >= 0 && pageAndTable.endsWith("]")) {
                tableName = pageAndTable.substring(0, marker);
                try {
                    page = Integer.parseInt(pageAndTable.substring(marker + 4, pageAndTable.length() - 1));
                } catch (NumberFormatException ignored) {
                    page = 0;
                }
            }
            handleMainClick(player, tableName, page, event);
        } else {
            handleSettingsClick(player, title.substring(SETTINGS_PREFIX.length()), event.getRawSlot());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith(MAIN_PREFIX) || title.startsWith(SETTINGS_PREFIX)) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void handleMainClick(Player player, String tableName, int page, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        LootTable table = plugin.getTableRegistry().getTable(tableName);
        if (table == null) {
            player.closeInventory();
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        if (slot == 49) {
            ItemStack held = event.getCursor();
            boolean fromCursor = held != null && held.getType() != Material.AIR;
            if (!fromCursor) {
                held = player.getInventory().getItemInMainHand();
            }
            if (held == null || held.getType() == Material.AIR) {
                player.sendMessage(ChatColor.YELLOW + "Hold the item to add in your main hand, then click Add held item.");
                return;
            }
            held = new ItemStack(held);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("material", held.getType().name());
            data.put("item-stack", held);
            data.put("chance", suggestedChance(held.getType()));
            data.put("weight", suggestedWeight(held.getType()));
            data.put("amount-min", 1);
            data.put("amount-max", Math.max(1, held.getAmount()));
            data.put("min-rolls", 1);
            data.put("max-rolls", 1);
            String key = held.getType().name().toLowerCase(java.util.Locale.ROOT) + "_" + (table.getItems().size() + 1);
            while (table.getItems().containsKey(key)) {
                key = key + "_";
            }
            table.addItem(key, data);
            if (fromCursor) {
                event.setCursor(null);
            }
            plugin.saveLootData();
            player.sendMessage(ChatColor.GREEN + "Added " + held.getType().name() + " to " + tableName + ".");
            openTable(player, tableName, 0);
            return;
        }
        if (slot == 45) {
            openTable(player, tableName, Math.max(0, page - 1));
            return;
        }
        if (slot == 44) {
            if (table.getItems().size() > (page + 1) * 44) {
                openTable(player, tableName, page + 1);
            }
            return;
        }
        if (slot == 46) {
            table.setMinRolls(Math.max(0, table.getMinRolls() - 1));
        } else if (slot == 47) {
            table.setMinRolls(Math.min(100, table.getMinRolls() + 1));
        } else if (slot == 51) {
            table.setMaxRolls(Math.max(0, table.getMaxRolls() - 1));
        } else if (slot == 52) {
            table.setMaxRolls(Math.min(100, table.getMaxRolls() + 1));
        } else {
            if (slot >= 0 && slot < 45 && event.getCurrentItem() != null) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasLore()) {
                    for (String line : meta.getLore()) {
                        if (line.startsWith(ChatColor.GRAY + "Key: ")) {
                            openSettings(player, tableName, ChatColor.stripColor(line.substring((ChatColor.GRAY + "Key: ").length())));
                            return;
                        }
                    }
                }
            }
            return;
        }
        plugin.saveLootData();
        openTable(player, tableName, page);
    }

    private void handleSettingsClick(Player player, String keyPath, int slot) {
        int separator = keyPath.indexOf(':');
        if (separator < 0) {
            player.closeInventory();
            return;
        }
        String tableName = keyPath.substring(0, separator);
        String key = keyPath.substring(separator + 1);
        LootTable table = plugin.getTableRegistry().getTable(tableName);
        Map<String, Object> data = table == null ? null : table.getItem(key);
        if (slot == 35) {
            openTable(player, tableName, 0);
            return;
        }
        if (data == null) {
            openTable(player, tableName, 0);
            return;
        }
        if (slot == 26) {
            table.removeItem(key);
            plugin.saveLootData();
            player.sendMessage(ChatColor.GREEN + "Removed " + key + " from " + tableName + ".");
            openTable(player, tableName, 0);
            return;
        }
        if (slot == 10 || slot == 12) {
            double chance = decimal(data.get("chance"), 1.0D) + (slot == 12 ? 0.05D : -0.05D);
            if (chance > 1.0D && decimal(data.get("chance"), 1.0D) > 1.0D) {
                chance = decimal(data.get("chance"), 1.0D) / 100.0D + (slot == 12 ? 0.05D : -0.05D);
            }
            data.put("chance", Math.max(0.0D, Math.min(1.0D, chance)));
        } else if (slot == 14 || slot == 15) {
            data.put("weight", Math.max(1, integer(data.get("weight"), 1) + (slot == 15 ? 1 : -1)));
        } else if (slot == 18 || slot == 19) {
            int min = Math.min(maxStack(data), Math.max(1, amount(data, "min", "amount-min", 1) + (slot == 19 ? 1 : -1)));
            data.put("amount-min", min);
            data.put("amount-max", Math.max(min, amount(data, "max", "amount-max", min)));
        } else if (slot == 22 || slot == 23) {
            int max = Math.min(maxStack(data), Math.max(1, amount(data, "max", "amount-max", 1) + (slot == 23 ? 1 : -1)));
            data.put("amount-max", max);
            data.put("amount-min", Math.min(max, amount(data, "min", "amount-min", 1)));
        } else if (slot == 28 || slot == 30) {
            int minimumRolls = Math.min(64, Math.max(0, integer(data.get("min-rolls"), 1) + (slot == 30 ? 1 : -1)));
            data.put("min-rolls", minimumRolls);
            data.put("max-rolls", Math.max(minimumRolls, integer(data.get("max-rolls"), minimumRolls)));
        } else if (slot == 31 || slot == 33) {
            int maximumRolls = Math.min(64, Math.max(0, integer(data.get("max-rolls"), 1) + (slot == 33 ? 1 : -1)));
            data.put("max-rolls", maximumRolls);
            data.put("min-rolls", Math.min(maximumRolls, integer(data.get("min-rolls"), 1)));
        } else {
            return;
        }
        table.addItem(key, data);
        plugin.saveLootData();
        openSettings(player, tableName, key);
    }

    private ItemStack iconFor(Map<String, Object> data) {
        Object stored = data.get("item-stack");
        if (stored instanceof ItemStack) {
            return ((ItemStack) stored).clone();
        }
        Material material = Material.matchMaterial(String.valueOf(data.get("material")));
        if (material == null || material == Material.AIR) {
            material = Material.BARRIER;
        }
        return new ItemStack(material);
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private int suggestedWeight(Material material) {
        String name = material.name();
        if (name.contains("DIAMOND") || name.contains("NETHER_STAR") || name.contains("GOLDEN_APPLE")) {
            return 5;
        }
        if (name.contains("GOLD") || name.contains("IRON") || name.contains("EMERALD")) {
            return 20;
        }
        return 100;
    }

    private double suggestedChance(Material material) {
        String name = material.name();
        if (name.contains("DIAMOND") || name.contains("NETHER_STAR")) {
            return 0.10D;
        }
        if (name.contains("GOLD") || name.contains("EMERALD")) {
            return 0.25D;
        }
        return 0.75D;
    }

    private int amount(Map<String, Object> data, String child, String legacy, int fallback) {
        Object flat = data.get(legacy);
        if (flat != null) {
            return integer(flat, fallback);
        }
        Object nested = data.get("amount");
        return nested instanceof Map ? integer(((Map<?, ?>) nested).get(child), fallback) : fallback;
    }

    private int maxStack(Map<String, Object> data) {
        Object stored = data.get("item-stack");
        if (stored instanceof ItemStack) {
            return ((ItemStack) stored).getType().getMaxStackSize();
        }
        Material material = Material.matchMaterial(String.valueOf(data.get("material")));
        return material == null || material == Material.AIR ? 64 : material.getMaxStackSize();
    }

    private int integer(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private double decimal(Object value, double fallback) {
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private String percent(Object value) {
        double chance = decimal(value, 1.0D);
        return Math.round(chance > 1.0D ? chance : chance * 100.0D) + "%";
    }
}
