package com.quickshop;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ShardsCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN = "quickshop.shards.admin";
    private static final long MAX = 1_000_000_000_000L;
    private final QuickShopPlugin plugin;

    public ShardsCommand(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        DataManager data = plugin.data();
        if (args.length == 0) {
            if (sender instanceof Player p) sender.sendMessage("You have " + data.getShards(p.getUniqueId()) + " shards.");
            else sender.sendMessage("Usage: /shards <player>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "pay" -> pay(sender, args);
            case "give", "take", "set" -> admin(sender, sub, args);
            default -> {
                OfflinePlayer t = resolve(args[0]);
                if (t == null) sender.sendMessage("Player not found.");
                else sender.sendMessage((t.getName() != null ? t.getName() : args[0]) + " has "
                        + data.getShards(t.getUniqueId()) + " shards.");
            }
        }
        return true;
    }

    private void pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can pay shards.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /shards pay <player> <amount>");
            return;
        }
        OfflinePlayer t = resolve(args[1]);
        if (t == null) {
            sender.sendMessage("Player not found.");
            return;
        }
        if (t.getUniqueId().equals(p.getUniqueId())) {
            sender.sendMessage("You can't pay yourself.");
            return;
        }
        long amt = parse(args[2]);
        if (amt <= 0) {
            sender.sendMessage("Amount must be a positive whole number.");
            return;
        }
        if (!plugin.data().takeShards(p.getUniqueId(), amt)) {
            sender.sendMessage("You don't have enough shards.");
            return;
        }
        plugin.data().addShards(t.getUniqueId(), amt);
        String name = t.getName() != null ? t.getName() : args[1];
        sender.sendMessage("Sent " + amt + " shards to " + name + ".");
        Player online = t.getPlayer();
        if (online != null) online.sendMessage("You received " + amt + " shards from " + p.getName() + ".");
    }

    private void admin(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage("You don't have permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /shards " + sub + " <player> <amount>");
            return;
        }
        OfflinePlayer t = resolve(args[1]);
        if (t == null) {
            sender.sendMessage("Player not found.");
            return;
        }
        long amt = parse(args[2]);
        if (amt < 0 || (!sub.equals("set") && amt == 0)) {
            sender.sendMessage("Invalid amount.");
            return;
        }
        UUID id = t.getUniqueId();
        DataManager d = plugin.data();
        switch (sub) {
            case "give" -> d.addShards(id, amt);
            case "take" -> d.setShards(id, d.getShards(id) - amt);
            default -> d.setShards(id, amt);
        }
        sender.sendMessage((t.getName() != null ? t.getName() : args[1]) + " now has " + d.getShards(id) + " shards.");
    }

    private OfflinePlayer resolve(String name) {
        Player on = Bukkit.getPlayerExact(name);
        return on != null ? on : Bukkit.getOfflinePlayerIfCached(name);
    }

    private long parse(String s) {
        try {
            long v = Long.parseLong(s);
            return v > MAX ? -1 : v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        boolean admin = sender.hasPermission(ADMIN);
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("pay");
            if (admin) out.addAll(List.of("give", "take", "set"));
            addPlayers(out);
        } else {
            String sub = args[0].toLowerCase(Locale.ROOT);
            boolean valid = sub.equals("pay") || (admin && (sub.equals("give") || sub.equals("take") || sub.equals("set")));
            if (valid && args.length == 2) addPlayers(out);
            else if (valid && args.length == 3) out.addAll(List.of("1", "10", "100", "1000"));
        }
        String cur = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(cur));
        return out;
    }

    private void addPlayers(List<String> out) {
        for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
    }
}
