package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/** Owns the actual "who receives what, formatted how" logic for all channels. */
public final class ChatService {

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

        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }

        int range = config.getLocalChatRange();
        long rangeSquared = (long) range * range;
        Location senderLocation = sender.getLocation();
        World senderWorld = senderLocation.getWorld();

        String formatted = formatMessage(config.getLocalChatFormat(), sender.getName(), message,
                senderWorld != null ? senderWorld.getName() : "");

        boolean deliveredToAnotherPlayer = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) {
                online.sendMessage(formatted);
                continue;
            }

            World onlineWorld = online.getWorld();
            if (onlineWorld == null || senderWorld == null || !onlineWorld.equals(senderWorld)) {
                continue;
            }

            if (senderLocation.distanceSquared(online.getLocation()) <= rangeSquared) {
                online.sendMessage(formatted);
                deliveredToAnotherPlayer = true;
            }
        }

        if (!deliveredToAnotherPlayer) {
            String alone = config.getMessage("ninguem-por-perto");
            if (!alone.isEmpty()) {
                sender.sendMessage(alone);
            }
        }
    }

    public void sendGlobalMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendGlobalMessage(sender, message));
            return;
        }

        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }

        String formatted = formatMessage(config.getGlobalChatFormat(), resolveSenderName(sender), message, "");
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(formatted);
        }
        notifyConsole(sender, formatted);
    }

    /** Delivers staff chat to every online player during the testing phase. */
    public void sendStaffMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendStaffMessage(sender, message));
            return;
        }

        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }

        String formatted = formatMessage(config.getStaffChatFormat(), resolveSenderName(sender), message, "");
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(formatted);
        }
        notifyConsole(sender, formatted);
    }

    private void notifyConsole(CommandSender sender, String formatted) {
        if (!(sender instanceof ConsoleCommandSender)) {
            Bukkit.getConsoleSender().sendMessage(formatted);
        }
    }

    private String resolveSenderName(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? "Console" : sender.getName();
    }

    private String formatMessage(String format, String player, String message, String world) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{player}", player);
        placeholders.put("{message}", message);
        placeholders.put("{world}", world);
        return MessageUtil.apply(format, placeholders);
    }
}
