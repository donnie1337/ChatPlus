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

/** Centraliza a ocultação de comandos sem permissão para jogadores. */
public final class UnknownCommandListener implements Listener {
    private static final String FALLBACK_PERMISSION_PREFIX = "chatplus.command.";

    private final ConfigManager configManager;
    private final CommandMap commandMap;

    public UnknownCommandListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.commandMap = resolveCommandMap();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isBlank() || !message.startsWith("/") || commandMap == null) {
            return;
        }

        String commandLabel = message.substring(1).trim().split("\\s+", 2)[0];
        if (commandLabel.isBlank()) {
            return;
        }

        Command command = commandMap.getCommand(commandLabel.toLowerCase(Locale.ROOT));
        if (command == null || !hasPermission(player, command, commandLabel)) {
            hideCommand(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (commandMap == null) {
            return;
        }

        event.getCommands().removeIf(label -> {
            Command command = commandMap.getCommand(label.toLowerCase(Locale.ROOT));
            return command == null || !hasPermission(player, command, label);
        });
    }

    private boolean hasPermission(Player player, Command command, String label) {
        String permission = command.getPermission();
        if (permission == null || permission.isBlank()) {
            permission = fallbackPermission(label);
        }
        return player.hasPermission(permission);
    }

    private String fallbackPermission(String label) {
        String normalized = label.toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.');
        return FALLBACK_PERMISSION_PREFIX + normalized;
    }

    private void hideCommand(Player player, PlayerCommandPreprocessEvent event) {
        event.setCancelled(true);
        String prefix = configManager.getMessage("prefixo-sistema");
        String message = configManager.getMessage("comando-desconhecido");
        player.sendMessage(prefix + message);
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
