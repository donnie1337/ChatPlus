package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatService;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles {@code /l <mensagem>}. Always routes through the local chat,
 * independently of whatever the player types normally in chat - both paths
 * end up calling {@link ChatService#sendLocalMessage(Player, String)}.
 *
 * <p>Requires a physical location to compute distance, so - unlike
 * {@code /g} and {@code /s} - it cannot be used from the console.</p>
 */
public final class LocalChatCommand implements CommandExecutor {

    private final ConfigManager config;
    private final ChatService chatService;

    public LocalChatCommand(ConfigManager config, ChatService chatService) {
        this.config = config;
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("apenas-jogadores"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(config.getMessage("uso-local"));
            return true;
        }

        String message = MessageUtil.join(args, 0);
        if (message.isEmpty()) {
            sender.sendMessage(config.getMessage("mensagem-vazia"));
            return true;
        }

        if (!config.isLocalChatEnabled()) {
            sender.sendMessage(config.getMessage("chat-local-desativado"));
            return true;
        }

        chatService.sendLocalMessage(player, message);
        return true;
    }
}
