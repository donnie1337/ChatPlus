package com.exemplo.chatplus.service;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Override temporário de tempo jogado usado apenas para testes de insígnias.
 * Não altera a estatística PLAY_ONE_MINUTE salva pelo Minecraft.
 */
public final class PlaytimeTestService {

    private final Map<UUID, Long> forcedHours = new ConcurrentHashMap<>();

    public long resolveHours(Player player) {
        if (player == null) return 0L;
        Long forced = forcedHours.get(player.getUniqueId());
        if (forced != null) return Math.max(0L, forced);
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L / 60L;
    }

    public boolean hasOverride(Player player) {
        return player != null && forcedHours.containsKey(player.getUniqueId());
    }

    public void setHours(Player player, long hours) {
        if (player == null) return;
        forcedHours.put(player.getUniqueId(), Math.max(0L, hours));
    }

    public void clear(Player player) {
        if (player == null) return;
        forcedHours.remove(player.getUniqueId());
    }
}
