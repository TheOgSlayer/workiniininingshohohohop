package com.quickshop;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;

/** Makes /shop and /quickbuy always reach QuickShop, even if another plugin registers /shop. */
public final class CommandInterceptor implements Listener {
    private static final Set<String> LABELS = Set.of("shop", "quickbuy", "quickshop:shop", "quickshop:quickbuy");
    private final ShopCommand command;

    public CommandInterceptor(ShopCommand command) {
        this.command = command;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg.length() < 2) return;
        int sp = msg.indexOf(' ');
        String label = (sp == -1 ? msg.substring(1) : msg.substring(1, sp)).toLowerCase(Locale.ROOT);
        if (!LABELS.contains(label)) return;
        e.setCancelled(true);
        String rest = sp == -1 ? "" : msg.substring(sp + 1).trim();
        command.handle(e.getPlayer(), rest.isEmpty() ? new String[0] : rest.split("\\s+"));
    }
}
