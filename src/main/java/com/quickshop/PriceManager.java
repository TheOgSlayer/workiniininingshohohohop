package com.quickshop;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.io.File;
import java.util.*;

/** Loads prices.yml. */
public final class PriceManager {
    public static final class Shop {
        public final String id;
        public final String title;
        public final Material icon;
        public final List<Product> products = new ArrayList<>();

        Shop(String id, String title, Material icon) {
            this.id = id;
            this.title = title;
            this.icon = icon;
        }
    }

    public static final class BookEnchant {
        public final String id;
        public final String name;
        public final List<Product> levels = new ArrayList<>();

        BookEnchant(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final QuickShopPlugin plugin;
    private final Map<String, Shop> shops = new LinkedHashMap<>();
    private final List<BookEnchant> books = new ArrayList<>();
    private final Map<String, Product> index = new HashMap<>();
    private String booksTitle = "Books Shop";
    private Material booksIcon = Material.ENCHANTED_BOOK;

    public PriceManager(QuickShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File f = new File(plugin.getDataFolder(), "prices.yml");
        if (!f.exists()) plugin.saveResource("prices.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);

        shops.clear();
        books.clear();
        index.clear();

        for (String key : y.getKeys(false)) {
            ConfigurationSection sec = y.getConfigurationSection(key);
            if (sec == null) continue;
            try {
                if (key.equalsIgnoreCase("books")) loadBooks(sec);
                else loadShop(key.toLowerCase(Locale.ROOT), sec);
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not load '" + key + "' from prices.yml: " + ex.getMessage());
            }
        }
    }

    private Material icon(ConfigurationSection sec, Material fallback) {
        Material m = Material.matchMaterial(sec.getString("icon", ""));
        return m != null && m.isItem() ? m : fallback;
    }

    private void loadShop(String id, ConfigurationSection sec) {
        Shop shop = new Shop(id, sec.getString("title", Texts.pretty(id) + " Shop"), icon(sec, Material.CHEST));
        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items != null) {
            boolean shards = id.equals("shards");
            for (String k : items.getKeys(false)) {
                double price = items.getDouble(k, -1);
                if (price < 0) {
                    plugin.getLogger().warning("prices.yml: bad price for " + id + "." + k);
                    continue;
                }
                Product p;
                if (shards) {
                    p = Product.spawner(k.toLowerCase(Locale.ROOT), price);
                } else {
                    Material m = Material.matchMaterial(k);
                    if (m == null || !m.isItem() || m.isAir()) {
                        plugin.getLogger().warning("prices.yml: unknown item " + id + "." + k);
                        continue;
                    }
                    p = Product.item(m, price);
                }
                shop.products.add(p);
                index.put(p.key(), p);
            }
        }
        shops.put(id, shop);
    }

    private void loadBooks(ConfigurationSection sec) {
        booksTitle = sec.getString("title", "Books Shop");
        booksIcon = icon(sec, Material.ENCHANTED_BOOK);
        ConfigurationSection ench = sec.getConfigurationSection("enchantments");
        if (ench == null) return;
        for (String e : ench.getKeys(false)) {
            String eid = e.toLowerCase(Locale.ROOT);
            Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(eid));
            ConfigurationSection lv = ench.getConfigurationSection(e);
            if (enchantment == null || lv == null) {
                plugin.getLogger().warning("prices.yml: unknown enchantment " + e);
                continue;
            }
            BookEnchant be = new BookEnchant(eid, Texts.pretty(eid));
            TreeMap<Integer, Double> sorted = new TreeMap<>();
            for (String l : lv.getKeys(false)) {
                try {
                    sorted.put(Integer.parseInt(l), lv.getDouble(l, -1));
                } catch (NumberFormatException ignored) {
                }
            }
            for (Map.Entry<Integer, Double> en : sorted.entrySet()) {
                if (en.getValue() < 0) continue;
                int level = en.getKey();
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                meta.addStoredEnchant(enchantment, level, true);
                book.setItemMeta(meta);
                String name = be.name + (enchantment.getMaxLevel() == 1 && level == 1 ? "" : " " + Texts.roman(level)) + " Book";
                Product p = Product.book(eid, level, name, en.getValue(), book);
                be.levels.add(p);
                index.put(p.key(), p);
            }
            if (!be.levels.isEmpty()) books.add(be);
        }
    }

    public Product find(Product.Kind kind, String id) {
        return index.get(kind.name() + ":" + id);
    }

    public Collection<Shop> shops() {
        return shops.values();
    }

    public Shop shop(String id) {
        return shops.get(id);
    }

    public List<BookEnchant> books() {
        return books;
    }

    public boolean hasBooks() {
        return !books.isEmpty();
    }

    public String booksTitle() {
        return booksTitle;
    }

    public Material booksIcon() {
        return booksIcon;
    }
}
