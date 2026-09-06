package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Handles /chat reload during the testing phase. */
public final class ChatCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload");

    private final ConfigManager config;

    public ChatCommand(ConfigManager config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            config.load();
            sender.sendMessage(config.getMessage("reload-sucesso"));
            return true;
        }

        sender.sendMessage(config.getMessage("uso-chat-reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>();
        String typed = args[0].toLowerCase();
        for (String subcommand : SUBCOMMANDS) {
            if (subcommand.startsWith(typed)) {
                suggestions.add(subcommand);
            }
        }
        return suggestions;
    }
}
