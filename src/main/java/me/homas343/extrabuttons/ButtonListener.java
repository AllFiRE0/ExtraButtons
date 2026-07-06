package me.homas343.extrabuttons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ButtonListener implements Listener {
    private final Set<UUID> commandSelectingPlayers = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> timeoutTasks = new HashMap<>();
    
    private FileConfiguration getConfig() {
        return Core.getInstance().getConfig();
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();
        if (!player.isSneaking())
            return;
        if (config.getBoolean("require-permission-to-use", false)) {
            String permission = config.getString("use-permission", "extrabutton.use");
            if (!player.hasPermission(permission)) {
                event.setCancelled(true);
                return;
            }
        }
        UUID playerId = player.getUniqueId();
        if (isInCommandSelectingMode(player)) {
            exitCommandSelectingMode(player, true, true);
            cooldowns.put(playerId, System.currentTimeMillis());
        } else {
            long cooldownTime = config.getLong("cooldown-in-seconds", 0L) * 1000L;
            long lastExitTime = cooldowns.getOrDefault(playerId, 0L);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastExitTime < cooldownTime)
                return;
            enterCommandSelectingMode(player);
        }
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!isInCommandSelectingMode(player))
            return;
        
        FileConfiguration config = getConfig();
        String key = getCommandKey(event.getNewSlot());
        ConfigurationSection slotSection = getCommandSection(config, key);
        
        if (slotSection != null) {
            String displayName = getDisplayName(slotSection, key);
            updateActionBar(player, displayName);
            playSwitchSound(player, slotSection);
        } else {
            clearActionBar(player);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInCommandSelectingMode(player))
            return;
        
        FileConfiguration config = getConfig();
        String key = getCommandKey(player.getInventory().getHeldItemSlot());
        ConfigurationSection slotSection = getCommandSection(config, key);
        
        if (slotSection != null) {
            ConfigurationSection actionSection = null;
            
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                actionSection = slotSection.getConfigurationSection("left");
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                actionSection = slotSection.getConfigurationSection("right");
            }
            
            // Если нет left/right секций, пробуем старый формат
            if (actionSection == null) {
                if (isValidAction(event.getAction(), slotSection)) {
                    actionSection = slotSection;
                }
            }
            
            if (actionSection != null) {
                if (!hasPermission(player, actionSection)) {
                    playSound(player, "no-permission-sound");
                    exitCommandSelectingMode(player, false, false);
                    event.setCancelled(true);
                    return;
                }
                
                if (!checkConditions(player, actionSection)) {
                    sendMessage(player, config.getString("messages.conditions-not-met", 
                        "&c▏  &7Условия не выполнены"));
                    playSound(player, "no-permission-sound");
                    exitCommandSelectingMode(player, false, false);
                    event.setCancelled(true);
                    return;
                }
                
                executeCommands(player, actionSection.getStringList("commands"));
            }
        }
        
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (getConfig().getBoolean("cancel-on.quit", true)) {
            exitCommandSelectingMode(player, false, false);
        }
        cooldowns.remove(player.getUniqueId());
        Integer taskId = timeoutTasks.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (getConfig().getBoolean("cancel-on.death", true)) {
            exitCommandSelectingMode(player, false, false);
        }
    }
    
    private boolean isInCommandSelectingMode(Player player) {
        return commandSelectingPlayers.contains(player.getUniqueId());
    }
    
    private void enterCommandSelectingMode(Player player) {
        FileConfiguration config = getConfig();
        UUID playerId = player.getUniqueId();
        commandSelectingPlayers.add(playerId);
        playSound(player, "enter-sound");
        boolean b = config.getBoolean("messages.send-enter-in-select-mode");
        if (b)
            sendMessage(player, config.getString("messages.enter-in-select-mode"));
        
        // Таймаут
        int timeout = config.getInt("timeout-seconds", 30);
        if (timeout > 0) {
            int taskId = Bukkit.getScheduler().runTaskLater(Core.getInstance(), () -> {
                if (isInCommandSelectingMode(player)) {
                    exitCommandSelectingMode(player, true, true);
                    sendMessage(player, "&e%cmi_user_chatcolor%▏  &7Время выбора истекло");
                }
            }, timeout * 20L);
            timeoutTasks.put(playerId, taskId);
        }
    }
    
    private void exitCommandSelectingMode(Player player, boolean sendLeaveMessage, boolean playSound) {
        FileConfiguration config = getConfig();
        UUID playerId = player.getUniqueId();
        commandSelectingPlayers.remove(playerId);
        
        // Отменяем таймер
        Integer taskId = timeoutTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        
        if (playSound)
            playSound(player, "leave-sound");
        boolean b = config.getBoolean("messages.send-leave-in-select-mode");
        if (sendLeaveMessage && b)
            sendMessage(player, config.getString("messages.leave-select-mode"));
        clearActionBar(player);
    }
    
    private void playSound(Player player, String soundPath) {
        FileConfiguration config = getConfig();
        String soundConfig = config.getString(soundPath);
        if (soundConfig == null)
            return;
        String[] parts = soundConfig.split(";");
        if (parts.length < 3)
            return;
        try {
            Sound sound = Sound.valueOf(parts[0]);
            float volume = Float.parseFloat(parts[1]);
            float pitch = Float.parseFloat(parts[2]);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    
    private void playSwitchSound(Player player, ConfigurationSection slotSection) {
        String soundPath = slotSection.getString("switch-sound");
        if (soundPath != null) {
            String[] parts = soundPath.split(";");
            if (parts.length >= 3) {
                try {
                    Sound sound = Sound.valueOf(parts[0]);
                    float volume = Float.parseFloat(parts[1]);
                    float pitch = Float.parseFloat(parts[2]);
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void sendMessage(Player player, String message) {
        if (message != null)
            player.sendMessage(PlaceholderHook.p(ChatColor.translateAlternateColorCodes('&', message), player));
    }
    
    private void updateActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
    
    private void clearActionBar(Player player) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
    }
    
    private String getCommandKey(int slot) {
        return String.valueOf(slot + 1);
    }
    
    private ConfigurationSection getCommandSection(FileConfiguration config, String key) {
        ConfigurationSection commandsSection = config.getConfigurationSection("commands-list");
        if (commandsSection != null)
            return commandsSection.getConfigurationSection(key);
        return null;
    }
    
    private String getDisplayName(ConfigurationSection slotSection, String key) {
        String displayName = slotSection.getString("display_name", "#" + key);
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }
    
    private boolean isValidAction(Action eventAction, ConfigurationSection commandSection) {
        String configAction = commandSection.getString("action", "RIGHT").toUpperCase();
        if ("LEFT".equals(configAction))
            return (eventAction == Action.LEFT_CLICK_AIR || eventAction == Action.LEFT_CLICK_BLOCK);
        if ("RIGHT".equals(configAction))
            return (eventAction == Action.RIGHT_CLICK_AIR || eventAction == Action.RIGHT_CLICK_BLOCK);
        return false;
    }
    
    private boolean hasPermission(Player player, ConfigurationSection actionSection) {
        FileConfiguration config = getConfig();
        if (actionSection.getBoolean("permission_required", false)) {
            String permission = actionSection.getString("permission");
            if (permission != null && !player.hasPermission(permission)) {
                sendMessage(player, config.getString("messages.no-permissions"));
                return false;
            }
        }
        return true;
    }
    
    private boolean checkConditions(Player player, ConfigurationSection actionSection) {
        boolean required = actionSection.getBoolean("conditions_required", false);
        if (!required) return true;
        
        List<String> conditions = actionSection.getStringList("conditions");
        if (conditions.isEmpty()) return true;
        
        return ConditionChecker.checkConditions(player, conditions);
    }
    
    private void executeCommands(Player player, List<String> commandsList) {
        for (String cmd : commandsList) {
            if (cmd.startsWith("[player]")) {
                player.performCommand(PlaceholderHook.p(cmd.replaceFirst("\\[player\\]\\s*", ""), player));
                continue;
            }
            if (cmd.startsWith("[console]")) {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), PlaceholderHook.p(cmd.replaceFirst("\\[console\\]\\s*", ""), player));
                continue;
            }
            player.performCommand(PlaceholderHook.p(cmd, player));
        }
        exitCommandSelectingMode(player, false, true);
    }
}