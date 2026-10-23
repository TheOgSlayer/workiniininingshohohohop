package com.quickshop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.function.Consumer;
import org.bukkit.entity.Player;

/** One holder class per GUI type. All state needed for clicks lives here (O(1) lookups). */
public final class Holders {
    private Holders() {}

    public abstract static class Base implements InventoryHolder {
        public Inventory inv;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    public static final class QuickBuy extends Base {
        public final boolean[] filled = new boolean[54];
        public final Product[] products = new Product[54];
        public final int[] amounts = new int[54];
    }

    public static final class Pick extends Base {
        public final int target;
        public final String[] ids = new String[36];

        public Pick(int target) {
            this.target = target;
        }
    }

    public static final class ShopView extends Base {
        public final PriceManager.Shop shop;
        public final int target;
        public final int page;
        public boolean hasNext;
        public final Product[] products = new Product[54];

        public ShopView(PriceManager.Shop shop, int target, int page) {
            this.shop = shop;
            this.target = target;
            this.page = page;
        }
    }

    public static final class BookList extends Base {
        public final int target;
        public final int page;
        public boolean hasNext;
        public final PriceManager.BookEnchant[] slots = new PriceManager.BookEnchant[54];

        public BookList(int target, int page) {
            this.target = target;
            this.page = page;
        }
    }

    public static final class BookLevel extends Base {
        public final int target;
        public final int page;
        public final PriceManager.BookEnchant enchant;
        public final Product[] products = new Product[36];

        public BookLevel(PriceManager.BookEnchant enchant, int target, int page) {
            this.enchant = enchant;
            this.target = target;
            this.page = page;
        }
    }

    public static final class Amount extends Base {
        public final Product product;
        public final int target;
        public final Consumer<Player> back;
        public int amount = 1;

        public Amount(Product product, int target, Consumer<Player> back) {
            this.product = product;
            this.target = target;
            this.back = back;
        }
    }
}
