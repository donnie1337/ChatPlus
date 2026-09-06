package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the actual "who receives what, formatted how" logic for all three
 * channels.
 *
 * <p>This class is intentionally the only place that knows how to deliver a
 * message. Both the chat listener (normal typing) and the /l, /g and /s
 * commands call into these same three methods, so there is exactly one code
 * path per channel - no risk of a message being delivered twice, or
 * formatted differently depending on how it was sent.</p>
 *
 * <p>Performance: no Bukkit scheduler tasks are used anywhere here. Local
 * chat does a single pass over the currently online players, filtering by
 * world and by squared distance (avoiding a square root) - see
 * {@link #sendLocalMessage(Player, String)}.</p>
 */
public final class ChatService {

    private final ConfigManager config;

    public ChatService(ConfigManager config) {
        this.config = config;
    }

    /**
     * Delivers a local chat message: only players in the same world as the
     * sender, within the configured range, receive it. The sender always
     * sees their own message, regardless of who else is in range.
     */
    public void sendLocalMessage(Player sender, String message) {
        int range = config.getLocalChatRange();
        long rangeSquared = (long) range * (long) range;

        Location senderLocation = sender.getLocation();
        World senderWorld = senderLocation.getWorld();

        String formatted = formatMessage(config.getLocalChatFormat(), sender.getName(), message,
                senderWorld != null ? senderWorld.getName() : "");

        List<Player> recipients = new ArrayList<>();
        for (Player online : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (online.equals(sender)) {
                recipients.add(online);
                continue;
            }

            World onlineWorld = online.getWorld();
            if (onlineWorld == null || senderWorld == null || !onlineWorld.equals(senderWorld)) {
                continue;
            }

            if (senderLocation.distanceSquared(online.getLocation()) <= rangeSquared) {
                recipients.add(online);
            }
        }

        for (Player recipient : recipients) {
            recipient.sendMessage(formatted);
        }

        // Only the sender itself was in range: let them know, unless the
        // operator disabled this notice by blanking it out in messages.yml.
        if (recipients.size() <= 1) {
            String alone = config.getMessage("ninguem-por-perto");
            if (!alone.isEmpty()) {
                sender.sendMessage(alone);
            }
        }
    }

    /**
     * Delivers a global chat message to every online player. Also accepts
     * the console as a sender (e.g. {@code /g} run from the server console).
     */
    public void sendGlobalMessage(CommandSender sender, String message) {
        String formatted = formatMessage(config.getGlobalChatFormat(), resolveSenderName(sender), message, "");

        for (Player online : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            online.sendMessage(formatted);
        }

        notifyConsole(sender, formatted);
    }

    /**
     * Delivers a staff chat message only to online players holding
     * {@code chat.staff}, plus the console. Accepts the console as a sender
     * as well (the console is always treated as staff).
     */
    public void sendStaffMessage(CommandSender sender, String message) {
        String formatted = formatMessage(config.getStaffChatFormat(), resolveSenderName(sender), message, "");

        for (Player online : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (online.hasPermission("chat.staff")) {
                online.sendMessage(formatted);
            }
        }

        notifyConsole(sender, formatted);
    }

    private void notifyConsole(CommandSender sender, String formatted) {
        // Avoid printing the message to the console twice when the console
        // itself was the sender.
        if (!(sender instanceof ConsoleCommandSender)) {
            Bukkit.getConsoleSender().sendMessage(formatted);
        }
    }

    private String resolveSenderName(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? "Console" : sender.getName();
    }

    /**
     * Builds the placeholder map and applies it to the given format
     * template. This is the single place that knows about {player},
     * {message} and {world}; supporting a new placeholder anywhere in the
     * plugin only requires adding another entry to this map.
     */
    private String formatMessage(String format, String player, String message, String world) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{player}", player);
        placeholders.put("{message}", message);
        placeholders.put("{world}", world);
        return MessageUtil.apply(format, placeholders);
    }
}
