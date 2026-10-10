package com.exemplo.chatplus.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class InsigniasCommand implements CommandExecutor {

    private final InsigniasGui gui;

    public InsigniasCommand(InsigniasGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando precisa ser usado por um jogador.");
            return true;
        }

        gui.open(player);
        return true;
    }
}
