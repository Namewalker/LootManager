package com.lootmanager.command;

import com.lootmanager.LootManagerPlugin;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LootTabCompleter implements TabCompleter {

    private final LootManagerPlugin plugin;

    public LootTabCompleter(LootManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = new ArrayList<String>();
        if (args.length == 1) {
            candidates.addAll(Arrays.asList("table", "edit", "preview", "fillworld", "fill", "fillselection",
                    "region", "registry", "refill", "wand", "pos1", "pos2", "clearselection", "showselection",
                    "info", "progress", "pause", "resume", "cancel", "stats", "reload", "help"));
        } else if ("table".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                candidates.addAll(Arrays.asList("create", "delete", "rename", "list", "info", "inherit", "clearinheritance", "clear"));
            } else if (args.length == 3 && !"create".equalsIgnoreCase(args[1])) {
                candidates.addAll(plugin.getTableRegistry().listTables());
            } else if (args.length == 4 && "inherit".equalsIgnoreCase(args[1])) {
                candidates.addAll(plugin.getTableRegistry().listTables());
            }
        } else if ("region".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                candidates.addAll(Arrays.asList("create", "remove", "list", "info", "enable", "disable", "settable", "settime"));
            } else if (args.length == 3 && !"create".equalsIgnoreCase(args[1])) {
                candidates.addAll(plugin.getRegionManager().listRegions());
            } else if (args.length == 4 && "create".equalsIgnoreCase(args[1])) {
                candidates.addAll(plugin.getTableRegistry().listTables());
            } else if (args.length == 4 && "settable".equalsIgnoreCase(args[1])) {
                candidates.addAll(plugin.getTableRegistry().listTables());
            }
        } else if ("fillworld".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                candidates.addAll(worldNames());
            } else if (args.length == 3) {
                candidates.addAll(plugin.getTableRegistry().listTables());
            }
        } else if ("registry".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                candidates.addAll(Arrays.asList("rebuild", "stats", "validate"));
            } else if (args.length == 3 && "rebuild".equalsIgnoreCase(args[1])) {
                candidates.addAll(worldNames());
            }
        } else if ("fill".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                candidates.add("loaded");
            } else if (args.length == 3 && "loaded".equalsIgnoreCase(args[1])) {
                candidates.addAll(worldNames());
            } else if (args.length == 4 && "loaded".equalsIgnoreCase(args[1])) {
                candidates.addAll(plugin.getTableRegistry().listTables());
            }
        } else if ("fillselection".equalsIgnoreCase(args[0]) || "edit".equalsIgnoreCase(args[0])
                || "preview".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                candidates.addAll(plugin.getTableRegistry().listTables());
            }
        } else if ("refill".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                candidates.add("region");
            } else if (args.length == 3 && "region".equalsIgnoreCase(args[1])) {
                candidates.addAll(plugin.getRegionManager().listRegions());
            }
        }
        return matching(candidates, args[args.length - 1]);
    }

    private List<String> worldNames() {
        List<String> worlds = new ArrayList<String>();
        for (World world : plugin.getServer().getWorlds()) {
            worlds.add(world.getName());
        }
        Collections.sort(worlds);
        return worlds;
    }

    private List<String> matching(List<String> values, String prefix) {
        List<String> result = new ArrayList<String>();
        String normalized = prefix.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
        }
        Collections.sort(result);
        return result;
    }
}
