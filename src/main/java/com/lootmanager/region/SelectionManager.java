package com.lootmanager.region;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private final Map<UUID, Selection> selections = new HashMap<UUID, Selection>();

    public void setPosition(UUID player, int position, Location location) {
        Selection selection = getOrCreate(player);
        if (position == 1) {
            selection.world = location.getWorld().getName();
            selection.pos1 = location.clone();
        } else {
            if (selection.world != null && !selection.world.equals(location.getWorld().getName())) {
                throw new IllegalArgumentException("Both selection points must be in the same world.");
            }
            selection.world = location.getWorld().getName();
            selection.pos2 = location.clone();
        }
    }

    public boolean hasCompleteSelection(UUID player) {
        Selection selection = selections.get(player);
        return selection != null && selection.pos1 != null && selection.pos2 != null
                && selection.pos1.getWorld().equals(selection.pos2.getWorld());
    }

    public String getWorldName(UUID player) {
        Selection selection = selections.get(player);
        return selection == null ? null : selection.world;
    }

    public Location getPosition(UUID player, int position) {
        Selection selection = selections.get(player);
        if (selection == null) {
            return null;
        }
        return (position == 1 ? selection.pos1 : selection.pos2);
    }

    public void clear(UUID player) {
        selections.remove(player);
    }

    private Selection getOrCreate(UUID player) {
        Selection selection = selections.get(player);
        if (selection == null) {
            selection = new Selection();
            selections.put(player, selection);
        }
        return selection;
    }

    private static class Selection {
        private String world;
        private Location pos1;
        private Location pos2;
    }
}