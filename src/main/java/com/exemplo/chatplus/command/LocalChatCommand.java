package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatService;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        if (!config.isMessageLengthValid(message)) {
            sender.sendMessage(config.getMessage("mensagem-muito-longa"));
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
