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

public final class ChatService {
    private final ConfigManager config;
    private final ChatDelayService delayService;
    private final CargoPlusBridge cargo;
    private volatile Plugin authPlugin;
    private volatile Method authCheckMethod;

    public ChatService(ConfigManager config, ChatDelayService delayService) {
        this.config = config;
        this.delayService = delayService;
        this.cargo = new CargoPlusBridge();
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
            if (online.isOnline() && isAuthenticated(online)) online.sendMessage(formatted);
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
        Plugin auth = Bukkit.getPluginManager().getPlugin("LoginPlus");
        if (auth == null || !auth.isEnabled()) {
            authPlugin = null;
            authCheckMethod = null;
            return false;
        }

        Method method = authCheckMethod;
        if (authPlugin != auth || method == null) {
            synchronized (this) {
                if (authPlugin != auth || authCheckMethod == null) {
                    try {
                        authPlugin = auth;
                        authCheckMethod = auth.getClass().getMethod("isAuthenticated", Player.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        authPlugin = auth;
                        authCheckMethod = null;
                    }
                }
                method = authCheckMethod;
            }
        }

        if (method == null) return false;
        try {
            Object result = method.invoke(auth, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private String formatMessage(String format, CommandSender sender, String message, String world) {
        String playerName;
        String chatColor = "";
        String prefix = "";
        if (sender instanceof ConsoleCommandSender) {
            playerName = "Console";
        } else {
            Player player = (Player) sender;
            playerName = cargo.getNicknameColor(player.getUniqueId()) + sender.getName();
            chatColor = cargo.getChatColor(player.getUniqueId());
            prefix = cargo.getPrefix(player.getUniqueId());
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{player}", playerName);
        placeholders.put("{message}", chatColor + MessageUtil.sanitizePlayerText(message));
        placeholders.put("{world}", world);
        placeholders.put("{prefix}", prefix);
        return MessageUtil.apply(format, placeholders);
    }
}
