package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.util.MessageUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatService {
    private static final String PREFIX_PLACEHOLDER = "{prefix}";
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String STAFF_PERMISSION = "chatplus.staff";
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String VANISH_SUFFIX_MARKER = "§0§0§0[ESSENTIALSPLUS_VANISH_HOVER]";
    private static final String VANISH_SUFFIX = "§l§x§F§F§F§F§F§F[§x§F§B§F§B§F§Bɪ§x§F§7§F§7§F§7ɴ§x§F§4§F§4§F§4ᴠ§x§F§0§F§0§F§0ɪ§x§E§C§E§C§E§C§s§x§E§8§E§8§E§8ɪ§x§E§4§E§4§E§4ᴠ§x§E§1§E§1§E§1ᴇ§x§D§D§D§D§D§Dʟ§x§D§9§D§9§D§9]";
    private static final String VANISH_HOVER_TEXT = "§fEste jogador está invisível.";
    private final ConfigManager config;
    private final ChatDelayService delayService;
    private final CargoPlusBridge cargo;
    private volatile Plugin authPlugin;
    private volatile Method authCheckMethod;
    private volatile Plugin vanishPlugin;
    private volatile Method vanishCheckMethod;

    public ChatService(ConfigManager config, ChatDelayService delayService) {
        this.config = config;
        this.delayService = delayService;
        this.cargo = new CargoPlusBridge();
    }

    public void sendLocalMessage(Player sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendLocalMessage(sender, message));
            return;
        }
        if (!isAuthenticated(sender)) {
            sender.sendMessage(config.getMessage("nao-autenticado"));
            return;
        }
        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }
        int range = config.getLocalChatRange();
        long rangeSquared = (long) range * range;
        Location senderLocation = sender.getLocation();
        World senderWorld = senderLocation.getWorld();
        boolean senderVanished = isVanished(sender);
        boolean deliveredToAnotherPlayer = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) {
                online.spigot().sendMessage(formatMessage(config.getLocalChatFormat(), sender, message, senderWorld != null ? senderWorld.getName() : "", online, senderVanished));
                continue;
            }
            boolean recipientVanished = isVanished(online);
            World onlineWorld = online.getWorld();
            if (onlineWorld == null || senderWorld == null || !onlineWorld.equals(senderWorld)) continue;
            if (senderLocation.distanceSquared(online.getLocation()) <= rangeSquared) {
                online.spigot().sendMessage(formatMessage(config.getLocalChatFormat(), sender, message, senderWorld.getName(), online, senderVanished));
                if (!(!senderVanished && recipientVanished)) deliveredToAnotherPlayer = true;
            }
        }
        if (!deliveredToAnotherPlayer) {
            String alone = config.getMessage("ninguem-por-perto");
            if (!alone.isEmpty()) sender.sendMessage(alone);
        }
    }

    public void sendGlobalMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendGlobalMessage(sender, message));
            return;
        }
        if (sender instanceof Player player && !isAuthenticated(player)) {
            sender.sendMessage(config.getMessage("nao-autenticado"));
            return;
        }
        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOnline() && isAuthenticated(online)) {
                online.spigot().sendMessage(formatMessage(config.getGlobalChatFormat(), sender, message, "", online, sender instanceof Player player && isVanished(player)));
            }
        }
        BaseComponent[] consoleFormatted = formatMessage(config.getGlobalChatFormat(), sender, message, "", null, false);
        notifyConsole(sender, TextComponent.toLegacyText(consoleFormatted));
    }

    public void sendStaffMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendStaffMessage(sender, message));
            return;
        }
        if (sender instanceof Player player && !isAuthenticated(player)) {
            sender.sendMessage(config.getMessage("nao-autenticado"));
            return;
        }
        if (!delayService.tryAcquire(sender)) {
            sender.sendMessage(config.getMessage("chat-em-delay"));
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(STAFF_PERMISSION) && isAuthenticated(online)) {
                online.spigot().sendMessage(formatMessage(config.getStaffChatFormat(), sender, message, "", online, sender instanceof Player player && isVanished(player)));
            }
        }
        BaseComponent[] consoleFormatted = formatMessage(config.getStaffChatFormat(), sender, message, "", null, false);
        notifyConsole(sender, TextComponent.toLegacyText(consoleFormatted));
    }

    public void sendStaffSystemMessage(Player sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendStaffSystemMessage(sender, message));
            return;
        }
        if (sender == null || !sender.isOnline() || message == null || message.isEmpty()) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(STAFF_PERMISSION) && isAuthenticated(online)) {
                online.spigot().sendMessage(formatMessage(config.getStaffChatFormat(), sender, message, "", online, false));
            }
        }
        BaseComponent[] consoleFormatted = formatMessage(config.getStaffChatFormat(), sender, message, "", null, false);
        notifyConsole(sender, TextComponent.toLegacyText(consoleFormatted));
    }

    private void notifyConsole(CommandSender sender, String formatted) {
        if (!(sender instanceof ConsoleCommandSender)) Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private boolean isAuthenticated(Player player) {
        if (player == null || !player.isOnline()) return false;
        Plugin auth = Bukkit.getPluginManager().getPlugin("LoginPlus");
        if (auth == null || !auth.isEnabled()) {
            authPlugin = null;
            authCheckMethod = null;
            return false;
        }
        Method method = authCheckMethod;
        if (authPlugin != auth || method == null) {
            synchronized (this) {
                if (authPlugin != auth || authCheckMethod == null) {
                    try {
                        authPlugin = auth;
                        authCheckMethod = auth.getClass().getMethod("isAuthenticated", Player.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        authPlugin = auth;
                        authCheckMethod = null;
                    }
                }
                method = authCheckMethod;
            }
        }
        if (method == null) return false;
        try {
            Object result = method.invoke(auth, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private boolean isVanished(Player player) {
        if (player == null || !player.isOnline()) return false;
        Plugin vanish = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
        if (vanish == null || !vanish.isEnabled()) {
            vanishPlugin = null;
            vanishCheckMethod = null;
            return false;
        }
        Method method = vanishCheckMethod;
        if (vanishPlugin != vanish || method == null) {
            synchronized (this) {
                if (vanishPlugin != vanish || vanishCheckMethod == null) {
                    try {
                        vanishPlugin = vanish;
                        vanishCheckMethod = vanish.getClass().getMethod("isVanished", Player.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        vanishPlugin = vanish;
                        vanishCheckMethod = null;
                    }
                }
                method = vanishCheckMethod;
            }
        }
        if (method == null) return false;
        try {
            Object result = method.invoke(vanish, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private BaseComponent[] formatMessage(String format, CommandSender sender, String message, String world, Player viewer, boolean senderVanished) {
        String playerName;
        String chatColor = "";
        String prefix = "";
        String clanTag = "";
        String group = "desconhecido";
        boolean addVanishHover = false;
        if (sender instanceof ConsoleCommandSender) {
            playerName = "Console";
        } else {
            Player player = (Player) sender;
            playerName = cargo.getNicknameColor(player.getUniqueId()) + sender.getName();
            clanTag = cargo.getClanTag(player.getUniqueId());
            if (senderVanished && viewer != null && viewer.hasPermission(VANISH_PERMISSION)) {
                playerName += " " + VANISH_SUFFIX_MARKER;
                addVanishHover = true;
            }
            chatColor = cargo.getChatColor(player.getUniqueId());
            prefix = cargo.getPrefix(player.getUniqueId());
            group = cargo.getGroup(player.getUniqueId());
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{player}", playerName);
        placeholders.put(TAG_PLACEHOLDER, clanTag);
        placeholders.put("{message}", chatColor + MessageUtil.sanitizePlayerText(message));
        placeholders.put("{world}", world);

        String template = MessageUtil.colorize(format == null ? "" : format);
        if (sender instanceof Player && template.contains(PREFIX_PLACEHOLDER)) {
            String before = template.substring(0, template.indexOf(PREFIX_PLACEHOLDER));
            String after = template.substring(template.indexOf(PREFIX_PLACEHOLDER) + PREFIX_PLACEHOLDER.length());
            placeholders.put(PREFIX_PLACEHOLDER, "");
            before = MessageUtil.apply(before, placeholders);
            after = MessageUtil.apply(after, placeholders);

            List<BaseComponent> components = new ArrayList<>();
            addLegacy(components, before);

            BaseComponent[] prefixComponents = TextComponent.fromLegacyText(MessageUtil.colorize(prefix));
            String hoverGroup = capitalizeGroupName(group);
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§fCargo: §e" + hoverGroup).create());
            for (BaseComponent component : prefixComponents) {
                component.setHoverEvent(hover);
                components.add(component);
            }

            if (!clanTag.isEmpty()) addClanTag(components, clanTag);

            if (addVanishHover) {
                addLegacyWithVanishHover(components, after);
            } else {
                addLegacy(components, after);
            }
            return components.toArray(BaseComponent[]::new);
        }

        placeholders.put(PREFIX_PLACEHOLDER, prefix);
        String formatted = MessageUtil.apply(template, placeholders);
        if (addVanishHover) return parseWithVanishHover(formatted);
        return TextComponent.fromLegacyText(formatted);
    }

    private void addClanTag(List<BaseComponent> components, String tag) {
        if (tag == null || tag.isEmpty()) return;
        String firstColor = firstTagColor(tag);
        if (firstColor.isEmpty()) firstColor = "§f";
        addLegacy(components, firstColor + "[");
        addLegacy(components, tag);
        addLegacy(components, firstColor + "]");
    }

    private String firstTagColor(String tag) {
        for (int i = 0; i + 1 < tag.length(); i++) {
            if (tag.charAt(i) == '§') {
                char code = tag.charAt(i + 1);
                if ("0123456789abcdefABCDEF".indexOf(code) >= 0) return "§" + code;
            }
        }
        return "";
    }

    private String capitalizeGroupName(String group) {
        if (group == null || group.isBlank()) return "Desconhecido";
        String normalized = group.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private void addLegacyWithVanishHover(List<BaseComponent> components, String text) {
        int markerIndex = text.indexOf(VANISH_SUFFIX_MARKER);
        if (markerIndex < 0) {
            addLegacy(components, text);
            return;
        }
        addLegacy(components, text.substring(0, markerIndex));
        addVanishComponents(components);
        addLegacy(components, text.substring(markerIndex + VANISH_SUFFIX_MARKER.length()));
    }

    private BaseComponent[] parseWithVanishHover(String formatted) {
        List<BaseComponent> components = new ArrayList<>();
        addLegacyWithVanishHover(components, formatted);
        return components.toArray(BaseComponent[]::new);
    }

    private void addVanishComponents(List<BaseComponent> components) {
        BaseComponent[] vanishComponents = TextComponent.fromLegacyText(VANISH_SUFFIX);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(VANISH_HOVER_TEXT).create());
        for (BaseComponent component : vanishComponents) {
            component.setHoverEvent(hover);
            components.add(component);
        }
    }

    private void addLegacy(List<BaseComponent> components, String text) {
        if (text == null || text.isEmpty()) return;
        BaseComponent[] parsed = TextComponent.fromLegacyText(text);
        for (BaseComponent component : parsed) components.add(component);
    }
}
