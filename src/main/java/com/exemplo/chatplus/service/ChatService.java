package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.util.MessageUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatService {
    private static final String PREFIX_PLACEHOLDER = "{prefix}";
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String PLAYER_PLACEHOLDER = "{player}";
    private static final String STAFF_PERMISSION = "chatplus.staff";
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String VANISH_SUFFIX_MARKER = "§0§0§0[ESSENTIALSPLUS_VANISH_HOVER]";
    private static final String VANISH_SUFFIX = "§l§x§F§F§F§F§F§F[§x§F§B§F§B§F§Bɪ§x§F§7§F§7§F§7ɴ§x§F§4§F§4§F§4ᴠ§x§F§0§F§0§F§0ɪ§x§E§C§E§C§E§C§x§E§8§E§8§E§8ɪ§x§E§4§E§4§E§4ᴠ§x§E§1§E§1§E§1ᴇ§x§D§D§D§D§D§Dʟ§x§D§9§D§9§D§9]";
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
            cargoColor = cargo.getNicknameColor(player.getUniqueId());
            if (cargoColor.isEmpty()) cargoColor = firstColorCode(prefix);
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
        placeholders.put("{message}", chatColor + MessageUtil.sanitizePlayerText(message));
        placeholders.put("{world}", world);
        String template = MessageUtil.colorize(format == null ? "" : format);

        if (sender instanceof Player && template.contains(PREFIX_PLACEHOLDER)) {
            String before = template.substring(0, template.indexOf(PREFIX_PLACEHOLDER));
            String afterTemplate = template.substring(template.indexOf(PREFIX_PLACEHOLDER) + PREFIX_PLACEHOLDER.length());
            placeholders.put(PREFIX_PLACEHOLDER, "");
            before = MessageUtil.apply(before, placeholders);
            afterTemplate = afterTemplate.replace(TAG_PLACEHOLDER, "");
            List<BaseComponent> components = new ArrayList<>();
            addLegacy(components, before);
            BaseComponent[] prefixComponents = TextComponent.fromLegacyText(MessageUtil.colorize(prefix));
            HoverEvent prefixHover = buildCargoHover(cargoColor, group);
            for (BaseComponent component : prefixComponents) {
                component.setHoverEvent(prefixHover);
                components.add(component);
            }
            if (!clanTag.isEmpty()) addClanTag(components, clanTag, cargoColor);
            addPlayerAndAfter(components, afterTemplate, (Player) sender, playerName, cargoColor, addVanishHover, placeholders);
            BaseComponent[] result = components.toArray(BaseComponent[]::new);
            decorateChatTypeHover(result, chatType);
            return result;
        }

        placeholders.put(PREFIX_PLACEHOLDER, prefix);
        String formatted = MessageUtil.apply(template, placeholders);
        BaseComponent[] result = addVanishHover ? parseWithVanishHover(formatted) : TextComponent.fromLegacyText(formatted);
        decorateChatTypeHover(result, chatType);
        return result;
    }

    private HoverEvent buildCargoHover(String cargoColor, String group) {
        String safeColor = cargoColor == null || cargoColor.isEmpty() ? "§f" : cargoColor;
        String cargoName = capitalizeGroupName(group);
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§fCargo: ").append(TextComponent.fromLegacyText(safeColor + cargoName)).create());
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
        placeholders.put(PLAYER_PLACEHOLDER, "");
        addLegacy(components, MessageUtil.apply(beforePlayer, placeholders));
        String nickname = cargoColor + player.getName();
        HoverEvent nicknameHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(buildPlayerProfileHover(player, nickname, cargoColor)).create());
        BaseComponent[] nicknameComponents = TextComponent.fromLegacyText(nickname);
        for (BaseComponent component : nicknameComponents) {
            component.setHoverEvent(nicknameHover);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, player.getName()));
            components.add(component);
        }
        if (addVanishHover) addLegacyWithVanishHover(components, MessageUtil.apply(afterPlayer, placeholders));
        else addLegacy(components, MessageUtil.apply(afterPlayer, placeholders));
    }

    private String buildPlayerProfileHover(Player player, String nickname, String cargoColor) {
        String cargoValue = cargo.getGroup(player.getUniqueId());
        if (cargoValue == null || cargoValue.isBlank()) cargoValue = "Desconhecido";
        String clanValue = cargo.getClanTag(player.getUniqueId());
        boolean hasClan = clanValue != null && !clanValue.isBlank();
        if (!hasClan) clanValue = "Nenhum";
        else clanValue = MessageUtil.colorize(clanValue);
        String moneyValue = getMoney(player);
        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = player.getStatistic(Statistic.DEATHS);
        double kdr = deaths <= 0 ? kills : (double) kills / deaths;
        String safeCargoColor = cargoColor == null || cargoColor.isEmpty() ? "§f" : cargoColor;
        StringBuilder lore = new StringBuilder();
        lore.append("§f§l◆ INFORMAÇÕES ◆\n");
        lore.append("\n");
        lore.append("  §7ᴊᴏɢᴀᴅᴏʀ §8• ").append(safeCargoColor).append(nickname.replaceFirst("§[0-9a-fk-or]", "")).append("\n");
        lore.append("  §7ᴄᴀʀɢᴏ §8• ").append(safeCargoColor).append(capitalizeGroupName(cargoValue)).append("\n");
        if (hasClan) lore.append("  §7ᴄʟᴀɴ §8• §f[").append(clanValue).append("§f]\n");
        else lore.append("  §7ᴄʟᴀɴ §8• §7Nenhum\n");
        lore.append("  §7ᴍᴏᴇᴅᴀs §8• §f").append(moneyValue).append("\n");
        lore.append("  §7ᴋᴅʀ §8• §f").append(String.format(Locale.US, "%.2f", kdr)).append("\n");
        lore.append("  §7ᴛᴇᴍᴘᴏ ᴏɴʟɪɴᴇ §8• §f").append(formatOnlineTime(player)).append("\n");
        lore.append("\n");
        lore.append("§7Perfil do jogador");
        lore.append("\n§bClique aqui para interagir com este jogador.");
        return lore.toString();
    }

    private String getMoney(Player player) {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Class<?> rspClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
            Object provider = Bukkit.getServicesManager().getRegistration(economyClass).getProvider();
            Method balanceMethod = economyClass.getMethod("getBalance", OfflinePlayer.class);
            Object balance = balanceMethod.invoke(provider, player);
            if (balance instanceof Number number) {
                DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("pt", "BR"));
                DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
                return format.format(number.doubleValue());
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return "0,00";
    }

    private String formatOnlineTime(Player player) {
        long minutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L;
        long days = minutes / (24L * 60L);
        long hours = (minutes % (24L * 60L)) / 60L;
        long mins = minutes % 60L;
        if (days > 0) return days + "d " + hours + "h " + mins + "m";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    private String capitalizeGroupName(String group) {
        if (group == null || group.isBlank()) return "Desconhecido";
        return group.substring(0, 1).toUpperCase(Locale.ROOT) + group.substring(1).toLowerCase(Locale.ROOT);
    }

    private String firstColorCode(String text) {
        if (text == null) return "";
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') return text.substring(i, i + 2);
        }
        return "";
    }

    private void addLegacy(List<BaseComponent> components, String text) {
        if (text == null || text.isEmpty()) return;
        for (BaseComponent component : TextComponent.fromLegacyText(text)) components.add(component);
    }

    private void addLegacyWithVanishHover(List<BaseComponent> components, String text) {
        if (text == null || text.isEmpty()) return;
        BaseComponent[] parsed = TextComponent.fromLegacyText(text);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(VANISH_HOVER_TEXT));
        for (BaseComponent component : parsed) {
            if (component.toPlainText().contains(VANISH_SUFFIX_MARKER)) {
                component.setText(component.toPlainText().replace(VANISH_SUFFIX_MARKER, VANISH_SUFFIX));
                component.setHoverEvent(hover);
            }
            components.add(component);
        }
    }

    private BaseComponent[] parseWithVanishHover(String text) {
        List<BaseComponent> components = new ArrayList<>();
        addLegacyWithVanishHover(components, text);
        return components.toArray(BaseComponent[]::new);
    }

    private void addClanTag(List<BaseComponent> components, String clanTag, String cargoColor) {
        BaseComponent[] tag = TextComponent.fromLegacyText("§8[" + MessageUtil.colorize(clanTag) + "§8]");
        for (BaseComponent component : tag) {
            component.setHoverEvent(buildCargoHover(cargoColor, cargo.getGroup(clanTag.hashCode() > 0 ? new java.util.UUID(0L, 0L) : new java.util.UUID(0L, 0L))));
            components.add(component);
        }
    }

    private void decorateChatTypeHover(BaseComponent[] components, String chatType) {
        // Mantido sem alteração para preservar o hover do tipo de chat.
    }
}
