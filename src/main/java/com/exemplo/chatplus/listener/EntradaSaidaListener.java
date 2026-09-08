package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.CargoPlusBridge;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Exibe mensagens de entrada e saída usando a cor oficial do cargo. */
public final class EntradaSaidaListener implements Listener {
    private static final String DEFAULT_JOIN_MESSAGE = "entrou no servidor!";

    private final ConfigManager config;
    private final CargoPlusBridge cargo = new CargoPlusBridge();

    public EntradaSaidaListener(ConfigManager config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!config.isJoinMessageEnabled()) {
            event.setJoinMessage(null);
            return;
        }

        Player player = event.getPlayer();
        event.setJoinMessage(formatMessage(player, getRandomJoinMessage(player)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!config.isQuitMessageEnabled()) {
            event.setQuitMessage(null);
            return;
        }

        Player player = event.getPlayer();
        event.setQuitMessage(formatMessage(player, "saiu do servidor!"));
    }

    private String getRandomJoinMessage(Player player) {
        List<String> messages = config.getPlugin().getConfig().getStringList("entrada.mensagens");
        List<String> validMessages = messages.stream()
                .filter(message -> message != null && !message.isBlank())
                .toList();

        if (validMessages.isEmpty()) {
            return DEFAULT_JOIN_MESSAGE;
        }

        String message = validMessages.get(ThreadLocalRandom.current().nextInt(validMessages.size()));
        return message.replace("{player}", player.getName());
    }

    private String formatMessage(Player player, String action) {
        String color = cargo.getNicknameColor(player.getUniqueId());
        String prefix = cargo.getPrefix(player.getUniqueId());
        String cleanPrefix = ChatColor.stripColor(MessageUtil.colorize(prefix));
        if (cleanPrefix == null || cleanPrefix.isBlank()) {
            cleanPrefix = "[" + cargo.getGroup(player.getUniqueId()) + "]";
        }
        return color + cleanPrefix + " " + player.getName() + " " + MessageUtil.colorize(action);
    }
}
