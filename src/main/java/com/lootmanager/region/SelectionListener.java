package com.lootmanager.region;

import com.lootmanager.LootManagerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SelectionListener implements Listener {

    private final LootManagerPlugin plugin;
    private final SelectionManager selections;

    public SelectionListener(LootManagerPlugin plugin, SelectionManager selections) {
        this.plugin = plugin;
        this.selections = selections;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isWand(item)) {
            return;
        }
        if (!event.getPlayer().hasPermission("loot.wand")) {
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to use the loot wand.");
            return;
        }

        event.setCancelled(true);
        int position = action == Action.LEFT_CLICK_BLOCK ? 1 : 2;
        try {
            selections.setPosition(event.getPlayer().getUniqueId(), position, event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Position " + position + " set to "
                    + event.getClickedBlock().getX() + ", " + event.getClickedBlock().getY() + ", "
                    + event.getClickedBlock().getZ() + " in " + event.getClickedBlock().getWorld().getName() + ".");
        } catch (IllegalArgumentException exception) {
            event.getPlayer().sendMessage(ChatColor.RED + exception.getMessage());
        }
    }

    private boolean isWand(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material configured = Material.matchMaterial(plugin.getConfig().getString("wand.material", "GOLD_HOE"));
        if (configured == null || item.getType() != configured) {
            return false;
        }
        String configuredName = plugin.getConfig().getString("wand.name", "&6Loot Selection Wand");
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', configuredName)));
    }
}