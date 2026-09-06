package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatService;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles {@code /g <mensagem>}. Requires the standard Bukkit permission
 * {@code chat.global} - this plugin never defines its own permission
 * system, it only calls {@link CommandSender#hasPermission(String)}, so a
 * future permissions plugin can grant/deny this node freely.
 *
 * <p>Usable from the console: a {@link org.bukkit.command.ConsoleCommandSender}
 * always evaluates {@code hasPermission} as {@code true}.</p>
 */
public final class GlobalChatCommand implements CommandExecutor {

    private final ConfigManager config;
    private final ChatService chatService;

    public GlobalChatCommand(ConfigManager config, ChatService chatService) {
        this.config = config;
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chat.global")) {
            sender.sendMessage(config.getMessage("sem-permissao"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(config.getMessage("uso-global"));
            return true;
        }

        String message = MessageUtil.join(args, 0);
        if (message.isEmpty()) {
            sender.sendMessage(config.getMessage("mensagem-vazia"));
            return true;
        }

        if (!config.isGlobalChatEnabled()) {
            sender.sendMessage(config.getMessage("chat-global-desativado"));
            return true;
        }

        chatService.sendGlobalMessage(sender, message);
        return true;
    }
}
