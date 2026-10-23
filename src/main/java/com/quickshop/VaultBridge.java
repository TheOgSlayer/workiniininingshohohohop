package com.quickshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** The ONLY class that touches Vault classes. Never loaded unless Vault is present. */
final class VaultBridge {
    private final Economy eco;

    private VaultBridge(Economy eco) {
        this.eco = eco;
    }

    static VaultBridge create() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : new VaultBridge(rsp.getProvider());
    }

    double balance(OfflinePlayer p) {
        return eco.getBalance(p);
    }

    boolean withdraw(OfflinePlayer p, double amount) {
        return eco.withdrawPlayer(p, amount).transactionSuccess();
    }
}
