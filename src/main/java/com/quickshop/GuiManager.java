package com.quickshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.Consumer;

/** Builds every chest GUI. Chest GUIs only - no Dialog API. */
public final class GuiManager {
    public static final int PREV = 45, BACK = 49, NEXT = 53;   // 54-slot shop pages
    public static final int PICK_BACK = 31;                    // 36-slot menus
    public static final int AMT_BACK = 21, AMT_CONFIRM = 23, AMT_ITEM = 13;
    private static final int[] GRID = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final QuickShopPlugin plugin;

    public GuiManager(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ items

    private static Component plain(String s, TextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack named(Material m, String name, TextColor color, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(plain(name, color));
        if (lore.length > 0) {
            meta.lore(java.util.Arrays.stream(lore).map(l -> plain(l, NamedTextColor.GRAY)).toList());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(meta);
        return it;
    }

    /** The product as shown in a slot: stack size = amount, lore = price only. */
    public ItemStack display(Product p, int amount) {
        ItemStack it = p.template != null ? p.template.clone() : new ItemStack(p.icon);
        it.setAmount(Math.max(1, Math.min(amount, p.maxStack)));
        ItemMeta meta = it.getItemMeta();
        meta.displayName(plain(p.name, NamedTextColor.WHITE));
        meta.lore(List.of(plain(p.priceLine(it.getAmount()), NamedTextColor.GRAY)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack back() {
        return named(Material.ARROW, "Back", NamedTextColor.RED);
    }

    private void nav(Inventory inv, boolean prev, boolean next) {
        if (prev) inv.setItem(PREV, named(Material.PAPER, "Previous page", NamedTextColor.YELLOW));
        inv.setItem(BACK, back());
        if (next) inv.setItem(NEXT, named(Material.PAPER, "Next page", NamedTextColor.YELLOW));
    }

    // ------------------------------------------------------------- Quick Buy

    public void openQuickBuy(Player p) {
        Holders.QuickBuy h = new Holders.QuickBuy();
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("Quick Buy"));
        h.inv = inv;
        for (var en : plugin.data().getLayout(p.getUniqueId()).entrySet()) {
            int slot = en.getKey();
            DataManager.Saved s = en.getValue();
            Product pr = plugin.prices().find(s.kind(), s.id());
            h.filled[slot] = true;
            h.products[slot] = pr;
            if (pr != null) {
                int amt = Math.min(s.amount(), pr.maxStack);
                h.amounts[slot] = amt;
                inv.setItem(slot, display(pr, amt));
            } else {
                inv.setItem(slot, named(Material.BARRIER, "Unavailable", NamedTextColor.RED,
                        "No longer in prices.yml", "Right click to remove"));
            }
        }
        p.openInventory(inv);
    }

    public void removeFromQuick(Player p, Holders.QuickBuy h, int slot) {
        plugin.data().removeSlot(p.getUniqueId(), slot);
        h.filled[slot] = false;
        h.products[slot] = null;
        h.inv.setItem(slot, null);
        p.sendActionBar(Texts.REMOVED);
    }

    public void addToQuick(Player p, Product pr, int amount, int target) {
        plugin.data().setSlot(p.getUniqueId(), target, pr.kind, pr.id, amount);
        openQuickBuy(p);
        p.sendActionBar(Texts.added(pr.name));
    }

    // -------------------------------------------------------------- pick shop

    public void openPick(Player p, int target) {
        Holders.Pick h = new Holders.Pick(target);
        Inventory inv = Bukkit.createInventory(h, 36, Component.text("Choose a Shop"));
        h.inv = inv;
        int i = 0;
        for (PriceManager.Shop s : plugin.prices().shops()) {
            if (i >= GRID.length) break;
            inv.setItem(GRID[i], named(s.icon, s.title, NamedTextColor.GREEN, "Click to browse"));
            h.ids[GRID[i]] = s.id;
            i++;
        }
        if (plugin.prices().hasBooks() && i < GRID.length) {
            inv.setItem(GRID[i], named(plugin.prices().booksIcon(), plugin.prices().booksTitle(),
                    NamedTextColor.GREEN, "Click to browse"));
            h.ids[GRID[i]] = "books";
        }
        inv.setItem(PICK_BACK, back());
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ shops

    public void openShop(Player p, PriceManager.Shop shop, int target, int page) {
        Holders.ShopView h = new Holders.ShopView(shop, target, page);
        Inventory inv = Bukkit.createInventory(h, 54, Component.text(shop.title));
        h.inv = inv;
        int start = page * 45;
        for (int i = 0; i < 45 && start + i < shop.products.size(); i++) {
            Product pr = shop.products.get(start + i);
            h.products[i] = pr;
            inv.setItem(i, display(pr, 1));
        }
        h.hasNext = start + 45 < shop.products.size();
        nav(inv, page > 0, h.hasNext);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ books

    public void openBookEnchants(Player p, int target, int page) {
        Holders.BookList h = new Holders.BookList(target, page);
        Inventory inv = Bukkit.createInventory(h, 54, Component.text(plugin.prices().booksTitle()));
        h.inv = inv;
        List<PriceManager.BookEnchant> list = plugin.prices().books();
        int start = page * 45;
        for (int i = 0; i < 45 && start + i < list.size(); i++) {
            PriceManager.BookEnchant be = list.get(start + i);
            h.slots[i] = be;
            inv.setItem(i, named(Material.ENCHANTED_BOOK, be.name, NamedTextColor.LIGHT_PURPLE, "Click to choose a level"));
        }
        h.hasNext = start + 45 < list.size();
        nav(inv, page > 0, h.hasNext);
        p.openInventory(inv);
    }

    public void openBookLevels(Player p, PriceManager.BookEnchant be, int target, int page) {
        Holders.BookLevel h = new Holders.BookLevel(be, target, page);
        Inventory inv = Bukkit.createInventory(h, 36, Component.text(be.name + " - Choose Level"));
        h.inv = inv;
        for (int i = 0; i < be.levels.size() && i < GRID.length; i++) {
            Product pr = be.levels.get(i);
            h.products[GRID[i]] = pr;
            inv.setItem(GRID[i], display(pr, 1));
        }
        inv.setItem(PICK_BACK, back());
        p.openInventory(inv);
    }

    // ----------------------------------------------------------------- amount

    public void openAmount(Player p, Product pr, int target, Consumer<Player> back) {
        Holders.Amount h = new Holders.Amount(pr, target, back);
        Inventory inv = Bukkit.createInventory(h, 27, Component.text("Choose Amount"));
        h.inv = inv;
        inv.setItem(10, pane(Material.RED_STAINED_GLASS_PANE, -64));
        inv.setItem(11, pane(Material.RED_STAINED_GLASS_PANE, -16));
        inv.setItem(12, pane(Material.RED_STAINED_GLASS_PANE, -1));
        inv.setItem(AMT_ITEM, display(pr, 1));
        inv.setItem(14, pane(Material.GREEN_STAINED_GLASS_PANE, 1));
        inv.setItem(15, pane(Material.GREEN_STAINED_GLASS_PANE, 16));
        inv.setItem(16, pane(Material.GREEN_STAINED_GLASS_PANE, 64));
        inv.setItem(AMT_BACK, back());
        inv.setItem(AMT_CONFIRM, named(Material.EMERALD, "Confirm", NamedTextColor.GREEN));
        p.openInventory(inv);
    }

    private ItemStack pane(Material m, int delta) {
        ItemStack it = new ItemStack(m, Math.max(1, Math.min(64, Math.abs(delta))));
        ItemMeta meta = it.getItemMeta();
        String label = (delta > 0 ? "+" : "-") + Math.abs(delta);
        meta.displayName(plain(label, delta > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decorate(TextDecoration.BOLD));
        it.setItemMeta(meta);
        return it;
    }

    /** Updates the amount in place - the GUI is never reopened. */
    public void changeAmount(Holders.Amount h, int delta) {
        int n = Math.max(1, Math.min(h.product.maxStack, h.amount + delta));
        if (n == h.amount) return;
        h.amount = n;
        h.inv.setItem(AMT_ITEM, display(h.product, n));
    }
}
