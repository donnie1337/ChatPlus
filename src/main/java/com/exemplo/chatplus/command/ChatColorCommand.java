package com.exemplo.chatplus.command;

import com.exemplo.chatplus.service.ChatColorService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class ChatColorCommand implements CommandExecutor, TabCompleter {
    private final ChatColorService colors;
    private final ChatColorGui gui;

    public ChatColorCommand(ChatColorService colors, ChatColorGui gui) {
        this.colors = colors;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission("sistemautil.cor")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /" + label);
            return true;
        }
        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
