package com.exemplo.chatplus.command;

import com.exemplo.chatplus.service.CargoPlusBridge;
import com.exemplo.chatplus.service.PlaytimeTestService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class InsigniasCommand implements TabExecutor {

    private final InsigniasGui gui;
    private final PlaytimeTestService playtimeTests;
    private final CargoPlusBridge cargo = new CargoPlusBridge();

    public InsigniasCommand(InsigniasGui gui, PlaytimeTestService playtimeTests) {
        this.gui = gui;
        this.playtimeTests = playtimeTests;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando precisa ser usado por um jogador.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("horas")) {
            if (!isDev(player)) {
                player.sendMessage("§c[Erro] §cApenas o cargo Dev pode usar este comando.");
                return true;
            }

            if (args.length != 2) {
                player.sendMessage("§e[Chat] §fUse: §e/" + label + " horas <horas|limpar>");
                return true;
            }

            if (args[1].equalsIgnoreCase("limpar")) {
                playtimeTests.clear(player);
                player.sendMessage("§e[Chat] §aTeste de horas removido. O tempo real voltou a ser usado.");
                gui.openOwned(player);
                return true;
            }

            long hours;
            try {
                hours = Long.parseLong(args[1]);
            } catch (NumberFormatException ignored) {
                player.sendMessage("§c[Erro] §cInforme uma quantidade válida de horas.");
                return true;
            }

            if (hours < 0 || hours > 100_000) {
                player.sendMessage("§c[Erro] §cUse um valor entre 0 e 100000 horas.");
                return true;
            }

            playtimeTests.setHours(player, hours);
            player.sendMessage("§e[Chat] §aTempo de teste definido para §f" + hours + "h§a.");
            gui.openOwned(player);
            return true;
        }

        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !isDev(player)) return List.of();

        if (args.length == 1) {
            return List.of("horas").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("horas")) {
            return List.of("120", "240", "360", "480", "600", "720", "limpar").stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return List.of();
    }

    private boolean isDev(Player player) {
        return player != null && "dev".equalsIgnoreCase(cargo.getGroup(player.getUniqueId()));
    }
}
