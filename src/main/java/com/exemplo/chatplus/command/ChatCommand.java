package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles {@code /chat reload} and its tab completion. A list of
 * subcommands is kept instead of a single hardcoded string so future
 * subcommands (e.g. {@code /chat version}) only require adding an entry to
 * {@link #SUBCOMMANDS} and a branch in {@link #onCommand}.
 *
 * <p>Requires {@code chat.admin} - a plain Bukkit permission node, not a
 * rank or group defined by this plugin.</p>
 */
public final class ChatCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload");

    private final ConfigManager config;

    public ChatCommand(ConfigManager config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chat.admin")) {
                sender.sendMessage(config.getMessage("sem-permissao"));
                return true;
            }
            config.load();
            sender.sendMessage(config.getMessage("reload-sucesso"));
            return true;
        }

        sender.sendMessage(config.getMessage("uso-chat-reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !sender.hasPermission("chat.admin")) {
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
