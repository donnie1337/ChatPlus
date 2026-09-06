package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatDelayService;
import com.exemplo.chatplus.service.ChatService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Intercepts the vanilla Spigot chat event and routes normal chat through the
 * local channel on the server's primary thread.
 */
public final class ChatListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final ChatService chatService;
    private final ChatDelayService delayService;

    public ChatListener(JavaPlugin plugin, ConfigManager config, ChatService chatService,
                        ChatDelayService delayService) {
        this.plugin = plugin;
        this.config = config;
        this.chatService = chatService;
        this.delayService = delayService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // We own delivery completely; Spigot must never broadcast this event.
        event.setCancelled(true);

        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }

        // Capture only immutable event data here. Bukkit state/config access
        // is deferred to the primary thread because this event may be async.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (!config.isMessageLengthValid(message)) {
                player.sendMessage(config.getMessage("mensagem-muito-longa"));
                return;
            }

            if (!config.isLocalChatEnabled()) {
                player.sendMessage(config.getMessage("chat-local-desativado"));
                return;
            }

            chatService.sendLocalMessage(player, message);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        delayService.clear(event.getPlayer());
    }
}
