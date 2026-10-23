package com.quickshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

/** Text helpers: small caps, number formatting and the action bar messages. */
public final class Texts {
    private Texts() {}

    public static final TextColor BLUE = TextColor.color(0x00a2ff);
    private static final String SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";

    // Pre-built (immutable) components, so spam-clicking allocates nothing.
    public static final Component INVENTORY_FULL =
            sc("inventory", BLUE).append(Component.space()).append(sc("full", NamedTextColor.RED));
    public static final Component NO_ECONOMY =
            sc("no economy found. install vault and an economy plugin", NamedTextColor.RED);
    public static final Component REMOVED =
            sc("removed", NamedTextColor.RED).append(Component.space())
                    .append(sc("item", BLUE)).append(Component.space())
                    .append(sc("from quick buy", NamedTextColor.GRAY));

    public static String smallCaps(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            b.append(c >= 'a' && c <= 'z' ? SMALL.charAt(c - 'a') : s.charAt(i));
        }
        return b.toString();
    }

    public static Component sc(String text, TextColor color) {
        return Component.text(smallCaps(text), color);
    }

    public static Component added(String item) {
        return sc("added", NamedTextColor.GREEN).append(Component.space())
                .append(sc(item, BLUE)).append(Component.space())
                .append(sc("to quick buy", NamedTextColor.GRAY));
    }

    /** "not enough money to buy <item>. need <amount> more" */
    public static Component notEnough(String item, String currencyWord, String amountText) {
        return sc("not enough " + currencyWord + " to buy ", NamedTextColor.GRAY)
                .append(sc(item, BLUE))
                .append(sc(". need ", NamedTextColor.GRAY))
                .append(sc(amountText, NamedTextColor.DARK_RED))
                .append(sc(" more", NamedTextColor.GRAY));
    }

    public static String num(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return Long.toString((long) d);
        return String.format(Locale.US, "%.2f", d);
    }

    public static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.toLowerCase(Locale.ROOT).split("[_ ]+")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    public static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= 10 ? r[n] : Integer.toString(n);
    }
}
