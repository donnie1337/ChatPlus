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
import org.bukkit.event.server.TabCompleteEvent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/** Centraliza a ocultação de comandos sem permissão para jogadores. */
public final class UnknownCommandListener implements Listener {
    private static final String SERVER_COMMAND_PERMISSION = "chatplus.comandos.servidor";
    private static final String[] PROTECTED_NAMESPACES = {"bukkit", "spigot", "minecraft", "paper"};

    private static final Set<String> PUBLIC_COMMANDS = Set.of(
            "login", "registro", "register", "cadastrar",
            "tpa", "tpaqui", "tpaccept", "tpaceitar", "tpdeny", "tpnegar", "tpacancel", "tpacancelar",
            "home", "homes", "sethome", "delhome",
            "clan", "clans", "pvp", "mcmmo", "habilidades",
            "marry", "casar",
            "terreno", "terrenos", "marcos", "marco",
            "coins", "coin", "money", "banco", "pagar", "topcoins", "coinstop", "baltop"
    );

    private final ConfigManager configManager;
    private final CommandMap commandMap;
    private final CargoPlusBridge cargoPlus = new CargoPlusBridge();

    public UnknownCommandListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.commandMap = resolveCommandMap();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isBlank() || !message.startsWith("/")) return;

        String commandLabel = message.substring(1).trim().split("\\s+", 2)[0];
        if (commandLabel.isBlank()) return;
        String normalizedLabel = commandLabel.toLowerCase(Locale.ROOT);

        if (isGloballyDisabledCommand(normalizedLabel)) {
            hideBukkitCommand(player, event);
            return;
        }

        if (isBukkitCommand(normalizedLabel)) {
            hideBukkitCommand(player, event);
            return;
        }

        if (isProtectedServerCommand(normalizedLabel)) {
            if (!player.hasPermission(SERVER_COMMAND_PERMISSION)) hideCommand(player, event);
            return;
        }

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
        event.getCommands().removeIf(label -> !isVisible(player, label));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;

        String buffer = event.getBuffer();
        if (buffer == null || buffer.isBlank()) return;

        String root = normalizeRoot(buffer);
        if (root == null || !isVisible(player, root)) {
            event.setCompletions(Collections.emptyList());
        }
    }

    private boolean isVisible(Player player, String label) {
        String normalizedLabel = normalizeRoot(label);
        if (normalizedLabel == null || normalizedLabel.indexOf(':') >= 0) return false;

        if (PUBLIC_COMMANDS.contains(normalizedLabel)) return true;
        if (isGloballyDisabledCommand(normalizedLabel)) return false;

        if (isProtectedServerCommand(normalizedLabel)) {
            return player.hasPermission(SERVER_COMMAND_PERMISSION);
        }

        Command command = commandMap == null ? null : commandMap.getCommand(normalizedLabel);
        return command != null && hasPermission(player, command, normalizedLabel);
    }

    private String normalizeRoot(String value) {
        String command = value == null ? "" : value.trim();
        if (command.startsWith("/")) command = command.substring(1);
        int space = command.indexOf(' ');
        if (space >= 0) command = command.substring(0, space);
        if (command.isBlank()) return null;
        return command.toLowerCase(Locale.ROOT);
    }

    private boolean hasPermission(Player player, Command command, String label) {
        String permission = permissionFor(command, label);
        if (permission == null) return true;
        if (player.hasPermission(permission)) return true;

        if ("cor".equalsIgnoreCase(label)) {
            return cargoPlus.hasCargoPermission(player.getUniqueId(), "chatplus.cor");
        }
        return cargoPlus.hasCargoPermission(player.getUniqueId(), permission);
    }

    /**
     * /v e /configurar não declaram mais permission no plugin.yml para impedir
     * que o dispatcher do servidor gere uma segunda resposta de permissão.
     * O ChatPlus mantém aqui as permissões reais desses comandos.
     */
    private String permissionFor(Command command, String label) {
        String normalized = label.toLowerCase(Locale.ROOT);
        if ("l".equals(normalized)) return "chatplus.local";
        if ("g".equals(normalized)) return "chatplus.global";
        if ("s".equals(normalized)) return "chatplus.staff";
        if ("cor".equals(normalized)) return "chatplus.cor";
        if ("v".equals(normalized)) return "essentialsplus.vanish";
        if ("configurar".equals(normalized)) return "utilidadesplus.configurar";

        String permission = command.getPermission();
        if (permission == null || permission.isBlank()) return null;
        return permission;
    }

    private boolean isGloballyDisabledCommand(String label) {
        if (label == null) return false;

        String normalized = label.toLowerCase(Locale.ROOT);

        // Bloqueia todo o namespace vanilla, inclusive /minecraft e
        // qualquer comando namespaced como /minecraft:give ou /minecraft:tp.
        if (normalized.equals("minecraft") || normalized.startsWith("minecraft:")) {
            return true;
        }

        int separator = normalized.indexOf(':');
        String base = separator >= 0 && separator + 1 < normalized.length()
                ? normalized.substring(separator + 1)
                : normalized;

        return base.equals("?") || base.equals("about") || base.equals("me");
    }

    private boolean isBukkitCommand(String label) {
        if (label == null) return false;
        String normalized = label.toLowerCase(Locale.ROOT);
        return normalized.equals("bukkit") || normalized.startsWith("bukkit:");
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
        player.sendMessage(configManager.getMessage("comando-desconhecido"));
    }

    private void hideBukkitCommand(Player player, PlayerCommandPreprocessEvent event) {
        event.setCancelled(true);
        player.sendMessage("§c[Erro] Comando não encontrado.");
    }
}
