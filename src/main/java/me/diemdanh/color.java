package me.diemdanh;

import org.bukkit.ChatColor;

public class color {
    public static String transalate(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
