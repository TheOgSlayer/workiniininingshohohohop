package com.quickshop;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class KillListener implements Listener {
    private final QuickShopPlugin plugin;

    public KillListener(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || e.getEntity() == killer || e.getEntity() instanceof ArmorStand) return;
        var cfg = plugin.getConfig();
        boolean victimIsPlayer = e.getEntity() instanceof Player;
        if (victimIsPlayer ? !cfg.getBoolean("shards.count-players", true) : !cfg.getBoolean("shards.count-mobs", true)) return;
        long amount = cfg.getLong("shards.per-kill", 10);
        if (amount <= 0) return;
        plugin.data().addShards(killer.getUniqueId(), amount);
        if (cfg.getBoolean("shards.notify-on-kill", true)) {
            killer.sendActionBar(Texts.sc("+" + amount + " shards", NamedTextColor.AQUA));
        }
    }
}
