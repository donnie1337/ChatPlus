package com.exemplo.chatplus.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ChatColorService {
    private final CargoPlusBridge cargo;

    public ChatColorService() {
        this.cargo = new CargoPlusBridge();
    }

    public Map<String, String> getColors() {
        var config = cargo.getCorConfig();
        if (config == null) return Map.of();

        Map<String, String> result = new LinkedHashMap<>();
        for (String name : config.getConfigurationSection("colors") == null
                ? java.util.List.<String>of()
                : config.getConfigurationSection("colors").getKeys(false)) {
            if (name.equalsIgnoreCase("preto")) continue;
            String raw = config.getString("colors." + name);
            ChatColor color = parse(raw);
            if (color != null) result.put(name.toLowerCase(Locale.ROOT), color.toString());
        }
        return result;
    }

    public ChatColor resolve(String name) {
        if (name == null || name.isBlank()) return ChatColor.WHITE;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        String value = getColors().get(normalized);
        ChatColor color = parse(value);
        return color == null ? ChatColor.WHITE : color;
    }

    public String getCurrentColorName(Player player) {
        if (player == null) return defaultAllowedColor();
        String code = cargo.getChatColor(player.getUniqueId());
        ChatColor current = parse(code);
        if (current != null) {
            for (Map.Entry<String, String> entry : getColors().entrySet()) {
                if (parse(entry.getValue()) == current) return entry.getKey();
            }
        }
        return defaultAllowedColor();
    }

    public boolean setColor(Player player, String color) {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled("CargoPlus")) return false;
        String normalized = color == null ? "" : color.trim().toLowerCase(Locale.ROOT);
        if (!getColors().containsKey(normalized)) return false;
        return cargo.setChatColor(player, normalized);
    }

    public String getTitle() {
        var config = cargo.getCorConfig();
        String raw = config == null ? "&8Escolha a cor da sua mensagem" :
                config.getString("gui.titulo", "&8Escolha a cor da sua mensagem");
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    public int getInventorySize() {
        var config = cargo.getCorConfig();
        int size = config == null ? 36 : config.getInt("gui.tamanho", 36);
        if (size < 9 || size > 54 || size % 9 != 0) return 36;
        return size;
    }

    public java.util.List<Integer> getSlots() {
        var config = cargo.getCorConfig();
        if (config == null) return java.util.List.of();
        return config.getIntegerList("gui.slots");
    }

    public boolean closeAfterSelection() {
        var config = cargo.getCorConfig();
        return config == null || config.getBoolean("gui.close-after-selection", true);
    }

    private String defaultAllowedColor() {
        var config = cargo.getCorConfig();
        String fallback = config == null ? "branco" : config.getString("default-color", "branco");
        if (fallback != null) {
            String normalized = fallback.trim().toLowerCase(Locale.ROOT);
            if (getColors().containsKey(normalized)) return normalized;
        }
        return getColors().keySet().stream().findFirst().orElse("branco");
    }

    private static ChatColor parse(String value) {
        if (value == null) return null;
        String translated = ChatColor.translateAlternateColorCodes('&', value.trim());
        if (translated.length() != 2 || translated.charAt(0) != ChatColor.COLOR_CHAR) return null;
        return ChatColor.getByChar(translated.charAt(1));
    }
}
