package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.util.MessageUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatService {
    private static final String PREFIX_PLACEHOLDER = "{prefix}";
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String PLAYER_PLACEHOLDER = "{player}";
    private static final String MESSAGE_PLACEHOLDER = "{message}";
    private static final String HABILIDADE_TAG_PLACEHOLDER = "{habilidade_tag}";
    private static final DateTimeFormatter MESSAGE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String STAFF_PERMISSION = "chatplus.staff";
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String VANISH_SUFFIX_MARKER = "§0§0§0[ESSENTIALSPLUS_VANISH_HOVER]";
    private static final String VANISH_SUFFIX = "§l§x§F§F§F§F§F§F[§x§F§B§F§B§F§Bɪ§x§F§7§F§7§F§7ɴ§x§F§4§F§4§F§4ᴠ§x§F§0§F§0§F§0ɪ§x§E§C§E§C§E§Cs§x§E§8§E§8§E§8ɪ§x§E§4§E§4§E§4ᴠ§x§E§1§E§1§E§1ᴇ§x§D§D§D§D§D§Dʟ§x§D§9§D§9§D§9]";
    private static final String VANISH_HOVER_TEXT = "§fEste jogador está invisível.";
    private final ConfigManager config;
    private final ChatDelayService delayService;
    private final CargoPlusBridge cargo;
    private volatile Plugin authPlugin;
    private volatile Method authCheckMethod;
    private volatile Method registrationDateMethod;
    private volatile Plugin habilidadesPlugin;
    private volatile Method top1SkillNameMethod;
    private volatile Plugin economiaPlugin;
    private volatile Method magnataTagMethod;
    private volatile Plugin vanishPlugin;
    private volatile Method vanishCheckMethod;
    private volatile Plugin marriagePlugin;
    private volatile Method marriageTagMethod;
    private volatile Method marriagePartnerNameMethod;
    private volatile Method marriagePartnerUuidMethod;

    public ChatService(ConfigManager config, ChatDelayService delayService) {
        this.config = config;
        this.delayService = delayService;
        this.cargo = new CargoPlusBridge();
    }

    public void sendLocalMessage(Player sender, String message) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendLocalMessage(sender, message)); return; }
        if (!isAuthenticated(sender)) { sender.sendMessage(config.getMessage("nao-autenticado")); return; }
        if (!delayService.tryAcquire(sender)) { sender.sendMessage(config.getMessage("chat-em-delay")); return; }
        int range = config.getLocalChatRange();
        long rangeSquared = (long) range * range;
        Location senderLocation = sender.getLocation();
        World senderWorld = senderLocation.getWorld();
        boolean senderVanished = isVanished(sender);
        boolean deliveredToAnotherPlayer = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) {
                online.spigot().sendMessage(formatMessage(config.getLocalChatFormat(), sender, message, senderWorld != null ? senderWorld.getName() : "", online, senderVanished, "Local"));
                continue;
            }
            boolean recipientVanished = isVanished(online);
            World onlineWorld = online.getWorld();
            if (onlineWorld == null || senderWorld == null || !onlineWorld.equals(senderWorld)) continue;
            if (senderLocation.distanceSquared(online.getLocation()) <= rangeSquared) {
                online.spigot().sendMessage(formatMessage(config.getLocalChatFormat(), sender, message, senderWorld.getName(), online, senderVanished, "Local"));
                if (!(!senderVanished && recipientVanished)) deliveredToAnotherPlayer = true;
            }
        }
        if (!deliveredToAnotherPlayer) {
            String alone = config.getMessage("ninguem-por-perto");
            if (!alone.isEmpty()) sender.sendMessage(alone);
        }
    }

    public void sendGlobalMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendGlobalMessage(sender, message)); return; }
        if (sender instanceof Player player && !isAuthenticated(player)) { sender.sendMessage(config.getMessage("nao-autenticado")); return; }
        if (!delayService.tryAcquire(sender)) { sender.sendMessage(config.getMessage("chat-em-delay")); return; }
        for (Player online : Bukkit.getOnlinePlayers()) if (online.isOnline() && isAuthenticated(online)) online.spigot().sendMessage(formatMessage(config.getGlobalChatFormat(), sender, message, "", online, sender instanceof Player player && isVanished(player), "Global"));
        BaseComponent[] consoleFormatted = formatMessage(config.getGlobalChatFormat(), sender, message, "", null, false, "Global");
        notifyConsole(sender, TextComponent.toLegacyText(consoleFormatted));
    }

    public void sendStaffMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendStaffMessage(sender, message)); return; }
        if (sender instanceof Player player && !isAuthenticated(player)) { sender.sendMessage(config.getMessage("nao-autenticado")); return; }
        if (!delayService.tryAcquire(sender)) { sender.sendMessage(config.getMessage("chat-em-delay")); return; }
        for (Player online : Bukkit.getOnlinePlayers()) if (online.hasPermission(STAFF_PERMISSION) && isAuthenticated(online)) online.spigot().sendMessage(formatMessage(config.getStaffChatFormat(), sender, message, "", online, sender instanceof Player player && isVanished(player), "Staff"));
        BaseComponent[] consoleFormatted = formatMessage(config.getStaffChatFormat(), sender, message, "", null, false, "Staff");
        notifyConsole(sender, TextComponent.toLegacyText(consoleFormatted));
    }

    public void sendSystemGlobalMessage(String message) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendSystemGlobalMessage(message)); return; }
        if (message == null || message.isEmpty() || !config.isGlobalChatEnabled()) return;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{prefix}", "");
        placeholders.put("{player}", "");
        placeholders.put("{tag}", "");
        placeholders.put("{habilidade_tag}", "");
        placeholders.put("{message}", message);
        String formatted = MessageUtil.apply(config.getGlobalSystemChatFormat(), placeholders);
        BaseComponent[] components = TextComponent.fromLegacyText(formatted);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOnline() && isAuthenticated(online)) online.spigot().sendMessage(components);
        }
        Bukkit.getConsoleSender().sendMessage(TextComponent.toLegacyText(components));
    }

    public void sendStaffSystemMessage(Player sender, String message) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(config.getPlugin(), () -> sendStaffSystemMessage(sender, message)); return; }
        if (sender == null || !sender.isOnline() || message == null || message.isEmpty()) return;
        for (Player online : Bukkit.getOnlinePlayers()) if (online.hasPermission(STAFF_PERMISSION) && isAuthenticated(online)) online.spigot().sendMessage(formatMessage(config.getStaffChatFormat(), sender, message, "", null, false, "Staff"));
        BaseComponent[] consoleFormatted = formatMessage(config.getStaffChatFormat(), sender, message, "", null, false, "Staff");
        notifyConsole(sender, TextComponent.toLegacyText(consoleFormatted));
    }

    private void notifyConsole(CommandSender sender, String formatted) {
        if (!(sender instanceof ConsoleCommandSender)) Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private boolean isAuthenticated(Player player) {
        if (player == null || !player.isOnline()) return false;
        Plugin auth = Bukkit.getPluginManager().getPlugin("LoginPlus");
        if (auth == null || !auth.isEnabled()) { authPlugin = null; authCheckMethod = null; return false; }
        Method method = authCheckMethod;
        if (authPlugin != auth || method == null) synchronized (this) {
            if (authPlugin != auth || authCheckMethod == null) try {
                authPlugin = auth;
                authCheckMethod = auth.getClass().getMethod("isAuthenticated", Player.class);
            } catch (ReflectiveOperationException | LinkageError ex) {
                authPlugin = auth;
                authCheckMethod = null;
            }
            method = authCheckMethod;
        }
        if (method == null) return false;
        try { Object result = method.invoke(auth, player); return result instanceof Boolean && (Boolean) result; }
        catch (ReflectiveOperationException | LinkageError ex) { return false; }
    }

    private boolean isVanished(Player player) {
        if (player == null || !player.isOnline()) return false;
        Plugin vanish = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
        if (vanish == null || !vanish.isEnabled()) { vanishPlugin = null; vanishCheckMethod = null; return false; }
        Method method = vanishCheckMethod;
        if (vanishPlugin != vanish || method == null) synchronized (this) {
            if (vanishPlugin != vanish || vanishCheckMethod == null) try {
                vanishPlugin = vanish;
                vanishCheckMethod = vanish.getClass().getMethod("isVanished", Player.class);
            } catch (ReflectiveOperationException | LinkageError ex) {
                vanishPlugin = vanish;
                vanishCheckMethod = null;
            }
            method = vanishCheckMethod;
        }
        if (method == null) return false;
        try { Object result = method.invoke(vanish, player); return result instanceof Boolean && (Boolean) result; }
        catch (ReflectiveOperationException | LinkageError ex) { return false; }
    }

    private BaseComponent[] formatMessage(String format, CommandSender sender, String message, String world, Player viewer, boolean senderVanished, String chatType) {
        String playerName;
        String chatColor = "";
        String prefix = "";
        String clanTag = "";
        String group = "desconhecido";
        boolean addVanishHover = false;
        String cargoColor = "§f";
        if (sender instanceof ConsoleCommandSender) {
            playerName = "Console";
        } else {
            Player player = (Player) sender;
            prefix = cargo.getPrefix(player.getUniqueId());
            // O nickname deve continuar exatamente na cor em que o gradient do
            // cargo termina. A cor fixa do CargoPlus não deve sobrescrever isso.
            cargoColor = lastColorCode(prefix);
            if (cargoColor.isEmpty()) cargoColor = cargo.getNicknameColor(player.getUniqueId());
            if (cargoColor.isEmpty()) cargoColor = "§f";
            playerName = cargoColor + sender.getName();
            clanTag = cargo.getClanTag(player.getUniqueId());
            if (senderVanished && viewer != null && viewer.hasPermission(VANISH_PERMISSION)) {
                playerName += " " + VANISH_SUFFIX_MARKER;
                addVanishHover = true;
            }
            chatColor = cargo.getChatColor(player.getUniqueId());
            group = cargo.getGroup(player.getUniqueId());
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(PLAYER_PLACEHOLDER, playerName);
        placeholders.put(TAG_PLACEHOLDER, clanTag);
        placeholders.put(HABILIDADE_TAG_PLACEHOLDER, resolveHabilidadeTag(sender));
        placeholders.put(MESSAGE_PLACEHOLDER, chatColor + MessageUtil.sanitizePlayerText(message));
        placeholders.put("{world}", world);
        String template = MessageUtil.colorize(format == null ? "" : format);
        // A tag Top 1 deve aparecer mesmo em configs antigas que ainda não
        // possuem {habilidade_tag}; a posição padrão é imediatamente após o nick.
        if (sender instanceof Player && !template.contains(HABILIDADE_TAG_PLACEHOLDER)) {
            template = template.replace(PLAYER_PLACEHOLDER, PLAYER_PLACEHOLDER + HABILIDADE_TAG_PLACEHOLDER);
        }

        if (sender instanceof Player && template.contains(PREFIX_PLACEHOLDER)) {
            String before = template.substring(0, template.indexOf(PREFIX_PLACEHOLDER));
            String afterTemplate = template.substring(template.indexOf(PREFIX_PLACEHOLDER) + PREFIX_PLACEHOLDER.length());
            placeholders.put(PREFIX_PLACEHOLDER, "");
            before = MessageUtil.apply(before, placeholders);
            afterTemplate = afterTemplate.replace(TAG_PLACEHOLDER, "");
            List<BaseComponent> components = new ArrayList<>();
            addLegacy(components, before);
            // Ordem fixa da identidade no chat:
            // [CANAL] ❤ [MCMMO/HABILIDADE] [$ MAGNATA] [TEMPO] [CLAN] [CARGO] Nickname
            String marriageTag = resolveMarriageTag((Player) sender);
            if (!marriageTag.isBlank()) {
                addMarriageTagWithHover(components, marriageTag, (Player) sender);
                addLegacy(components, " ");
            }
            String habilidadeTag = placeholders.getOrDefault(HABILIDADE_TAG_PLACEHOLDER, "");
            if (!habilidadeTag.isBlank()) {
                addHabilidadeTagWithHover(components, bracketHabilidadeTag(habilidadeTag), (Player) sender);
                addLegacy(components, " ");
            }
            String magnataTag = resolveMagnataTag((Player) sender);
            if (!magnataTag.isBlank()) {
                addMagnataTagWithHover(components, magnataTag);
                addLegacy(components, " ");
            }
            PlaytimeTag playtimeTag = resolvePlaytimeTag((Player) sender);
            if (playtimeTag != null) {
                addPlaytimeTagWithHover(components, playtimeTag, (Player) sender);
                addLegacy(components, " ");
            }
            if (!clanTag.isEmpty()) addClanTag(components, clanTag, cargoColor);
            BaseComponent[] prefixComponents = TextComponent.fromLegacyText(MessageUtil.colorize(prefix));
            HoverEvent prefixHover = buildCargoHover((Player) sender, prefix);
            for (BaseComponent component : prefixComponents) {
                component.setHoverEvent(prefixHover);
                components.add(component);
            }
            addPlayerAndAfter(components, afterTemplate, (Player) sender, playerName, cargoColor, addVanishHover, placeholders);
            BaseComponent[] result = components.toArray(BaseComponent[]::new);
            decorateChatTypeHover(result, chatType);
            return result;
        }

        placeholders.put(PREFIX_PLACEHOLDER, prefix);
        if (sender instanceof Player player) {
            String magnataTag = resolveMagnataTag(player);
            if (!magnataTag.isBlank()) {
                if (template.contains(HABILIDADE_TAG_PLACEHOLDER)) {
                    template = template.replace(HABILIDADE_TAG_PLACEHOLDER,
                            HABILIDADE_TAG_PLACEHOLDER + " " + magnataTag);
                } else if (template.contains(PLAYER_PLACEHOLDER)) {
                    template = template.replace(PLAYER_PLACEHOLDER,
                            PLAYER_PLACEHOLDER + " " + magnataTag);
                }
            }
        }
        BaseComponent[] result = formatWithMessageTimestamp(template, placeholders, addVanishHover);
        decorateChatTypeHover(result, chatType);
        return result;
    }


    private String resolveMarriageTag(Player player) {
        if (player == null) return "";
        Plugin essentials = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
        if (essentials == null || !essentials.isEnabled()) {
            marriagePlugin = null;
            marriageTagMethod = null;
            return "";
        }

        Method method = marriageTagMethod;
        if (marriagePlugin != essentials || method == null) {
            synchronized (this) {
                if (marriagePlugin != essentials || marriageTagMethod == null) {
                    try {
                        marriagePlugin = essentials;
                        marriageTagMethod = essentials.getClass().getMethod("getMarriageTag", java.util.UUID.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        marriagePlugin = essentials;
                        marriageTagMethod = null;
                    }
                }
                method = marriageTagMethod;
            }
        }

        if (method == null) return "";
        try {
            Object value = method.invoke(essentials, player.getUniqueId());
            return value == null ? "" : String.valueOf(value).trim();
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private String resolveMarriagePartnerName(Player player) {
        if (player == null) return "";
        Plugin essentials = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
        if (essentials == null || !essentials.isEnabled()) {
            marriagePlugin = null;
            marriageTagMethod = null;
            marriagePartnerNameMethod = null;
            return "";
        }

        Method method = marriagePartnerNameMethod;
        if (marriagePlugin != essentials || method == null) {
            synchronized (this) {
                if (marriagePlugin != essentials || marriagePartnerNameMethod == null) {
                    try {
                        marriagePlugin = essentials;
                        marriagePartnerNameMethod = essentials.getClass().getMethod(
                                "getMarriagePartnerName", java.util.UUID.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        marriagePlugin = essentials;
                        marriagePartnerNameMethod = null;
                    }
                }
                method = marriagePartnerNameMethod;
            }
        }

        if (method == null) return "";
        try {
            Object value = method.invoke(essentials, player.getUniqueId());
            return value == null ? "" : String.valueOf(value).trim();
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private java.util.UUID resolveMarriagePartnerUuid(Player player) {
        if (player == null) return null;
        Plugin essentials = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
        if (essentials == null || !essentials.isEnabled()) {
            marriagePlugin = null;
            marriageTagMethod = null;
            marriagePartnerNameMethod = null;
            marriagePartnerUuidMethod = null;
            return null;
        }

        Method method = marriagePartnerUuidMethod;
        if (marriagePlugin != essentials || method == null) {
            synchronized (this) {
                if (marriagePlugin != essentials || marriagePartnerUuidMethod == null) {
                    try {
                        marriagePlugin = essentials;
                        marriagePartnerUuidMethod = essentials.getClass().getMethod(
                                "getMarriagePartnerUuid", java.util.UUID.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        marriagePlugin = essentials;
                        marriagePartnerUuidMethod = null;
                    }
                }
                method = marriagePartnerUuidMethod;
            }
        }

        if (method == null) return null;
        try {
            Object value = method.invoke(essentials, player.getUniqueId());
            return value instanceof java.util.UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private void addMarriageTagWithHover(List<BaseComponent> components, String displayTag, Player player) {
        String partner = resolveMarriagePartnerName(player);
        java.util.UUID partnerUuid = resolveMarriagePartnerUuid(player);
        String partnerColor = partnerUuid == null ? "" : cargo.getNicknameColor(partnerUuid);
        if (partnerColor == null || partnerColor.isBlank()) partnerColor = "§f";
        else partnerColor = MessageUtil.colorize(partnerColor);

        BaseComponent[] hoverText = partner.isBlank()
                ? TextComponent.fromLegacyText("§7Casado(a)")
                : TextComponent.fromLegacyText("§7Casado(a) com " + partnerColor + partner);

        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText);
        BaseComponent[] parsed = TextComponent.fromLegacyText(MessageUtil.colorize(displayTag));
        for (BaseComponent component : parsed) {
            component.setHoverEvent(hover);
            components.add(component);
        }
    }

    private String resolveMagnataTag(Player player) {
        if (player == null) return "";

        Plugin economia = Bukkit.getPluginManager().getPlugin("EconomiaPlus");
        if (economia == null || !economia.isEnabled()) {
            economiaPlugin = null;
            magnataTagMethod = null;
            return "";
        }

        Method method = magnataTagMethod;
        if (economiaPlugin != economia || method == null) {
            synchronized (this) {
                if (economiaPlugin != economia || magnataTagMethod == null) {
                    try {
                        economiaPlugin = economia;
                        magnataTagMethod = economia.getClass().getMethod("getMagnataChatTag", java.util.UUID.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        economiaPlugin = economia;
                        magnataTagMethod = null;
                    }
                }
                method = magnataTagMethod;
            }
        }

        if (method == null) return "";
        try {
            Object value = method.invoke(economia, player.getUniqueId());
            return value == null ? "" : String.valueOf(value).trim();
        } catch (ReflectiveOperationException | LinkageError ex) {
            magnataTagMethod = null;
            return "";
        }
    }

    private String resolveHabilidadeTag(CommandSender sender) {
        if (!(sender instanceof Player player) || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return "";
        }
        try {
            String tag = PlaceholderAPI.setPlaceholders(player, "%habilidade_tag%");
            return tag == null || tag.equals("%habilidade_tag%") ? "" : tag;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String bracketHabilidadeTag(String tag) {
        if (tag == null || tag.isBlank()) return "";
        String value = tag.trim();
        if (value.startsWith("[") && value.endsWith("]")) return value;

        int index = 0;
        while (index + 1 < value.length()) {
            char marker = value.charAt(index);
            if (marker != '&' && marker != '§') break;
            char code = value.charAt(index + 1);
            if (code == 'x' && index + 13 < value.length()) {
                boolean validHex = true;
                for (int i = 0; i < 6; i++) {
                    if (value.charAt(index + 2 + i * 2) != '§'
                            && value.charAt(index + 2 + i * 2) != '&') {
                        validHex = false;
                        break;
                    }
                }
                if (validHex) {
                    index += 14;
                    continue;
                }
            }
            index += 2;
        }

        String colors = value.substring(0, index);
        String text = value.substring(index).trim();
        if (text.startsWith("[") && text.endsWith("]")) return value;
        return colors + "[" + text + "]";
    }

    private void addMagnataTagWithHover(List<BaseComponent> components, String displayTag) {
        HoverEvent hover = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(MessageUtil.colorize("&fTop 1 em &aCoins (Magnata)"))
        );
        BaseComponent[] parsed = TextComponent.fromLegacyText(MessageUtil.colorize(displayTag));
        for (BaseComponent component : parsed) {
            component.setHoverEvent(hover);
            components.add(component);
        }
    }

    private void addHabilidadeTagWithHover(List<BaseComponent> components, String displayTag, Player player) {
        BaseComponent[] parsed = TextComponent.fromLegacyText(MessageUtil.colorize(displayTag));
        if (!config.isHabilidadeHoverEnabled()) {
            for (BaseComponent component : parsed) {
                components.add(component);
            }
            return;
        }

        String skillName = resolveTop1SkillName(player);
        String hoverTemplate = skillName.isBlank()
                ? config.getHabilidadeHoverFallback()
                : config.getHabilidadeHoverFormat().replace("{habilidade}", skillName);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(MessageUtil.colorize(hoverTemplate)));
        for (BaseComponent component : parsed) {
            component.setHoverEvent(hover);
            components.add(component);
        }
    }

    private String resolveTop1SkillName(Player player) {
        if (player == null) return "";
        Plugin habilidades = Bukkit.getPluginManager().getPlugin("HabilidadesPlus");
        if (habilidades == null || !habilidades.isEnabled()) return "";
        try {
            if (habilidadesPlugin != habilidades || top1SkillNameMethod == null) {
                habilidadesPlugin = habilidades;
                top1SkillNameMethod = habilidades.getClass().getMethod("getTop1SkillDisplayName", java.util.UUID.class);
            }
            Object value = top1SkillNameMethod.invoke(habilidades, player.getUniqueId());
            return value == null ? "" : String.valueOf(value).trim();
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private HoverEvent buildCargoHover(Player player, String prefix) {
        String group = player == null ? "" : cargo.getGroup(player.getUniqueId());
        String displayName = cargoDisplayName(player, prefix, group);

        String nicknameColor = player == null ? "" : cargo.getNicknameColor(player.getUniqueId());
        if (nicknameColor == null || nicknameColor.isBlank()) nicknameColor = "§f";
        else nicknameColor = MessageUtil.colorize(nicknameColor);

        BaseComponent[] text = TextComponent.fromLegacyText("§7Cargo: " + nicknameColor + displayName);
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
    }

    private void addPlayerAndAfter(List<BaseComponent> components, String template, Player player, String playerName, String cargoColor, boolean addVanishHover, Map<String, String> placeholders) {
        int playerIndex = template.indexOf(PLAYER_PLACEHOLDER);
        if (playerIndex < 0) {
            placeholders.put(PLAYER_PLACEHOLDER, playerName);
            if (addVanishHover) addLegacyWithVanishHover(components, MessageUtil.apply(template, placeholders));
            else addLegacy(components, MessageUtil.apply(template, placeholders));
            return;
        }
        String beforePlayer = template.substring(0, playerIndex);
        String afterPlayer = template.substring(playerIndex + PLAYER_PLACEHOLDER.length());
        afterPlayer = afterPlayer.replace(HABILIDADE_TAG_PLACEHOLDER, "");
        placeholders.put(PLAYER_PLACEHOLDER, "");
        addLegacy(components, MessageUtil.apply(beforePlayer, placeholders));

        String nickname = cargoColor + player.getName();
        HoverEvent nicknameHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, buildPlayerProfileHover(player, nickname, cargoColor));
        BaseComponent[] nicknameComponents = TextComponent.fromLegacyText(nickname);
        for (BaseComponent component : nicknameComponents) {
            component.setHoverEvent(nicknameHover);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/g " + player.getName() + ", "));
            components.add(component);
        }

        // A tag de invisibilidade fica imediatamente depois do nickname.
        // Ela só é adicionada quando o visualizador possui a permissão de
        // enxergar jogadores invisíveis (essentialsplus.vanish).
        if (addVanishHover) {
            addLegacyWithVanishHover(components, " " + VANISH_SUFFIX);
        }

        addTemplateWithMessageTimestamp(components, afterPlayer, placeholders, addVanishHover);
    }

    private void addTemplateWithMessageTimestamp(List<BaseComponent> components, String template,
                                                 Map<String, String> placeholders, boolean addVanishHover) {
        int messageIndex = template.indexOf(MESSAGE_PLACEHOLDER);
        if (messageIndex < 0) {
            String rendered = MessageUtil.apply(template, placeholders);
            if (addVanishHover) addLegacyWithVanishHover(components, rendered);
            else addLegacy(components, rendered);
            return;
        }

        String beforeMessage = template.substring(0, messageIndex);
        String afterMessage = template.substring(messageIndex + MESSAGE_PLACEHOLDER.length());

        String renderedBefore = MessageUtil.apply(beforeMessage, placeholders);
        if (addVanishHover) addLegacyWithVanishHover(components, renderedBefore);
        else addLegacy(components, renderedBefore);

        String renderedMessage = placeholders.getOrDefault(MESSAGE_PLACEHOLDER, "");
        HoverEvent timestampHover = buildMessageTimestampHover();
        for (BaseComponent component : TextComponent.fromLegacyText(renderedMessage)) {
            component.setHoverEvent(timestampHover);
            components.add(component);
        }

        String renderedAfter = MessageUtil.apply(afterMessage, placeholders);
        if (addVanishHover) addLegacyWithVanishHover(components, renderedAfter);
        else addLegacy(components, renderedAfter);
    }

    private BaseComponent[] formatWithMessageTimestamp(String template, Map<String, String> placeholders,
                                                       boolean addVanishHover) {
        List<BaseComponent> components = new ArrayList<>();
        int messageIndex = template.indexOf(MESSAGE_PLACEHOLDER);
        if (messageIndex < 0) {
            String rendered = MessageUtil.apply(template, placeholders);
            return addVanishHover ? parseWithVanishHover(rendered) : TextComponent.fromLegacyText(rendered);
        }

        String beforeMessage = MessageUtil.apply(template.substring(0, messageIndex), placeholders);
        if (addVanishHover) {
            for (BaseComponent component : parseWithVanishHover(beforeMessage)) components.add(component);
        } else {
            addLegacy(components, beforeMessage);
        }

        HoverEvent timestampHover = buildMessageTimestampHover();
        String renderedMessage = placeholders.getOrDefault(MESSAGE_PLACEHOLDER, "");
        for (BaseComponent component : TextComponent.fromLegacyText(renderedMessage)) {
            component.setHoverEvent(timestampHover);
            components.add(component);
        }

        String afterMessage = MessageUtil.apply(
                template.substring(messageIndex + MESSAGE_PLACEHOLDER.length()), placeholders);
        if (addVanishHover) {
            for (BaseComponent component : parseWithVanishHover(afterMessage)) components.add(component);
        } else {
            addLegacy(components, afterMessage);
        }

        return components.toArray(BaseComponent[]::new);
    }

    private HoverEvent buildMessageTimestampHover() {
        LocalDateTime sentAt = LocalDateTime.now();
        String hover = config.getMessageHover()
                .replace("{data}", MESSAGE_DATE_FORMAT.format(sentAt))
                .replace("{hora}", MESSAGE_TIME_FORMAT.format(sentAt));
        return new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(MessageUtil.colorize(hover))
        );
    }

    private String cargoDisplayName(Player player, String prefix, String group) {
        String normalizedGroup = group == null ? "" : group.trim();
        if (normalizedGroup.equalsIgnoreCase("dev")
                || normalizedGroup.equalsIgnoreCase("developer")
                || normalizedGroup.equalsIgnoreCase("desenvolvedor")) {
            return "Dono/Desenvolvedor";
        }

        if (player != null) {
            String configured = cargo.getDisplayName(player.getUniqueId());
            if (configured != null && !configured.isBlank()) {
                return org.bukkit.ChatColor.stripColor(MessageUtil.colorize(configured)).trim();
            }
        }

        if (!normalizedGroup.isBlank() && !normalizedGroup.equalsIgnoreCase("desconhecido")) {
            return capitalizeGroupName(normalizedGroup);
        }

        if (prefix == null || prefix.isBlank()) return "Desconhecido";
        String plain = org.bukkit.ChatColor.stripColor(MessageUtil.colorize(prefix)).trim();
        if (plain.startsWith("[") && plain.endsWith("]")) plain = plain.substring(1, plain.length() - 1).trim();
        return plain.isBlank() ? "Desconhecido" : plain;
    }

    private BaseComponent[] buildPlayerProfileHover(Player player, String nickname, String cargoColor) {
        String clanValue = cargo.getClanTag(player.getUniqueId());
        boolean hasClan = clanValue != null && !clanValue.isBlank();
        if (!hasClan) clanValue = "Nenhum";
        else clanValue = MessageUtil.colorize(clanValue);

        String balance = getEconomyBalance(player);
        String coinsValue = balance;
        String moedasValue = balance;
        String powerValue = String.valueOf(getPowerLevel(player));

        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = player.getStatistic(Statistic.DEATHS);
        double kdr = deaths <= 0 ? kills : (double) kills / deaths;

        String cleanNickname = player.getName();
        String nicknameDisplay = (cargoColor == null || cargoColor.isEmpty() ? "§f" : cargoColor) + cleanNickname;

        Map<String, String> hover = new HashMap<>();
        hover.put("{player}", nicknameDisplay);
        hover.put("{coins}", coinsValue);
        hover.put("{moedas}", moedasValue);
        hover.put("{poder}", powerValue);
        hover.put("{kdr}", String.format(Locale.US, "%.2f", kdr));
        hover.put("{clan}", clanValue);
        hover.put("{tempo}", formatOnlineTime(player));
        hover.put("{conta-criada}", getRegistrationDate(player));

        ComponentBuilder builder = new ComponentBuilder();
        appendConfiguredHoverLine(builder, config.getPlayerHoverTitle(), hover);
        builder.append("\n\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverCoins(), hover);
        builder.append("\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverMoedas(), hover);
        builder.append("\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverPower(), hover);
        builder.append("\n\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverKdr(), hover);
        builder.append("\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverClan(), hover);
        builder.append("\n\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverOnlineTime(), hover);
        builder.append("\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverRegistered(), hover);
        builder.append("\n\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverInteraction(), hover);
        return builder.create();
    }

    private void appendConfiguredHoverLine(ComponentBuilder builder, String template, Map<String, String> placeholders) {
        String line = template == null ? "" : template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            line = line.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        BaseComponent[] components = TextComponent.fromLegacyText(MessageUtil.colorize(line));
        for (BaseComponent component : components) builder.append(component);
    }

    private String getRegistrationDate(Player player) {
        if (player == null) return "Desconhecida";
        Plugin loginPlus = Bukkit.getPluginManager().getPlugin("LoginPlus");
        if (loginPlus == null || !loginPlus.isEnabled()) return "Desconhecida";
        try {
            Method method = registrationDateMethod;
            if (method == null) {
                method = loginPlus.getClass().getMethod("getRegistrationDate", String.class);
                registrationDateMethod = method;
            }
            Object result = method.invoke(loginPlus, player.getName());
            if (result == null) return "Desconhecida";
            String date = String.valueOf(result).trim();
            return date.isEmpty() ? "Desconhecida" : date;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return "Desconhecida";
        }
    }

    private String getEconomyBalance(Player player) {
        try {
            Plugin economyPlugin = Bukkit.getPluginManager().getPlugin("CoinsEconomy");
            if (economyPlugin == null) economyPlugin = Bukkit.getPluginManager().getPlugin("EconomiaPlus");
            if (economyPlugin == null || !economyPlugin.isEnabled()) return "0";

            Method getEconomyManager = economyPlugin.getClass().getMethod("getEconomyManager");
            Object economyManager = getEconomyManager.invoke(economyPlugin);
            if (economyManager == null) return "0";

            Method getSaldo = economyManager.getClass().getMethod("getSaldo", java.util.UUID.class);
            Object saldo = getSaldo.invoke(economyManager, player.getUniqueId());
            if (!(saldo instanceof Number number)) return "0";

            double value = number.doubleValue();
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("pt", "BR"));
            DecimalFormat format = new DecimalFormat("#,##0.##", symbols);
            return format.format(value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return "0";
        }
    }

    private int getPowerLevel(Player player) {
        try {
            Plugin habilidades = Bukkit.getPluginManager().getPlugin("HabilidadesPlus");
            if (habilidades == null || !habilidades.isEnabled()) return 0;

            Method getDataManager = habilidades.getClass().getMethod("getDataManager");
            Object dataManager = getDataManager.invoke(habilidades);
            if (dataManager == null) return 0;

            Method getProfile = dataManager.getClass().getMethod("getProfile", java.util.UUID.class);
            Object profile = getProfile.invoke(dataManager, player.getUniqueId());
            if (profile == null) return 0;

            Method getPowerLevel = profile.getClass().getMethod("getPowerLevel");
            Object power = getPowerLevel.invoke(profile);
            return power instanceof Number number ? number.intValue() : 0;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    private PlaytimeTag resolvePlaytimeTag(Player player) {
        if (player == null || !config.isPlaytimeTagsEnabled()) return null;

        long playedHours = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L / 60L;
        PlaytimeTag selected = null;

        for (Map<?, ?> entry : config.getPlaytimeTagRanges()) {
            Object rawHours = entry.get("horas");
            Object rawTag = entry.get("tag");
            if (!(rawHours instanceof Number number) || rawTag == null) continue;

            long requiredHours = Math.max(0L, number.longValue());
            String tag = String.valueOf(rawTag).trim();
            if (tag.isEmpty() || playedHours < requiredHours) continue;

            if (selected == null || requiredHours > selected.requiredHours()) {
                selected = new PlaytimeTag(tag, requiredHours);
            }
        }

        return selected;
    }

    private void addPlaytimeTagWithHover(List<BaseComponent> components, PlaytimeTag tag, Player player) {
        String hoverText = config.getPlaytimeTagHover()
                .replace("{tag}", tag.text())
                .replace("{tempo}", formatOnlineTime(player))
                .replace("{horas}", String.valueOf(tag.requiredHours()));

        BaseComponent[] parsed = TextComponent.fromLegacyText(MessageUtil.colorize(tag.text()));
        HoverEvent hover = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(MessageUtil.colorize(hoverText)).create()
        );
        for (BaseComponent component : parsed) {
            component.setHoverEvent(hover);
            components.add(component);
        }
    }

    private record PlaytimeTag(String text, long requiredHours) {}

    private String formatOnlineTime(Player player) {
        long minutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L;
        long days = minutes / (24L * 60L);
        long hours = (minutes % (24L * 60L)) / 60L;
        long remainingMinutes = minutes % 60L;
        if (days > 0) return days + "d " + hours + "h " + remainingMinutes + "m";
        if (hours > 0) return hours + "h " + remainingMinutes + "m";
        return remainingMinutes + "m";
    }

    private String capitalizeGroupName(String group) {
        if (group == null || group.isBlank()) return "Desconhecido";
        return Character.toUpperCase(group.charAt(0)) + group.substring(1).toLowerCase(Locale.ROOT);
    }

    private String firstColorCode(String text) {
        if (text == null) return "";
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') return text.substring(i, i + 2);
        }
        return "";
    }

    /**
     * Retorna a última cor real presente no prefixo do cargo.
     * Suporta tanto cores legacy quanto RGB no formato §x§R§R§G§G§B§B.
     */
    private String lastColorCode(String text) {
        if (text == null || text.isEmpty()) return "";
        String last = "";
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != '§') continue;

            char code = text.charAt(i + 1);
            if (code == 'x' && i + 13 < text.length()) {
                boolean validHex = true;
                for (int j = 0; j < 6; j++) {
                    if (text.charAt(i + 2 + (j * 2)) != '§'
                            || Character.digit(text.charAt(i + 3 + (j * 2)), 16) < 0) {
                        validHex = false;
                        break;
                    }
                }
                if (validHex) {
                    last = text.substring(i, i + 14);
                    i += 13;
                    continue;
                }
            }

            if (isLegacyColorCode(code)) {
                last = text.substring(i, i + 2);
            }
        }
        return last;
    }

    private boolean isLegacyColorCode(char code) {
        return (code >= '0' && code <= '9')
                || (code >= 'a' && code <= 'f')
                || (code >= 'A' && code <= 'F');
    }

    private void addClanTag(List<BaseComponent> components, String clanTag, String cargoColor) {
        // Os colchetes permanecem sempre em cinza claro (§7).
        // Somente o nome/tag do clã usa a cor configurada no ClanPlus.
        String coloredTag = MessageUtil.colorize(clanTag);
        addLegacy(components, "§7[" + coloredTag + "§7] ");
    }

    private void addLegacy(List<BaseComponent> components, String text) {
        if (text == null || text.isEmpty()) return;
        for (BaseComponent component : TextComponent.fromLegacyText(text)) components.add(component);
    }

    private void addLegacyWithVanishHover(List<BaseComponent> components, String text) {
        BaseComponent[] parsed = TextComponent.fromLegacyText(text);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessageUtil.colorize(config.getVanishHover())).create());
        for (BaseComponent component : parsed) {
            component.setHoverEvent(hover);
            components.add(component);
        }
    }

    private BaseComponent[] parseWithVanishHover(String text) {
        BaseComponent[] parsed = TextComponent.fromLegacyText(text.replace(VANISH_SUFFIX_MARKER, VANISH_SUFFIX));
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(VANISH_HOVER_TEXT).create());
        for (BaseComponent component : parsed) component.setHoverEvent(hover);
        return parsed;
    }

    private void decorateChatTypeHover(BaseComponent[] components, String chatType) {
        if (components == null || chatType == null || chatType.isEmpty()) return;
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessageUtil.colorize(config.getChannelHover().replace("{canal}", chatType))).create());
        for (BaseComponent component : components) if (component.getHoverEvent() == null) component.setHoverEvent(hover);
    }
}
