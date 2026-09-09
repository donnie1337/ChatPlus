package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

/** Oculta comandos e detalhes de comandos para jogadores que não fazem parte da staff. */
public final class UnknownCommandListener implements Listener {
    private static final Set<String> HIDDEN_INFORMATION_COMMANDS = Set.of(
            "help", "?", "plugins", "pl", "bukkit:help", "bukkit:plugins"
    );

    private final ConfigManager configManager;
    private final CommandMap commandMap;

    public UnknownCommandListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.commandMap = resolveCommandMap();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isStaff(player)) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.isBlank() || !message.startsWith("/")) {
            return;
        }

        String commandLabel = message.substring(1).trim().split("\\s+", 2)[0];
        if (commandLabel.isBlank() || commandMap == null) {
            return;
        }

        String normalizedLabel = commandLabel.toLowerCase(Locale.ROOT);
        Command command = commandMap.getCommand(normalizedLabel);

        // Comandos de descoberta nunca devem revelar que existem para jogadores comuns.
        if (HIDDEN_INFORMATION_COMMANDS.contains(normalizedLabel)) {
            hideCommand(player, event);
            return;
        }

        // Se o jogador não pode executar o comando, ele recebe exatamente a mesma resposta
        // de um comando inexistente, sem revelar permissão, uso, aliases ou detalhes internos.
        if (command != null && !command.testPermissionSilent(player)) {
            hideCommand(player, event);
            return;
        }

        // Comandos inexistentes continuam sendo tratados pelo mesmo padrão.
        if (command == null) {
            hideCommand(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (isStaff(player) || commandMap == null) {
            return;
        }

        event.getCommands().removeIf(label -> {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            if (HIDDEN_INFORMATION_COMMANDS.contains(normalizedLabel)) {
                return true;
            }

            Command command = commandMap.getCommand(normalizedLabel);
            return command != null && !command.testPermissionSilent(player);
        });
    }

    private void hideCommand(Player player, PlayerCommandPreprocessEvent event) {
        event.setCancelled(true);
        String prefix = configManager.getMessage("prefixo-sistema");
        String message = configManager.getMessage("comando-desconhecido");
        player.sendMessage(prefix + message);
    }

    private boolean isStaff(Player player) {
        return player.isOp()
                || player.hasPermission("chatplus.staff")
                || player.hasPermission("chatplus.admin");
    }

    private CommandMap resolveCommandMap() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            Object result = method.invoke(Bukkit.getServer());
            return result instanceof CommandMap map ? map : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            Bukkit.getLogger().warning("Não foi possível acessar o CommandMap para tratar comandos ocultos.");
            return null;
        }
    }
}
