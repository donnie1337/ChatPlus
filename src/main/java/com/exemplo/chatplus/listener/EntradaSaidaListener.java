package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.CargoPlusBridge;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Exibe mensagens fixas de entrada e saída usando a cor oficial do cargo. */
public final class EntradaSaidaListener implements Listener {
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
        String message = formatMessage(player, "entrou no servidor!");
        event.setJoinMessage(message);
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

    private String formatMessage(Player player, String action) {
        String color = cargo.getNicknameColor(player.getUniqueId());
        String prefix = cargo.getPrefix(player.getUniqueId());
        String cleanPrefix = ChatColor.stripColor(MessageUtil.colorize(prefix));
        if (cleanPrefix == null || cleanPrefix.isBlank()) {
            cleanPrefix = "[" + cargo.getGroup(player.getUniqueId()) + "]";
        }
        return color + cleanPrefix + " " + player.getName() + " " + action;
    }
}
