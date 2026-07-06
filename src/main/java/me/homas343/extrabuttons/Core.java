package me.homas343.extrabuttons;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Core extends JavaPlugin {
    private static Core instance;
    
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        c.sendMessage("");
        Bukkit.getServer().getPluginManager().registerEvents(new ButtonListener(), (Plugin)this);
        getCommand("extrabutton").setExecutor(new AdminCommand());
        getCommand("extrabutton").setTabCompleter(new AdminCommand());
    }
    
    public static Core getInstance() {
        return instance;
    }
}
