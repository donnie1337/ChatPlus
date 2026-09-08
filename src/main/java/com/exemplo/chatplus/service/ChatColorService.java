package com.exemplo.chatplus.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatColorService {
    private static final List<Integer> GUI_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24
    );

    private final CargoPlusBridge cargo;

    public ChatColorService() {
        this.cargo = new CargoPlusBridge();
    }

    public Map<String, String> getColors() {
        Map<String, String> configured = cargo.getChatColors();
        if (configured.isEmpty()) return Map.of();

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configured.entrySet()) {
            String name = entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (name.equals("preto")) continue;
            ChatColor color = parse(entry.getValue());
            if (color != null) result.put(name, color.toString());
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
        return ChatColor.translateAlternateColorCodes('&', "&8Selecione a cor da sua mensagem");
    }

    public int getInventorySize() {
        return 36;
    }

    public List<Integer> getSlots() {
        return new ArrayList<>(GUI_SLOTS);
    }

    public boolean closeAfterSelection() {
        return true;
    }

    private String defaultAllowedColor() {
        String fallback = cargo.getDefaultChatColor();
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
