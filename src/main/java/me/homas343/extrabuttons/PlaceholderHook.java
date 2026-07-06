package me.homas343.extrabuttons;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderHook {
    public static String p(String s, Player p) {
        if (s == null) return "";
        if (p == null) return s;
        try {
            return PlaceholderAPI.setPlaceholders(p, s);
        } catch (Exception e) {
            return s;
        }
    }
}