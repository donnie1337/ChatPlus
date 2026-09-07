package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ChatColorService {
    private static final String CARGO_API = "com.cargoplus.api.CargoPlusAPI";
    private final ConfigManager config;

    public ChatColorService(ConfigManager config) {
        this.config = config;
    }

    public Map<String, String> getColors() {
        Map<String, String> result = new LinkedHashMap<>();
        var section = config.getPlugin().getConfig().getConfigurationSection("chat.cores");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (parse(value) != null) result.put(key.toLowerCase(Locale.ROOT), value);
        }
        return result;
    }

    public ChatColor resolve(String name) {
        String value = getColors().get(name == null ? "" : name.toLowerCase(Locale.ROOT));
        ChatColor color = parse(value);
        return color == null ? ChatColor.WHITE : color;
    }

    public String getCurrentColorName(Player player) {
        String code = getCargoChatColor(player);
        ChatColor current = parse(code);
        if (current != null) {
            for (Map.Entry<String, String> entry : getColors().entrySet()) {
                if (parse(entry.getValue()) == current) return entry.getKey();
            }
        }
        String fallback = config.getPlugin().getConfig().getString("chat.cor-padrao", "branco");
        fallback = fallback == null ? "branco" : fallback.toLowerCase(Locale.ROOT);
        return getColors().containsKey(fallback) ? fallback : "branco";
    }

    public boolean setColor(Player player, String color) {
        if (player == null || !player.hasPermission("cargoplus.cor")) return false;
        String normalized = color == null ? "" : color.toLowerCase(Locale.ROOT);
        if (!getColors().containsKey(normalized)) return false;
        Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (cargo == null || !cargo.isEnabled()) return false;
        try {
            Method method = cargo.getClass().getMethod("setChatColor", Player.class, String.class);
            Object result = method.invoke(cargo, player, normalized);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            config.getPlugin().getLogger().warning("Não foi possível salvar a cor do chat no CargoPlus: " + ex.getMessage());
            return false;
        }
    }

    private String getCargoChatColor(Player player) {
        Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (cargo == null || !cargo.isEnabled()) return null;
        try {
            Class<?> apiClass = Class.forName(CARGO_API, true, cargo.getClass().getClassLoader());
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return null;
            Object api = registration.getClass().getMethod("getProvider").invoke(registration);
            Object result = apiClass.getMethod("getChatColor", java.util.UUID.class)
                    .invoke(api, player.getUniqueId());
            return result instanceof String ? (String) result : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static ChatColor parse(String value) {
        String translated = ChatColor.translateAlternateColorCodes('&', value == null ? "" : value.trim());
        if (translated.length() != 2 || translated.charAt(0) != ChatColor.COLOR_CHAR) return null;
        return ChatColor.getByChar(translated.charAt(1));
    }
}
