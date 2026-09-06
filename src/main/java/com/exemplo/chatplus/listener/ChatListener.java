package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Intercepts the vanilla Spigot chat event and turns every normal message a
 * player types into a local chat message.
 *
 * <p>The event is <b>always</b> cancelled here so Spigot never broadcasts it
 * on its own: this plugin takes complete, exclusive control over who
 * receives a "normal" chat message, and the actual delivery is delegated to
 * {@link ChatService#sendLocalMessage(Player, String)} - the very same
 * method {@code /l} uses - so a message can never be sent twice or
 * formatted inconsistently depending on how it was typed.</p>
 *
 * <p>{@code priority = HIGHEST, ignoreCancelled = true}: this listener runs
 * after every other plugin's chat listener, and does nothing if the event
 * was already cancelled by then (for example by a mute plugin) - it never
 * overrides another plugin's decision to silence a message.</p>
 */
public final class ChatListener implements Listener {

    private final ConfigManager config;
    private final ChatService chatService;

    public ChatListener(ConfigManager config, ChatService chatService) {
        this.config = config;
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Cancel unconditionally: from this point on, Spigot's own chat
        // broadcast must never run - we own delivery completely.
        event.setCancelled(true);

        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }

        if (!config.isLocalChatEnabled()) {
            player.sendMessage(config.getMessage("chat-local-desativado"));
            return;
        }

        chatService.sendLocalMessage(player, message);
    }
}
