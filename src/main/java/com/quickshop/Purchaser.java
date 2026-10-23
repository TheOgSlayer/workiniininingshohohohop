package com.quickshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * The hot path. Runs on every click, so: no inventory copies, no GUI refresh,
 * no cooldown, no saving. Just: space check -> balance check -> withdraw -> give.
 */
public final class Purchaser {
    private final QuickShopPlugin plugin;

    public Purchaser(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void buy(Player p, Product prod, int amount) {
        if (prod.usesShards()) buyWithShards(p, prod, amount);
        else buyWithMoney(p, prod, amount);
    }

    private void buyWithMoney(Player p, Product prod, int amount) {
        PlayerInventory inv = p.getInventory();
        if (!hasSpace(inv, prod, amount)) {
            p.sendActionBar(Texts.INVENTORY_FULL);
            return;
        }
        EconomyHook eco = plugin.economy();
        if (!eco.available()) {
            p.sendActionBar(Texts.NO_ECONOMY);
            return;
        }
        double total = prod.price * amount;
        double bal = eco.balance(p);
        if (bal < total) {
            p.sendActionBar(Texts.notEnough(prod.name, "money", "$" + Texts.num(total - bal)));
            return;
        }
        if (!eco.withdraw(p, total)) {
            p.sendActionBar(Texts.notEnough(prod.name, "money", "$" + Texts.num(total)));
            return;
        }
        ItemStack stack = prod.template.clone();
        stack.setAmount(amount);
        Map<Integer, ItemStack> left = inv.addItem(stack);
        if (!left.isEmpty()) { // should not happen after hasSpace(); never lose items
            for (ItemStack s : left.values()) p.getWorld().dropItem(p.getLocation(), s);
        }
    }

    private void buyWithShards(Player p, Product prod, int amount) {
        long cost = Math.round(prod.price * amount);
        DataManager data = plugin.data();
        long bal = data.getShards(p.getUniqueId());
        if (bal < cost) {
            p.sendActionBar(Texts.notEnough(prod.name, "shards", (cost - bal) + " shards"));
            return;
        }
        data.takeShards(p.getUniqueId(), cost);
        String cmd = plugin.getConfig().getString("shards.spawner-command", "givespawner {player} {mob} {amount}")
                .replace("{player}", p.getName())
                .replace("{mob}", prod.id)
                .replace("{amount}", Integer.toString(amount));
        boolean ok;
        try {
            ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Exception ex) {
            ok = false;
        }
        if (!ok) { // command failed: refund
            data.addShards(p.getUniqueId(), cost);
            p.sendMessage("Spawner command failed - you were not charged. Tell an admin.");
            plugin.getLogger().warning("Spawner command failed: " + cmd);
        }
    }

    /** Walks the 36 storage slots via getItem() (no array copy). Returns as soon as space is found. */
    private boolean hasSpace(PlayerInventory inv, Product prod, int amount) {
        Material m = prod.template.getType();
        int max = prod.maxStack;
        int free = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) return true; // amount <= maxStack, one empty slot is enough
            if (max > 1 && it.getType() == m && !it.hasItemMeta()) {
                free += max - it.getAmount();
                if (free >= amount) return true;
            }
        }
        return false;
    }
}
