package com.quickshop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Something that can be bought: a normal item, a spawner (shards) or an enchanted book. */
public final class Product {
    public enum Kind { ITEM, SPAWNER, BOOK }

    public final Kind kind;
    public final String id;        // saved in data.yml
    public final String name;      // display name
    public final Material icon;
    public final double price;     // per unit
    public final int maxStack;
    public final ItemStack template; // null for spawners

    private Product(Kind kind, String id, String name, Material icon, double price, int maxStack, ItemStack template) {
        this.kind = kind;
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.price = price;
        this.maxStack = maxStack;
        this.template = template;
    }

    public static Product item(Material m, double price) {
        return new Product(Kind.ITEM, m.name(), Texts.pretty(m.name()), m, price,
                Math.max(1, m.getMaxStackSize()), new ItemStack(m));
    }

    public static Product spawner(String mob, double price) {
        Material egg = Material.matchMaterial(mob.toUpperCase(Locale.ROOT) + "_SPAWN_EGG");
        return new Product(Kind.SPAWNER, mob, Texts.pretty(mob) + " Spawner",
                egg != null ? egg : Material.SPAWNER, price, 64, null);
    }

    public static Product book(String enchId, int level, String displayName, double price, ItemStack template) {
        return new Product(Kind.BOOK, enchId + ":" + level, displayName, Material.ENCHANTED_BOOK, price, 1, template);
    }

    public boolean usesShards() {
        return kind == Kind.SPAWNER;
    }

    public String key() {
        return kind.name() + ":" + id;
    }

    /** "Price: $100" or "Price: 50 shards" for the given amount. */
    public String priceLine(int amount) {
        double total = price * amount;
        return usesShards() ? "Price: " + Texts.num(total) + " shards" : "Price: $" + Texts.num(total);
    }
}
