package com.quickshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class ShopCommand implements CommandExecutor, TabCompleter {
    private final QuickShopPlugin plugin;

    public ShopCommand(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    /** Shared by the normal command and the PlayerCommandPreprocessEvent interceptor. */
    public void handle(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("quickshop.reload")) {
                sender.sendMessage("You don't have permission.");
                return;
            }
            plugin.reloadAll();
            sender.sendMessage("QuickShop reloaded (config.yml + prices.yml).");
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can open the shop. Use /shop reload from console.");
            return;
        }
        if (!p.hasPermission("quickshop.use")) {
            p.sendMessage("You don't have permission.");
            return;
        }
        plugin.gui().openQuickBuy(p);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        handle(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("quickshop.reload") && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
