package com.quickshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Lazy Vault lookup. The plugin loads fine without Vault; purchases just show a message. */
public final class EconomyHook {
    private final QuickShopPlugin plugin;
    private VaultBridge bridge;
    private long lastLookup;
    private boolean warned;

    public EconomyHook(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    /** Cheap when connected (one null check). When missing, retries at most every 2 seconds. */
    public boolean available() {
        if (bridge != null) return true;
        long now = System.currentTimeMillis();
        if (now - lastLookup < 2000) return false;
        lastLookup = now;
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                bridge = VaultBridge.create();
            }
        } catch (Throwable t) {
            bridge = null;
        }
        if (bridge == null && !warned) {
            warned = true;
            plugin.getLogger().warning("No Vault economy found - money purchases are disabled until one is available.");
        }
        return bridge != null;
    }

    public double balance(Player p) {
        return bridge.balance(p);
    }

    public boolean withdraw(Player p, double amount) {
        return bridge.withdraw(p, amount);
    }
}
