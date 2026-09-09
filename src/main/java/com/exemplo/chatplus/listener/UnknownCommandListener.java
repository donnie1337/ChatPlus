package com.exemplo.chatplus.listener;

import com.exemplo.chatplus.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Method;
import java.util.Locale;

/** Substitui a mensagem padrão de comando desconhecido por uma mensagem do ChatPlus. */
public final class UnknownCommandListener implements Listener {
    private final ConfigManager configManager;
    private final CommandMap commandMap;

    public UnknownCommandListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.commandMap = resolveCommandMap();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
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
        if (command == null && normalizedLabel.contains(":")) {
            command = commandMap.getCommand(normalizedLabel);
        }

        if (command != null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        player.sendMessage(configManager.getMessage("comando-desconhecido"));
    }

    private CommandMap resolveCommandMap() {
        try {
            Object server = Bukkit.getServer();
            Method method = server.getClass().getMethod("getCommandMap");
            method.setAccessible(true);
            Object result = method.invoke(server);
            return result instanceof CommandMap map ? map : null;
        } catch (ReflectiveOperationException | SecurityException | LinkageError exception) {
            Bukkit.getLogger().warning("Não foi possível acessar o CommandMap para tratar comandos desconhecidos: "
                    + exception.getClass().getSimpleName());
            return null;
        }
    }
}
