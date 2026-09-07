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
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : cargo.getChatColors().entrySet()) {
            ChatColor color = parse(entry.getValue());
            if (color != null) result.put(entry.getKey().toLowerCase(Locale.ROOT), color.toString());
        }
        return result;
    }

    public ChatColor resolve(String name) {
        String value = getColors().get(name == null ? "" : name.toLowerCase(Locale.ROOT));
        ChatColor color = parse(value);
        return color == null ? ChatColor.WHITE : color;
    }

    public String getCurrentColorName(Player player) {
        if (player == null) return cargo.getDefaultChatColor();
        String code = cargo.getChatColor(player.getUniqueId());
        ChatColor current = parse(code);
        if (current != null) {
            for (Map.Entry<String, String> entry : getColors().entrySet()) {
                if (parse(entry.getValue()) == current) return entry.getKey();
            }
        }
        String fallback = cargo.getDefaultChatColor();
        return getColors().containsKey(fallback) ? fallback : getColors().keySet().stream().findFirst().orElse("branco");
    }

    public boolean setColor(Player player, String color) {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled("CargoPlus")) return false;
        String normalized = color == null ? "" : color.toLowerCase(Locale.ROOT);
        if (!getColors().containsKey(normalized)) return false;
        return cargo.setChatColor(player, normalized);
    }

    private static ChatColor parse(String value) {
        if (value == null) return null;
        String translated = ChatColor.translateAlternateColorCodes('&', value.trim());
        if (translated.length() != 2 || translated.charAt(0) != ChatColor.COLOR_CHAR) return null;
        return ChatColor.getByChar(translated.charAt(1));
    }
}
