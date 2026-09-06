package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.ChatService;
import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles {@code /s <mensagem>}. Requires the standard Bukkit permission
 * {@code chat.staff} both to send and to receive - a sender without the
 * permission is rejected here, and {@link ChatService#sendStaffMessage}
 * only delivers to online players who hold it.
 *
 * <p>Usable from the console, which is always treated as staff.</p>
 */
public final class StaffChatCommand implements CommandExecutor {

    private final ConfigManager config;
    private final ChatService chatService;

    public StaffChatCommand(ConfigManager config, ChatService chatService) {
        this.config = config;
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chat.staff")) {
            sender.sendMessage(config.getMessage("sem-permissao"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(config.getMessage("uso-staff"));
            return true;
        }

        String message = MessageUtil.join(args, 0);
        if (message.isEmpty()) {
            sender.sendMessage(config.getMessage("mensagem-vazia"));
            return true;
        }

        if (!config.isStaffChatEnabled()) {
            sender.sendMessage(config.getMessage("chat-staff-desativado"));
            return true;
        }

        chatService.sendStaffMessage(sender, message);
        return true;
    }
}
