package com.lootmanager.command;

import com.lootmanager.LootManagerPlugin;
import com.lootmanager.loot.LootTable;
import com.lootmanager.region.LootRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LootCommandExecutor implements CommandExecutor {

    private final LootManagerPlugin plugin;

    public LootCommandExecutor(LootManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if ("table".equals(action)) {
            return handleTable(sender, args);
        }
        if ("region".equals(action)) {
            return handleRegion(sender, args);
        }
        if ("registry".equals(action)) {
            return handleRegistry(sender, args);
        }
        if ("wand".equals(action)) {
            return giveWand(sender);
        }
        if ("pos1".equals(action) || "pos2".equals(action)) {
            return setPosition(sender, "pos1".equals(action) ? 1 : 2);
        }
        if ("clearselection".equals(action)) {
            if (!(sender instanceof Player)) {
                return playerOnly(sender);
            }
            if (!sender.hasPermission("loot.wand")) {
                return noPermission(sender);
            }
            plugin.getSelectionManager().clear(((Player) sender).getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Selection cleared.");
            return true;
        }
        if ("showselection".equals(action) || "info".equals(action)) {
            return showSelection(sender);
        }
        if ("fillworld".equals(action)) {
            return fillWorld(sender, args);
        }
        if ("fill".equals(action)) {
            return fillLoaded(sender, args);
        }
        if ("fillselection".equals(action)) {
            return fillSelection(sender, args);
        }
        if ("edit".equals(action)) {
            return editTable(sender, args);
        }
        if ("preview".equals(action)) {
            return preview(sender, args);
        }
        if ("refill".equals(action)) {
            return refill(sender, args);
        }
        if ("progress".equals(action)) {
            if (!sender.hasPermission("loot.fill")) {
                return noPermission(sender);
            }
            sender.sendMessage(ChatColor.GOLD + "Loot Scan Progress: " + ChatColor.YELLOW + plugin.getScanManager().getProgress());
            return true;
        }
        if ("pause".equals(action)) {
            if (!sender.hasPermission("loot.fill")) {
                return noPermission(sender);
            }
            if (!plugin.getScanManager().isActive()) {
                sender.sendMessage(ChatColor.YELLOW + "No scan is active.");
                return true;
            }
            plugin.getScanManager().pause();
            sender.sendMessage(ChatColor.YELLOW + "Scan paused.");
            return true;
        }
        if ("resume".equals(action)) {
            if (!sender.hasPermission("loot.fill")) {
                return noPermission(sender);
            }
            if (!plugin.getScanManager().isActive()) {
                sender.sendMessage(ChatColor.YELLOW + "No scan is active.");
                return true;
            }
            plugin.getScanManager().resume();
            sender.sendMessage(ChatColor.GREEN + "Scan resumed.");
            return true;
        }
        if ("cancel".equals(action)) {
            if (!sender.hasPermission("loot.fill")) {
                return noPermission(sender);
            }
            if (!plugin.getScanManager().isActive()) {
                sender.sendMessage(ChatColor.YELLOW + "No scan is active.");
                return true;
            }
            plugin.getScanManager().cancel();
            sender.sendMessage(ChatColor.YELLOW + "Scan cancellation requested.");
            return true;
        }
        if ("stats".equals(action)) {
            if (!sender.hasPermission("loot.stats")) {
                return noPermission(sender);
            }
            sender.sendMessage(ChatColor.GOLD + "LootManager Stats: " + ChatColor.YELLOW + plugin.getScanManager().getStatistics());
            return true;
        }
        if ("reload".equals(action)) {
            if (!sender.hasPermission("loot.reload")) {
                return noPermission(sender);
            }
            try {
                plugin.reloadPluginData();
                sender.sendMessage(ChatColor.GREEN + "LootManager configuration and data reloaded.");
            } catch (IllegalStateException exception) {
                sender.sendMessage(ChatColor.RED + "Reload rejected: " + exception.getMessage());
            }
            return true;
        }
        sendHelp(sender);
        return true;
    }

    private boolean handleTable(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.table")) {
            return noPermission(sender);
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Table: create, delete, rename, list, info, inherit, clearinheritance, clear");
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        try {
            if ("create".equals(sub) && args.length == 3) {
                LootTable table = plugin.getTableRegistry().createTable(args[2]);
                plugin.saveLootData();
                sender.sendMessage(ChatColor.GREEN + "Created empty loot table " + table.getName() + ".");
            } else if ("delete".equals(sub) && args.length == 3) {
                if (plugin.getRegionManager().usesTable(args[2])) {
                    throw new IllegalStateException("A region uses this table. Change or remove the region first.");
                }
                plugin.getTableRegistry().deleteTable(args[2]);
                plugin.saveLootData();
                sender.sendMessage(ChatColor.GREEN + "Deleted loot table " + args[2] + ".");
            } else if ("rename".equals(sub) && args.length == 4) {
                plugin.getTableRegistry().renameTable(args[2], args[3]);
                plugin.tableRegistryValidate();
                plugin.getRegionManager().renameTableReferences(args[2], args[3]);
                plugin.saveRegionData();
                plugin.saveLootData();
                sender.sendMessage(ChatColor.GREEN + "Renamed loot table " + args[2] + " to " + args[3] + ".");
            } else if ("list".equals(sub) && args.length == 2) {
                sender.sendMessage(ChatColor.GOLD + "Loot tables: " + ChatColor.YELLOW + String.join(", ", plugin.getTableRegistry().listTables()));
            } else if ("info".equals(sub) && args.length == 3) {
                LootTable table = requireTable(args[2]);
                if (table != null) {
                    sender.sendMessage(ChatColor.GOLD + "Table " + table.getName() + ChatColor.YELLOW
                            + " | Parent: " + (table.getParentName() == null ? "none" : table.getParentName())
                            + " | Items: " + table.getItems().size() + " | Regions: " + countRegions(table.getName())
                            + " | Rolls: " + table.getMinRolls() + "-" + table.getMaxRolls()
                            + " | Last edited: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(table.getLastEdited())));
                }
            } else if ("inherit".equals(sub) && args.length == 4) {
                LootTable table = requireTable(args[2]);
                if (table == null) {
                    return true;
                }
                if (plugin.getTableRegistry().getTable(args[3]) == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown parent table " + args[3] + ".");
                    return true;
                }
                String previous = table.getParentName();
                table.setParentName(args[3]);
                try {
                    plugin.tableRegistryValidate();
                } catch (IllegalStateException exception) {
                    table.setParentName(previous);
                    throw exception;
                }
                plugin.saveLootData();
                sender.sendMessage(ChatColor.GREEN + "Table " + args[2] + " now inherits " + args[3] + ".");
            } else if ("clearinheritance".equals(sub) && args.length == 3) {
                LootTable table = requireTable(args[2]);
                if (table != null) {
                    table.clearParent();
                    plugin.saveLootData();
                    sender.sendMessage(ChatColor.GREEN + "Inheritance cleared for " + args[2] + ".");
                }
            } else if ("clear".equals(sub) && args.length == 3) {
                LootTable table = requireTable(args[2]);
                if (table != null) {
                    table.clearItems();
                    plugin.saveLootData();
                    sender.sendMessage(ChatColor.GREEN + "Cleared items from " + args[2] + ".");
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /loot table <create|delete|rename|list|info|inherit|clearinheritance|clear> ...");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean handleRegistry(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.fill")) {
            return noPermission(sender);
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /loot registry <rebuild|stats|validate> [world]");
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        try {
            if ("rebuild".equals(sub) && args.length == 3) {
                World world = Bukkit.getWorld(args[2]);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "World is not loaded: " + args[2]);
                    return true;
                }
                int count = rebuildContainerRegistry(world);
                sender.sendMessage(ChatColor.GREEN + "Rebuilt container registry for " + world.getName() + ". Found " + count + " tracked containers.");
            } else if ("stats".equals(sub) && args.length == 2) {
                int total = 0;
                for (World world : Bukkit.getWorlds()) {
                    total += plugin.getContainerRegistry().getContainerCount(world.getName());
                }
                sender.sendMessage(ChatColor.GOLD + "Container Registry Stats: " + ChatColor.YELLOW
                        + "worlds tracked=" + countBuiltWorlds() + ", total containers=" + total);
            } else if ("validate".equals(sub) && args.length == 2) {
                List<String> missing = new ArrayList<String>();
                for (World world : Bukkit.getWorlds()) {
                    if (!plugin.getContainerRegistry().isWorldBuilt(world.getName())) {
                        missing.add(world.getName());
                    }
                }
                if (missing.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "All loaded worlds have a valid container registry cache.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Registry cache missing for: " + String.join(", ", missing)
                            + ". Run /loot registry rebuild <world> once to build the per-world cache.");
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /loot registry <rebuild|stats|validate> [world]");
            }
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean handleRegion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.region")) {
            return noPermission(sender);
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Region: create, remove, list, info, enable, disable, settable, settime");
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        try {
            if ("create".equals(sub) && (args.length == 4 || args.length == 5)) {
                if (!(sender instanceof Player)) {
                    return playerOnly(sender);
                }
                Player player = (Player) sender;
                if (!plugin.getSelectionManager().hasCompleteSelection(player.getUniqueId())) {
                    sender.sendMessage(ChatColor.RED + "Set both selection positions with /loot wand or /loot pos1 and /loot pos2 first.");
                    return true;
                }
                LootTable table = requireTable(args[3]);
                if (table == null) {
                    return true;
                }
                int minutes = args.length == 5 ? Integer.parseInt(args[4]) : plugin.getConfig().getInt("regions.default-regen-minutes", 60);
                if (minutes < 0) {
                    throw new IllegalArgumentException("Regeneration minutes cannot be negative.");
                }
                Location first = plugin.getSelectionManager().getPosition(player.getUniqueId(), 1);
                Location second = plugin.getSelectionManager().getPosition(player.getUniqueId(), 2);
                LootRegion region = plugin.getRegionManager().createRegion(args[2], first.getWorld().getName(), args[3], minutes);
                region.setBounds(first.getBlockX(), first.getBlockY(), first.getBlockZ(), second.getBlockX(), second.getBlockY(), second.getBlockZ());
                plugin.saveRegionData();
                sender.sendMessage(ChatColor.GREEN + "Created region " + region.getName() + " in " + region.getWorldName() + " using " + table.getName() + ".");
            } else if ("remove".equals(sub) && args.length == 3) {
                plugin.getRegionManager().removeRegion(args[2]);
                plugin.saveRegionData();
                sender.sendMessage(ChatColor.GREEN + "Removed region " + args[2] + ".");
            } else if ("list".equals(sub) && args.length == 2) {
                sender.sendMessage(ChatColor.GOLD + "Regions: " + ChatColor.YELLOW + String.join(", ", plugin.getRegionManager().listRegions()));
            } else if ("info".equals(sub) && args.length == 3) {
                LootRegion region = requireRegion(args[2]);
                if (region != null) {
                    sender.sendMessage(ChatColor.GOLD + "Region " + region.getName() + ChatColor.YELLOW + " | World: " + region.getWorldName()
                            + " | Table: " + region.getTableName() + " | Regen: " + region.getRegenerationMinutes() + "m"
                            + " | Enabled: " + region.isEnabled() + " | Bounds: " + region.getMinX() + "," + region.getMinY() + "," + region.getMinZ()
                            + " to " + region.getMaxX() + "," + region.getMaxY() + "," + region.getMaxZ());
                }
            } else if (("enable".equals(sub) || "disable".equals(sub)) && args.length == 3) {
                LootRegion region = requireRegion(args[2]);
                if (region != null) {
                    region.setEnabled("enable".equals(sub));
                    plugin.saveRegionData();
                    sender.sendMessage(ChatColor.GREEN + (region.isEnabled() ? "Enabled " : "Disabled ") + "region " + region.getName() + ".");
                }
            } else if ("settable".equals(sub) && args.length == 4) {
                LootRegion region = requireRegion(args[2]);
                LootTable table = requireTable(args[3]);
                if (region != null && table != null) {
                    region.setTableName(table.getName());
                    plugin.saveRegionData();
                    sender.sendMessage(ChatColor.GREEN + "Region " + region.getName() + " now uses " + table.getName() + ".");
                }
            } else if ("settime".equals(sub) && args.length == 4) {
                LootRegion region = requireRegion(args[2]);
                int minutes = Integer.parseInt(args[3]);
                if (minutes < 0) {
                    throw new IllegalArgumentException("Regeneration minutes cannot be negative.");
                }
                if (region != null) {
                    region.setRegenerationMinutes(minutes);
                    plugin.saveRegionData();
                    sender.sendMessage(ChatColor.GREEN + "Region timer updated to " + minutes + " minutes.");
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /loot region <create|remove|list|info|enable|disable|settable|settime> ...");
            }
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Expected regeneration time as a whole number of minutes.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean fillWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.fill")) {
            return noPermission(sender);
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /loot fillworld <world> <table>");
            return true;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World is not loaded: " + args[1]);
            return true;
        }
        if (!hasTable(sender, args[2])) {
            return true;
        }
        WorldBorder border = world.getWorldBorder();
        double half = border.getSize() / 2.0D;
        Location center = border.getCenter();
        int minX = ((int) Math.floor(center.getX() - half)) >> 4;
        int maxX = ((int) Math.floor(center.getX() + half)) >> 4;
        int minZ = ((int) Math.floor(center.getZ() - half)) >> 4;
        int maxZ = ((int) Math.floor(center.getZ() + half)) >> 4;
        startScan(sender, world, args[2], minX, maxX, minZ, maxZ, false, null);
        return true;
    }

    private boolean fillLoaded(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.fill")) {
            return noPermission(sender);
        }
        if (args.length != 4 || !"loaded".equalsIgnoreCase(args[1])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /loot fill loaded <world> <table>");
            return true;
        }
        World world = Bukkit.getWorld(args[2]);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World is not loaded: " + args[2]);
            return true;
        }
        if (!hasTable(sender, args[3])) {
            return true;
        }
        ChunkBounds bounds = loadedBounds(world);
        if (bounds == null) {
            sender.sendMessage(ChatColor.YELLOW + "World " + world.getName() + " has no loaded chunks to scan.");
            return true;
        }
        startScan(sender, world, args[3], bounds.minX, bounds.maxX, bounds.minZ, bounds.maxZ, true, null);
        return true;
    }

    private boolean fillSelection(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.fill")) {
            return noPermission(sender);
        }
        if (!(sender instanceof Player)) {
            return playerOnly(sender);
        }
        if (args.length != 2 || !plugin.getSelectionManager().hasCompleteSelection(((Player) sender).getUniqueId())) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: select two positions, then /loot fillselection <table>");
            return true;
        }
        if (!hasTable(sender, args[1])) {
            return true;
        }
        Player player = (Player) sender;
        Location first = plugin.getSelectionManager().getPosition(player.getUniqueId(), 1);
        Location second = plugin.getSelectionManager().getPosition(player.getUniqueId(), 2);
        startScan(sender, first.getWorld(), args[1], first.getBlockX() >> 4, second.getBlockX() >> 4,
                first.getBlockZ() >> 4, second.getBlockZ() >> 4, false, null);
        return true;
    }

    private void startScan(CommandSender sender, World world, String table, int x1, int x2, int z1, int z2, boolean loadedOnly, LootRegion region) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        if (!plugin.getContainerRegistry().isWorldBuilt(world.getName())) {
            sender.sendMessage(ChatColor.RED + "Container registry for world " + world.getName() + " is not built. Run /loot registry rebuild " + world.getName() + " once to build the world cache.");
            return;
        }
        try {
            if (!plugin.getScanManager().start(world, table, minX, maxX, minZ, maxZ, loadedOnly, region)) {
                sender.sendMessage(ChatColor.RED + "Another scan is already active. Use /loot progress, /loot pause, or /loot cancel.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Queued scan for world " + world.getName() + " using table " + table + ".");
            }
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
    }

    private boolean setPosition(CommandSender sender, int position) {
        if (!(sender instanceof Player)) {
            return playerOnly(sender);
        }
        if (!sender.hasPermission("loot.wand")) {
            return noPermission(sender);
        }
        Player player = (Player) sender;
        Block block = player.getLocation().getBlock();
        try {
            plugin.getSelectionManager().setPosition(player.getUniqueId(), position, block.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Position " + position + " set to " + block.getX() + ", " + block.getY() + ", " + block.getZ() + " in " + block.getWorld().getName() + ".");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean showSelection(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return playerOnly(sender);
        }
        if (!sender.hasPermission("loot.wand")) {
            return noPermission(sender);
        }
        Player player = (Player) sender;
        Location first = plugin.getSelectionManager().getPosition(player.getUniqueId(), 1);
        Location second = plugin.getSelectionManager().getPosition(player.getUniqueId(), 2);
        if (first == null && second == null) {
            sender.sendMessage(ChatColor.YELLOW + "No selection. Use /loot wand and left/right click blocks, or /loot pos1 and /loot pos2.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Selection in " + plugin.getSelectionManager().getWorldName(player.getUniqueId())
                + ChatColor.YELLOW + " | pos1: " + format(first) + " | pos2: " + format(second));
        return true;
    }

    private boolean giveWand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return playerOnly(sender);
        }
        if (!sender.hasPermission("loot.wand")) {
            return noPermission(sender);
        }
        Player player = (Player) sender;
        Material material = Material.matchMaterial(plugin.getConfig().getString("wand.material", "GOLD_HOE"));
        if (material == null || material == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Invalid wand.material in config.yml.");
            return true;
        }
        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("wand.name", "&6Loot Selection Wand")));
            wand.setItemMeta(meta);
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(wand);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        sender.sendMessage(ChatColor.GREEN + "Loot selection wand given. Left-click for position 1, right-click for position 2.");
        return true;
    }

    private boolean editTable(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return playerOnly(sender);
        }
        if (!sender.hasPermission("loot.edit")) {
            return noPermission(sender);
        }
        if (args.length != 2 || !hasTable(sender, args[1])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /loot edit <table>");
            return true;
        }
        plugin.openLootEditor((Player) sender, args[1]);
        return true;
    }

    private boolean preview(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.preview")) {
            return noPermission(sender);
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /loot preview <table>");
            return true;
        }
        LootTable table = plugin.getTableRegistry().getTable(args[1]);
        if (table == null) {
            sender.sendMessage(ChatColor.RED + "Unknown loot table: " + args[1]);
            return true;
        }
        try {
            Map<String, Map<String, Object>> items = table.resolveItems(plugin.getTableRegistry().getTables());
            sender.sendMessage(ChatColor.GOLD + "Possible loot for " + table.getName() + ": " + ChatColor.YELLOW + String.join(", ", items.keySet()));
        } catch (IllegalStateException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean refill(CommandSender sender, String[] args) {
        if (!sender.hasPermission("loot.refill")) {
            return noPermission(sender);
        }
        if (args.length == 3 && "region".equalsIgnoreCase(args[1])) {
            LootRegion region = plugin.getRegionManager().getRegion(args[2]);
            if (region == null) {
                sender.sendMessage(ChatColor.RED + "Unknown region: " + args[2]);
                return true;
            }
            World world = Bukkit.getWorld(region.getWorldName());
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "Region world is not loaded: " + region.getWorldName());
                return true;
            }
            startScan(sender, world, region.getTableName(), region.getMinX() >> 4, region.getMaxX() >> 4,
                    region.getMinZ() >> 4, region.getMaxZ() >> 4, false, region);
            return true;
        }
        if (args.length == 1) {
            int count = plugin.getScanManager().startAllRegionRefills();
            if (count == 0) {
                sender.sendMessage(ChatColor.YELLOW + "There are no enabled regions to refill.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Queued refills for " + count + " enabled regions. Only tracked, eligible containers will be refilled.");
            }
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: /loot refill [region <region>]");
        return true;
    }

    private ChunkBounds loadedBounds(World world) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            minX = Math.min(minX, chunk.getX());
            maxX = Math.max(maxX, chunk.getX());
            minZ = Math.min(minZ, chunk.getZ());
            maxZ = Math.max(maxZ, chunk.getZ());
        }
        return minX == Integer.MAX_VALUE ? null : new ChunkBounds(minX, maxX, minZ, maxZ);
    }

    private LootTable requireTable(String name) {
        LootTable table = plugin.getTableRegistry().getTable(name);
        if (table == null) {
            throw new IllegalArgumentException("Unknown loot table: " + name);
        }
        return table;
    }

    private boolean hasTable(CommandSender sender, String name) {
        if (plugin.getTableRegistry().getTable(name) == null) {
            sender.sendMessage(ChatColor.RED + "Unknown loot table: " + name);
            return false;
        }
        return true;
    }

    private LootRegion requireRegion(String name) {
        LootRegion region = plugin.getRegionManager().getRegion(name);
        if (region == null) {
            throw new IllegalArgumentException("Unknown region: " + name);
        }
        return region;
    }

    private int countRegions(String tableName) {
        int count = 0;
        for (LootRegion region : plugin.getRegionManager().getRegions()) {
            if (tableName.equals(region.getTableName())) {
                count++;
            }
        }
        return count;
    }

    private int rebuildContainerRegistry(World world) {
        plugin.getContainerRegistry().clearWorld(world.getName());
        int count = 0;
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            for (BlockState state : chunk.getTileEntities()) {
                if (!(state instanceof InventoryHolder)) {
                    continue;
                }
                if (!isTrackedContainer(state)) {
                    continue;
                }
                Block block = state.getBlock();
                if (block == null) {
                    continue;
                }
                plugin.getContainerRegistry().addContainer(world.getName(), block.getX(), block.getY(), block.getZ(), block.getType().name());
                count++;
            }
        }
        plugin.getContainerRegistry().markWorldBuilt(world.getName(), true);
        plugin.saveContainerData();
        return count;
    }

    private int countBuiltWorlds() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            if (plugin.getContainerRegistry().isWorldBuilt(world.getName())) {
                count++;
            }
        }
        return count;
    }

    private boolean isTrackedContainer(BlockState state) {
        if (state instanceof Chest) {
            return true;
        }
        if (state instanceof ShulkerBox) {
            return plugin.getConfig().getBoolean("containers.shulker-boxes", false);
        }
        return state instanceof InventoryHolder && plugin.getConfig().getBoolean("containers.any-inventory-holder", false);
    }

    private String format(Location location) {
        return location == null ? "unset" : location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private boolean playerOnly(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
        return true;
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "LootManager 1.1.0 commands:");
        sender.sendMessage(ChatColor.YELLOW + "/loot table <create|delete|rename|list|info|inherit|clearinheritance|clear> ...");
        sender.sendMessage(ChatColor.YELLOW + "/loot edit|preview <table> | /loot fillworld <world> <table>");
        sender.sendMessage(ChatColor.YELLOW + "/loot fill loaded <world> <table> | /loot fillselection <table>");
        sender.sendMessage(ChatColor.YELLOW + "/loot region <create|remove|list|info|enable|disable|settable|settime> ...");
        sender.sendMessage(ChatColor.YELLOW + "/loot registry <rebuild|stats|validate> [world]");
        sender.sendMessage(ChatColor.YELLOW + "/loot wand|pos1|pos2|clearselection|showselection|info");
        sender.sendMessage(ChatColor.YELLOW + "/loot progress|pause|resume|cancel|refill|stats|reload");
    }

    private static class ChunkBounds {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        private ChunkBounds(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }
}
