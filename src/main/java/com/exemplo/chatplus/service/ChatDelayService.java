package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Controls the per-player chat cooldown without creating repeating tasks.
 * All access is expected to happen on the server's primary thread.
 */
public final class ChatDelayService {

    private final ConfigManager config;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ChatDelayService(ConfigManager config) {
        this.config = config;
    }

    /**
     * Returns true when the sender may send now and reserves the cooldown.
     * Console is never throttled.
     */
    public boolean tryAcquire(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true;
        }

        int delaySeconds = config.getChatDelaySeconds();
        if (delaySeconds <= 0) {
            return true;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = delaySeconds * 1000L;
        Long availableAt = cooldowns.get(player.getUniqueId());

        if (availableAt != null && availableAt > now) {
            return false;
        }

        cooldowns.put(player.getUniqueId(), now + cooldownMillis);

        // Small defensive cleanup; no scheduled task is needed.
        if (cooldowns.size() > 1024) {
            cleanupExpired(now);
        }

        return true;
    }

    /**
     * Clears the player's cooldown when they leave, avoiding stale entries.
     */
    public void clear(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    private void cleanupExpired(long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }
}
