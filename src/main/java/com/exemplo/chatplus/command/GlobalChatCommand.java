package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatService;
import com.exemplo.chatplus.service.CargoPlusBridge;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class GlobalChatCommand implements CommandExecutor {

    private final ConfigManager config;
    private final ChatService chatService;
    private final CargoPlusBridge cargoPlus = new CargoPlusBridge();

    public GlobalChatCommand(ConfigManager config, ChatService chatService) {
        this.config = config;
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "chatplus.global")) {
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

        if (!config.isMessageLengthValid(message)) {
            sender.sendMessage(config.getMessage("mensagem-muito-longa"));
            return true;
        }

        if (!config.isGlobalChatEnabled()) {
            sender.sendMessage(config.getMessage("chat-global-desativado"));
            return true;
        }

        chatService.sendGlobalMessage(sender, message);
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        if (sender instanceof org.bukkit.entity.Player player) {
            return cargoPlus.hasCargoPermission(player.getUniqueId(), permission);
        }
        return false;
    }
}
