package com.quickshop;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** data.yml: each player's Quick Buy layout and shard balances. Saved periodically, never on the click path. */
public final class DataManager {
    public record Saved(Product.Kind kind, String id, int amount) {}

    private final QuickShopPlugin plugin;
    private final File file;
    private YamlConfiguration cfg = new YamlConfiguration();
    private boolean dirty;
    private long seq;
    private long written;
    private final Object ioLock = new Object();

    public DataManager(QuickShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        if (file.exists()) cfg = YamlConfiguration.loadConfiguration(file);
    }

    // ---- Quick Buy layout ----

    public Map<Integer, Saved> getLayout(UUID id) {
        Map<Integer, Saved> out = new TreeMap<>();
        ConfigurationSection sec = cfg.getConfigurationSection("layouts." + id);
        if (sec == null) return out;
        for (String k : sec.getKeys(false)) {
            try {
                int slot = Integer.parseInt(k);
                ConfigurationSection s = sec.getConfigurationSection(k);
                if (slot < 0 || slot > 53 || s == null) continue;
                String pid = s.getString("id");
                if (pid == null) continue;
                Product.Kind kind = Product.Kind.valueOf(s.getString("type", "ITEM"));
                out.put(slot, new Saved(kind, pid, Math.max(1, s.getInt("amount", 1))));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    public void setSlot(UUID id, int slot, Product.Kind kind, String pid, int amount) {
        String base = "layouts." + id + "." + slot;
        cfg.set(base + ".type", kind.name());
        cfg.set(base + ".id", pid);
        cfg.set(base + ".amount", amount);
        dirty = true;
    }

    public void removeSlot(UUID id, int slot) {
        cfg.set("layouts." + id + "." + slot, null);
        dirty = true;
    }

    // ---- Shards ----

    public long getShards(UUID id) {
        return cfg.getLong("shards." + id, 0L);
    }

    public void setShards(UUID id, long amount) {
        cfg.set("shards." + id, Math.max(0L, amount));
        dirty = true;
    }

    public void addShards(UUID id, long amount) {
        setShards(id, getShards(id) + amount);
    }

    /** Returns false (and changes nothing) if the player has less than amount. */
    public boolean takeShards(UUID id, long amount) {
        long bal = getShards(id);
        if (bal < amount) return false;
        setShards(id, bal - amount);
        return true;
    }

    // ---- Playtime progress (minutes since the last playtime reward) ----

    public int getPlaytime(UUID id) {
        return cfg.getInt("playtime." + id, 0);
    }

    public void setPlaytime(UUID id, int minutes) {
        cfg.set("playtime." + id, minutes);
        dirty = true;
    }

    // ---- Saving ----

    public void flush(boolean sync) {
        if (!dirty) return;
        dirty = false;
        final String data = cfg.saveToString();
        final long s = ++seq;
        if (sync) write(s, data);
        else Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(s, data));
    }

    private void write(long s, String data) {
        synchronized (ioLock) {
            if (s <= written) return; // a newer snapshot is already on disk
            try {
                File tmp = new File(file.getParentFile(), "data.yml.tmp");
                Files.writeString(tmp.toPath(), data, StandardCharsets.UTF_8);
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                written = s;
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
            }
        }
    }
}
