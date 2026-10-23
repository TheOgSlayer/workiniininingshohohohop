package com.quickshop;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class QuickShopPlugin extends JavaPlugin {
    private PriceManager prices;
    private DataManager data;
    private EconomyHook economy;
    private GuiManager gui;
    private Purchaser purchaser;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prices = new PriceManager(this);
        prices.load();
        data = new DataManager(this);
        data.load();
        economy = new EconomyHook(this);   // lazy: Vault is only looked up on the first purchase
        gui = new GuiManager(this);
        purchaser = new Purchaser(this);

        ShopCommand shop = new ShopCommand(this);
        ShardsCommand shards = new ShardsCommand(this);
        bind("shop", shop, shop);
        bind("shards", shards, shards);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(new CommandInterceptor(shop), this);
        pm.registerEvents(new KillListener(this), this);

        // Save data.yml every 30s if something changed (never on the click path).
        getServer().getScheduler().runTaskTimer(this, () -> data.flush(false), 600L, 600L);

        // Playtime shards: checked once a minute.
        getServer().getScheduler().runTaskTimer(this, new PlaytimeRewarder(this), 1200L, 1200L);

        if (pm.getPlugin("Vault") == null) {
            getLogger().info("Vault not found. QuickShop loaded anyway; money purchases need Vault + an economy plugin.");
        }
    }

    @Override
    public void onDisable() {
        if (data != null) data.flush(true);
    }

    private void bind(String name, CommandExecutor ex, TabCompleter tc) {
        PluginCommand c = getCommand(name);
        if (c == null) return;
        c.setExecutor(ex);
        c.setTabCompleter(tc);
    }

    public void reloadAll() {
        reloadConfig();
        prices.load();
    }

    public PriceManager prices() { return prices; }
    public DataManager data() { return data; }
    public EconomyHook economy() { return economy; }
    public GuiManager gui() { return gui; }
    public Purchaser purchaser() { return purchaser; }
}
