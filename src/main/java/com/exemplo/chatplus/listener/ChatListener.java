package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Intercepts the vanilla Spigot chat event and turns every normal message a
 * player types into a local chat message.
 *
 * <p>The event may be asynchronous. Therefore this listener does not touch
 * the Bukkit API beyond the event data itself: it cancels the event, captures
 * the immutable input it needs, and delegates the actual delivery to the
 * server's main thread through {@link ChatService}.</p>
 */
public final class ChatListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final ChatService chatService;

    public ChatListener(JavaPlugin plugin, ConfigManager config, ChatService chatService) {
        this.plugin = plugin;
        this.config = config;
        this.chatService = chatService;
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

        // AsyncPlayerChatEvent can run off the primary thread. Only capture
        // event data here; all Bukkit state access happens on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (!config.isLocalChatEnabled()) {
                player.sendMessage(config.getMessage("chat-local-desativado"));
                return;
            }

            chatService.sendLocalMessage(player, message);
        });
    }
}
