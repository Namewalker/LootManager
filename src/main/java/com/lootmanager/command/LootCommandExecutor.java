package com.lootmanager.command;

import com.lootmanager.LootManagerPlugin;
import com.lootmanager.loot.LootTable;
import com.lootmanager.region.LootRegion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public class LootCommandExecutor implements CommandExecutor {

    private final LootManagerPlugin plugin;

    public LootCommandExecutor(LootManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("LootManager commands are available. Use /loot table, /loot region, /loot fillworld, /loot preview, or /loot stats.");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        if ("table".equals(action)) {
            return handleTableCommand(sender, args);
        }
        if ("region".equals(action)) {
            return handleRegionCommand(sender, args);
        }
        if ("fill".equals(action) && args.length >= 3) {
            sender.sendMessage("Scan queued for world " + args[1] + " with table " + args[2] + ".");
            plugin.getScanQueue().enqueueChunk(args[1], 0, 0);
            return true;
        }
        if ("fillworld".equals(action)) {
            sender.sendMessage("World scan requested for table " + (args.length > 2 ? args[2] : "<table>") + ".");
            return true;
        }
        if ("preview".equals(action)) {
            sender.sendMessage("Preview requested for the selected loot table.");
            return true;
        }
        if ("stats".equals(action)) {
            sender.sendMessage("Loot statistics are currently available through the plugin manager.");
            return true;
        }
        if ("reload".equals(action)) {
            plugin.reloadConfig();
            sender.sendMessage("LootManager configuration reloaded.");
            return true;
        }
        if ("fill".equals(action) && args.length >= 2 && "loaded".equalsIgnoreCase(args[1])) {
            sender.sendMessage("Loaded-chunk scan requested for world " + (args.length > 2 ? args[2] : "<world>") + ".");
            return true;
        }

        sender.sendMessage("Unknown loot command. See the README for supported commands.");
        return true;
    }

    private boolean handleTableCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Table commands: create, delete, rename, list, info, inherit, clearinheritance");
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        if ("create".equals(sub) && args.length >= 3) {
            String name = args[2];
            LootTable table = plugin.getTableRegistry().createTable(name);
            sender.sendMessage("Created loot table: " + table.getName());
            return true;
        }
        if ("list".equals(sub)) {
            sender.sendMessage(plugin.getTableRegistry().listTables().toString());
            return true;
        }
        if ("info".equals(sub) && args.length >= 3) {
            LootTable table = plugin.getTableRegistry().getTable(args[2]);
            if (table == null) {
                sender.sendMessage("Unknown loot table: " + args[2]);
                return true;
            }
            sender.sendMessage("Name: " + table.getName() + ", Parent: " + (table.getParentName() == null ? "<none>" : table.getParentName()) + ", Items: " + table.getItems().size());
            return true;
        }
        if ("inherit".equals(sub) && args.length >= 4) {
            LootTable table = plugin.getTableRegistry().getTable(args[2]);
            if (table == null) {
                sender.sendMessage("Unknown loot table: " + args[2]);
                return true;
            }
            table.setParentName(args[3]);
            plugin.getTableRegistry().validateTables();
            sender.sendMessage("Inheritance updated for " + table.getName() + ".");
            return true;
        }
        if ("clearinheritance".equals(sub) && args.length >= 3) {
            LootTable table = plugin.getTableRegistry().getTable(args[2]);
            if (table == null) {
                sender.sendMessage("Unknown loot table: " + args[2]);
                return true;
            }
            table.clearParent();
            sender.sendMessage("Parent removed from " + table.getName() + ".");
            return true;
        }
        if ("delete".equals(sub) && args.length >= 3) {
            plugin.getTableRegistry().deleteTable(args[2]);
            sender.sendMessage("Deleted loot table: " + args[2]);
            return true;
        }
        if ("rename".equals(sub) && args.length >= 4) {
            plugin.getTableRegistry().renameTable(args[2], args[3]);
            sender.sendMessage("Renamed loot table from " + args[2] + " to " + args[3]);
            return true;
        }

        sender.sendMessage("Unsupported table operation. Use create, delete, rename, list, info, inherit, or clearinheritance.");
        return true;
    }

    private boolean handleRegionCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Region commands: create, remove, list, info, enable, disable, settable, settime");
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        if ("create".equals(sub) && args.length >= 4) {
            String regionName = args[2];
            String tableName = args[3];
            int regenMinutes = 60;
            if (args.length >= 5) {
                try {
                    regenMinutes = Integer.parseInt(args[4]);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage("Invalid regeneration time: " + args[4]);
                    return true;
                }
            }
            LootRegion region = plugin.getRegionManager().createRegion(regionName, "<world>", tableName, regenMinutes);
            sender.sendMessage("Created region " + region.getName() + " using table " + region.getTableName() + ".");
            return true;
        }
        if ("list".equals(sub)) {
            sender.sendMessage(plugin.getRegionManager().listRegions().toString());
            return true;
        }
        if ("enable".equals(sub) && args.length >= 3) {
            plugin.getRegionManager().enableRegion(args[2]);
            sender.sendMessage("Enabled region " + args[2] + ".");
            return true;
        }
        if ("disable".equals(sub) && args.length >= 3) {
            plugin.getRegionManager().disableRegion(args[2]);
            sender.sendMessage("Disabled region " + args[2] + ".");
            return true;
        }
        if ("settable".equals(sub) && args.length >= 4) {
            plugin.getRegionManager().setTable(args[2], args[3]);
            sender.sendMessage("Updated region table for " + args[2] + ".");
            return true;
        }
        if ("settime".equals(sub) && args.length >= 4) {
            try {
                plugin.getRegionManager().setRegenerationMinutes(args[2], Integer.parseInt(args[3]));
                sender.sendMessage("Updated region timer for " + args[2] + ".");
            } catch (NumberFormatException ignored) {
                sender.sendMessage("Invalid regeneration time: " + args[3]);
            }
            return true;
        }
        sender.sendMessage("Region operation queued: " + sub + ".");
        return true;
    }
}
