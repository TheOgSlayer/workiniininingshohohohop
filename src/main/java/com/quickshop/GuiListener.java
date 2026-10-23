package com.quickshop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

public final class GuiListener implements Listener {
    private final QuickShopPlugin plugin;
    private final GuiManager gui;

    public GuiListener(QuickShopPlugin plugin) {
        this.plugin = plugin;
        this.gui = plugin.gui();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Holders.Base base)) return;
        e.setCancelled(true); // nothing can ever be taken out of / put into these GUIs
        if (e.getClickedInventory() != top) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // DOUBLE_CLICK matters: the 2nd click of a fast double click arrives as DOUBLE_CLICK.
        ClickType ct = e.getClick();
        boolean left = ct == ClickType.LEFT || ct == ClickType.SHIFT_LEFT || ct == ClickType.DOUBLE_CLICK;
        boolean right = ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT;
        if (!left && !right) return;
        int slot = e.getSlot();

        if (base instanceof Holders.QuickBuy h) quick(p, h, slot, left);
        else if (base instanceof Holders.Pick h) pick(p, h, slot);
        else if (base instanceof Holders.ShopView h) shop(p, h, slot);
        else if (base instanceof Holders.BookList h) bookList(p, h, slot);
        else if (base instanceof Holders.BookLevel h) bookLevel(p, h, slot);
        else if (base instanceof Holders.Amount h) amount(p, h, slot);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Holders.Base) e.setCancelled(true);
    }

    private void quick(Player p, Holders.QuickBuy h, int slot, boolean left) {
        if (slot < 0 || slot >= 54) return;
        if (h.filled[slot]) {
            if (left) {
                Product pr = h.products[slot];
                if (pr != null) plugin.purchaser().buy(p, pr, h.amounts[slot]); // no GUI update, no cooldown
            } else {
                gui.removeFromQuick(p, h, slot);
            }
        } else {
            gui.openPick(p, slot);
        }
    }

    private void pick(Player p, Holders.Pick h, int slot) {
        if (slot == GuiManager.PICK_BACK) {
            gui.openQuickBuy(p);
            return;
        }
        if (slot < 0 || slot >= h.ids.length || h.ids[slot] == null) return;
        String id = h.ids[slot];
        if (id.equals("books")) {
            gui.openBookEnchants(p, h.target, 0);
        } else {
            PriceManager.Shop s = plugin.prices().shop(id);
            if (s != null) gui.openShop(p, s, h.target, 0);
        }
    }

    private void shop(Player p, Holders.ShopView h, int slot) {
        if (slot == GuiManager.BACK) gui.openPick(p, h.target);
        else if (slot == GuiManager.PREV && h.page > 0) gui.openShop(p, h.shop, h.target, h.page - 1);
        else if (slot == GuiManager.NEXT && h.hasNext) gui.openShop(p, h.shop, h.target, h.page + 1);
        else if (slot >= 0 && slot < 45 && h.products[slot] != null) {
            choose(p, h.products[slot], h.target, pl -> gui.openShop(pl, h.shop, h.target, h.page));
        }
    }

    private void bookList(Player p, Holders.BookList h, int slot) {
        if (slot == GuiManager.BACK) gui.openPick(p, h.target);
        else if (slot == GuiManager.PREV && h.page > 0) gui.openBookEnchants(p, h.target, h.page - 1);
        else if (slot == GuiManager.NEXT && h.hasNext) gui.openBookEnchants(p, h.target, h.page + 1);
        else if (slot >= 0 && slot < 45 && h.slots[slot] != null) gui.openBookLevels(p, h.slots[slot], h.target, h.page);
    }

    private void bookLevel(Player p, Holders.BookLevel h, int slot) {
        if (slot == GuiManager.PICK_BACK) {
            gui.openBookEnchants(p, h.target, h.page);
        } else if (slot >= 0 && slot < h.products.length && h.products[slot] != null) {
            choose(p, h.products[slot], h.target, pl -> gui.openBookLevels(pl, h.enchant, h.target, h.page));
        }
    }

    private void amount(Player p, Holders.Amount h, int slot) {
        switch (slot) {
            case 10 -> gui.changeAmount(h, -64);
            case 11 -> gui.changeAmount(h, -16);
            case 12 -> gui.changeAmount(h, -1);
            case 14 -> gui.changeAmount(h, 1);
            case 15 -> gui.changeAmount(h, 16);
            case 16 -> gui.changeAmount(h, 64);
            case GuiManager.AMT_BACK -> h.back.accept(p);
            case GuiManager.AMT_CONFIRM -> gui.addToQuick(p, h.product, h.amount, h.target);
            default -> { }
        }
    }

    /** Items that stack to 1 skip the amount screen and are added with amount 1. */
    private void choose(Player p, Product pr, int target, Consumer<Player> back) {
        if (pr.maxStack <= 1) gui.addToQuick(p, pr, 1, target);
        else gui.openAmount(p, pr, target, back);
    }
}
