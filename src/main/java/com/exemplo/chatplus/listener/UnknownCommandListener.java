package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.CargoPlusBridge;
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
    private static final String SERVER_COMMAND_PERMISSION = "chatplus.comandos.servidor";
    private static final String[] PROTECTED_NAMESPACES = {"bukkit", "spigot", "minecraft", "paper"};

    private final ConfigManager configManager;
    private final CommandMap commandMap;
    private final CargoPlusBridge cargoPlus = new CargoPlusBridge();

    public UnknownCommandListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.commandMap = resolveCommandMap();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onProtectedServerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isBlank() || !message.startsWith("/")) return;

        String commandLabel = message.substring(1).trim().split("\\s+", 2)[0];
        if (commandLabel.isBlank()) return;
        String normalizedLabel = commandLabel.toLowerCase(Locale.ROOT);
        if (!isProtectedServerCommand(normalizedLabel)) return;
        if (player.hasPermission(SERVER_COMMAND_PERMISSION)) return;

        hideCommand(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isBlank() || !message.startsWith("/")) return;

        String commandLabel = message.substring(1).trim().split("\\s+", 2)[0];
        if (commandLabel.isBlank()) return;
        String normalizedLabel = commandLabel.toLowerCase(Locale.ROOT);

        if (isProtectedServerCommand(normalizedLabel) || event.isCancelled()) return;
        if (commandMap == null) return;

        Command command = commandMap.getCommand(normalizedLabel);
        if (command == null || !hasPermission(player, command, commandLabel)) {
            hideCommand(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        if (commandMap == null) return;

        Player player = event.getPlayer();
        event.getCommands().removeIf(label -> {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            Command command = commandMap.getCommand(normalizedLabel);

            if (isProtectedServerCommand(normalizedLabel)) {
                return !player.hasPermission(SERVER_COMMAND_PERMISSION);
            }

            return command == null || !hasPermission(player, command, label);
        });
    }

    private boolean hasPermission(Player player, Command command, String label) {
        String permission = command.getPermission();
        if (permission == null || permission.isBlank()) permission = fallbackPermission(label);
        if (player.hasPermission(permission)) return true;

        if ("cor".equalsIgnoreCase(label)) {
            return cargoPlus.hasCargoPermission(player.getUniqueId(), "chatplus.cor");
        }
        return cargoPlus.hasCargoPermission(player.getUniqueId(), permission);
    }

    private boolean isProtectedServerCommand(String label) {
        int separator = label.indexOf(':');
        if (separator > 0 && separator < label.length() - 1) {
            String namespace = label.substring(0, separator);
            for (String protectedNamespace : PROTECTED_NAMESPACES) {
                if (protectedNamespace.equals(namespace)) return true;
            }
        }

        return switch (label) {
            case "help", "?", "plugins", "pl", "version", "ver", "about", "bukkit", "spigot",
                 "reload", "rl", "restart", "stop", "save-all", "save-on", "save-off", "trigger" -> true;
            default -> false;
        };
    }

    private String fallbackPermission(String label) {
        return "chatplus.command." + label.toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.');
    }

    private CommandMap resolveCommandMap() {
        try {
            Object server = Bukkit.getServer();
            Method method = server.getClass().getMethod("getCommandMap");
            method.setAccessible(true);
            Object result = method.invoke(server);
            return result instanceof CommandMap map ? map : null;
        } catch (ReflectiveOperationException | SecurityException | LinkageError exception) {
            Bukkit.getLogger().warning("Não foi possível acessar o CommandMap para tratar comandos: "
                    + exception.getClass().getSimpleName());
            return null;
        }
    }

    private void hideCommand(Player player, PlayerCommandPreprocessEvent event) {
        event.setCancelled(true);
        String prefix = configManager.getMessage("prefixo-sistema");
        String message = configManager.getMessage("comando-desconhecido");
        player.sendMessage(prefix + message);
    }
}
