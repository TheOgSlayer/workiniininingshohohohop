package com.quickshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Runs once a minute. Every N minutes of online time a player is awarded shards. */
public final class PlaytimeRewarder implements Runnable {
    private final QuickShopPlugin plugin;

    public PlaytimeRewarder(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("shards.playtime.enabled", true)) return;
        int interval = Math.max(1, cfg.getInt("shards.playtime.interval-minutes", 5));
        long amount = cfg.getLong("shards.playtime.amount", 3);
        if (amount <= 0) return;
        DataManager data = plugin.data();

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            int minutes = data.getPlaytime(id) + 1;   // progress is saved, so relogging doesn't reset it
            if (minutes < interval) {
                data.setPlaytime(id, minutes);
                continue;
            }
            data.setPlaytime(id, 0);
            data.addShards(id, amount);
            p.sendMessage(Component.text("Awarded ")
                    .append(Component.text(amount + " shards", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" for "))
                    .append(Component.text("playing", Texts.BLUE))
                    .append(Component.text(" the server")));
        }
    }
}
