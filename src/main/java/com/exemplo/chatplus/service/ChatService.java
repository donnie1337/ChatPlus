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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatService {
    private static final String PREFIX_PLACEHOLDER = "{prefix}";
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String PLAYER_PLACEHOLDER = "{player}";
    private static final String HABILIDADE_TAG_PLACEHOLDER = "{habilidade_tag}";
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
        placeholders.put("{message}", chatColor + MessageUtil.sanitizePlayerText(message));
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
            BaseComponent[] prefixComponents = TextComponent.fromLegacyText(MessageUtil.colorize(prefix));
            HoverEvent prefixHover = buildCargoHover(prefix);
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

    private HoverEvent buildCargoHover(String prefix) {
        // O hover do cargo mostra somente a tag fixa do cargo,
        // preservando exatamente as cores do gradient exibidas no chat.
        BaseComponent[] tag = TextComponent.fromLegacyText(
                MessageUtil.colorize(prefix == null ? "" : prefix)
        );
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, tag);
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
        String habilidadeTag = placeholders.getOrDefault(HABILIDADE_TAG_PLACEHOLDER, "");
        afterPlayer = afterPlayer.replace(HABILIDADE_TAG_PLACEHOLDER, "");
        placeholders.put(PLAYER_PLACEHOLDER, "");
        addLegacy(components, MessageUtil.apply(beforePlayer, placeholders));

        // A tag Top 1 pertence à identidade do jogador e deve ficar
        // imediatamente antes do nickname, nunca depois dele.
        if (habilidadeTag != null && !habilidadeTag.isBlank()) {
            // A tag Top 1 é exibida sempre entre colchetes e com hover explicativo.
            String displayTag = bracketHabilidadeTag(habilidadeTag);
            addHabilidadeTagWithHover(components, displayTag, player);
            addLegacy(components, " ");
        }

        String nickname = cargoColor + player.getName();
        HoverEvent nicknameHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, buildPlayerProfileHover(player, nickname, cargoColor));
        BaseComponent[] nicknameComponents = TextComponent.fromLegacyText(nickname);
        for (BaseComponent component : nicknameComponents) {
            component.setHoverEvent(nicknameHover);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, player.getName()));
            components.add(component);
        }

        // A tag de invisibilidade fica imediatamente depois do nickname.
        // Ela só é adicionada quando o visualizador possui a permissão de
        // enxergar jogadores invisíveis (essentialsplus.vanish).
        if (addVanishHover) {
            addLegacyWithVanishHover(components, " " + VANISH_SUFFIX);
        }

        if (addVanishHover) addLegacyWithVanishHover(components, MessageUtil.apply(afterPlayer, placeholders));
        else addLegacy(components, MessageUtil.apply(afterPlayer, placeholders));
    }

    private String cargoDisplayName(String prefix) {
        if (prefix == null || prefix.isBlank()) return "Desconhecido";
        String plain = org.bukkit.ChatColor.stripColor(MessageUtil.colorize(prefix)).trim();
        if (plain.startsWith("[") && plain.endsWith("]")) plain = plain.substring(1, plain.length() - 1).trim();
        return plain.isBlank() ? "Desconhecido" : plain;
    }

    private BaseComponent[] buildPlayerProfileHover(Player player, String nickname, String cargoColor) {
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
        // O hover deve usar o nickname real e completo do jogador.
        // Não derivar o nome do componente colorido para evitar truncamentos
        // quando houver mais de um código de cor/formatação.
        String cleanNickname = player.getName();
        String nicknameDisplay = (cargoColor == null || cargoColor.isEmpty() ? "§f" : cargoColor) + cleanNickname;

        Map<String, String> hover = new HashMap<>();
        hover.put("{player}", nicknameDisplay);
        hover.put("{cargo}", cargoValue);
        hover.put("{clan}", clanValue);
        hover.put("{moedas}", moneyValue);
        hover.put("{kdr}", String.format(Locale.US, "%.2f", kdr));
        hover.put("{tempo}", formatOnlineTime(player));
        hover.put("{conta-criada}", getRegistrationDate(player));

        ComponentBuilder builder = new ComponentBuilder();
        appendConfiguredHoverLine(builder, config.getPlayerHoverTitle(), hover);
        builder.append("\n\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverClan(), hover);
        builder.append("\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverMoney(), hover);
        builder.append("\n");
        appendConfiguredHoverLine(builder, config.getPlayerHoverKdr(), hover);
        builder.append("\n");
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

    private String getMoney(Player player) {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null) return "0,00";
            Method providerMethod = registration.getClass().getMethod("getProvider");
            Object provider = providerMethod.invoke(registration);
            if (provider == null) return "0,00";
            Method balanceMethod = provider.getClass().getMethod("getBalance", OfflinePlayer.class);
            Object balance = balanceMethod.invoke(provider, player);
            double value = balance instanceof Number ? ((Number) balance).doubleValue() : 0.0D;
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("pt", "BR"));
            DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
            return format.format(value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return "0,00";
        }
    }

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
