package me.homas343.extrabuttons;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class AdminCommand implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            Core.getInstance().reloadConfig();
            sender.sendMessage("");
        } else {
            sender.sendMessage("reload");
        }
        return true;
    }
    
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase()))
            completions.add("reload");
        return completions;
    }
}
