package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChatService {
    private static final String CARGO_API = "com.cargoplus.api.CargoPlusAPI";
    private final ConfigManager config;
    private final ChatDelayService delayService;

    public ChatService(ConfigManager config, ChatDelayService delayService) {
        this.config = config;
        this.delayService = delayService;
    }

    public void sendLocalMessage(Player sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendLocalMessage(sender, message));
            return;
        }
        if (!isAuthenticated(sender)) {
            sender.sendMessage(config.getMessage("nao-autenticado"));
            return;
        }
        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }
        int range = config.getLocalChatRange();
        long rangeSquared = (long) range * range;
        Location senderLocation = sender.getLocation();
        World senderWorld = senderLocation.getWorld();
        String formatted = formatMessage(config.getLocalChatFormat(), sender, message, senderWorld != null ? senderWorld.getName() : "");
        boolean deliveredToAnotherPlayer = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) {
                online.sendMessage(formatted);
                continue;
            }
            World onlineWorld = online.getWorld();
            if (onlineWorld == null || senderWorld == null || !onlineWorld.equals(senderWorld)) continue;
            if (senderLocation.distanceSquared(online.getLocation()) <= rangeSquared) {
                online.sendMessage(formatted);
                deliveredToAnotherPlayer = true;
            }
        }
        if (!deliveredToAnotherPlayer) {
            String alone = config.getMessage("ninguem-por-perto");
            if (!alone.isEmpty()) sender.sendMessage(alone);
        }
    }

    public void sendGlobalMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendGlobalMessage(sender, message));
            return;
        }
        if (sender instanceof Player player && !isAuthenticated(player)) {
            sender.sendMessage(config.getMessage("nao-autenticado"));
            return;
        }
        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }
        String formatted = formatMessage(config.getGlobalChatFormat(), sender, message, "");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!(online.isOnline()) || (isAuthenticated(online))) online.sendMessage(formatted);
        }
        notifyConsole(sender, formatted);
    }

    public void sendStaffMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendStaffMessage(sender, message));
            return;
        }
        if (sender instanceof Player player && !isAuthenticated(player)) {
            sender.sendMessage(config.getMessage("nao-autenticado"));
            return;
        }
        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }
        String formatted = formatMessage(config.getStaffChatFormat(), sender, message, "");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("chat.staff") && isAuthenticated(online)) online.sendMessage(formatted);
        }
        notifyConsole(sender, formatted);
    }

    private void notifyConsole(CommandSender sender, String formatted) {
        if (!(sender instanceof ConsoleCommandSender)) Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private boolean isAuthenticated(Player player) {
        if (player == null || !player.isOnline()) return false;
        Plugin auth = Bukkit.getPluginManager().getPlugin("AuthSystem");
        if (auth == null || !auth.isEnabled()) return false;
        try {
            Method method = auth.getClass().getMethod("isAuthenticated", Player.class);
            Object result = method.invoke(auth, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private String formatMessage(String format, CommandSender sender, String message, String world) {
        String playerName = sender instanceof ConsoleCommandSender ? "Console" : sender.getName();
        String prefix = sender instanceof Player ? getCargoPrefix(((Player) sender).getUniqueId()) : "";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{player}", playerName);
        placeholders.put("{message}", message);
        placeholders.put("{world}", world);
        placeholders.put("{prefix}", prefix);
        return MessageUtil.apply(format, placeholders);
    }

    private String getCargoPrefix(UUID uuid) {
        try {
            Plugin cargoPlugin = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (cargoPlugin == null || !cargoPlugin.isEnabled()) return "";

            Class<?> apiClass = Class.forName(CARGO_API, true, cargoPlugin.getClass().getClassLoader());
            Object registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return "";

            Method providerMethod = registration.getClass().getMethod("getProvider");
            Object api = providerMethod.invoke(registration);
            Method prefixMethod = apiClass.getMethod("getPrefix", UUID.class);
            Object value = prefixMethod.invoke(api, uuid);
            return value instanceof String ? (String) value : "";
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "";
        }
    }
}
