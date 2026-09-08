package com.exemplo.chatplus.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Centraliza as integrações opcionais do ChatPlus com CargoPlus e SistemaUtil. */
public final class CargoPlusBridge {
    private static final String API_CLASS_NAME = "com.cargoplus.api.CargoPlusAPI";

    private Class<?> apiClass;
    private Object api;
    private Plugin cargoPlugin;
    private Method getGroupMethod;
    private Method getPrefixMethod;
    private Method getNicknameColorMethod;
    private Method getChatColorMethod;
    private Method setChatColorMethod;
    private Method getChatColorsMethod;
    private Method getDefaultChatColorMethod;
    private Plugin utilPlugin;
    private Method getCorConfigMethod;

    public synchronized boolean refresh() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (plugin == null || !plugin.isEnabled()) {
            clearCargoCache();
            refreshUtilCache();
            return false;
        }

        try {
            Class<?> clazz = Class.forName(API_CLASS_NAME, true, plugin.getClass().getClassLoader());
            var registration = Bukkit.getServicesManager().getRegistration(clazz);
            if (registration == null) {
                clearCargoCache();
                cargoPlugin = plugin;
                apiClass = clazz;
                refreshUtilCache();
                return false;
            }

            Method providerMethod = registration.getClass().getMethod("getProvider");
            Object provider = providerMethod.invoke(registration);
            cargoPlugin = plugin;
            apiClass = clazz;
            api = provider;
            if (provider != null) {
                Class<?> providerClass = provider.getClass();
                getGroupMethod = providerClass.getMethod("getGroup", UUID.class);
                getPrefixMethod = providerClass.getMethod("getPrefix", UUID.class);
                getNicknameColorMethod = providerClass.getMethod("getNicknameColor", UUID.class);
                getChatColorMethod = providerClass.getMethod("getChatColor", UUID.class);
                setChatColorMethod = providerClass.getMethod("setChatColor", UUID.class, String.class);
                getChatColorsMethod = providerClass.getMethod("getChatColors");
                getDefaultChatColorMethod = providerClass.getMethod("getDefaultChatColor");
            }
            refreshUtilCache();
            return provider != null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            clearCargoCache();
            cargoPlugin = plugin;
            refreshUtilCache();
            return false;
        }
    }

    public String getGroup(UUID uuid) {
        Object value = invoke("getGroup", uuid);
        return value instanceof String && !((String) value).isBlank() ? (String) value : "desconhecido";
    }

    public String getPrefix(UUID uuid) {
        Object value = invoke("getPrefix", uuid);
        return value instanceof String ? (String) value : "";
    }

    public String getNicknameColor(UUID uuid) {
        Object value = invoke("getNicknameColor", uuid);
        return value instanceof String ? (String) value : "";
    }

    public String getChatColor(UUID uuid) {
        Object value = invoke("getChatColor", uuid);
        return value instanceof String ? (String) value : "";
    }

    public boolean setChatColor(Player player, String color) {
        if (player == null) return false;
        Object value = invoke("setChatColor", player.getUniqueId(), color);
        return value instanceof Boolean && (Boolean) value;
    }

    public Map<String, String> getChatColors() {
        Object value = invoke("getChatColors");
        if (!(value instanceof Map<?, ?> source)) return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof String color) {
                result.put(key.toLowerCase(Locale.ROOT), color);
            }
        }
        return result;
    }

    public String getDefaultChatColor() {
        Object value = invoke("getDefaultChatColor");
        return value instanceof String ? ((String) value).toLowerCase(Locale.ROOT) : "branco";
    }

    public synchronized FileConfiguration getCorConfig() {
        if (!refreshUtilCache() || utilPlugin == null || getCorConfigMethod == null) return null;
        try {
            Object value = getCorConfigMethod.invoke(utilPlugin);
            return value instanceof FileConfiguration config ? config : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            getCorConfigMethod = null;
            return null;
        }
    }

    private synchronized Object invoke(String methodName, Object... args) {
        if (api == null || apiClass == null || cargoPlugin == null || !cargoPlugin.isEnabled()) {
            if (!refresh()) return null;
        }

        Method method = switch (methodName) {
            case "getGroup" -> getGroupMethod;
            case "getPrefix" -> getPrefixMethod;
            case "getNicknameColor" -> getNicknameColorMethod;
            case "getChatColor" -> getChatColorMethod;
            case "setChatColor" -> setChatColorMethod;
            case "getChatColors" -> getChatColorsMethod;
            case "getDefaultChatColor" -> getDefaultChatColorMethod;
            default -> null;
        };
        if (method == null || api == null) return null;

        try {
            return method.invoke(api, args);
        } catch (ReflectiveOperationException | LinkageError ex) {
            api = null;
            return null;
        }
    }

    private boolean refreshUtilCache() {
        Plugin util = Bukkit.getPluginManager().getPlugin("SistemaUtil");
        if (util == null || !util.isEnabled()) {
            utilPlugin = null;
            getCorConfigMethod = null;
            return false;
        }
        if (utilPlugin == util && getCorConfigMethod != null) return true;
        try {
            utilPlugin = util;
            getCorConfigMethod = util.getClass().getMethod("getCorConfig");
            return true;
        } catch (ReflectiveOperationException | LinkageError ex) {
            utilPlugin = util;
            getCorConfigMethod = null;
            return false;
        }
    }

    private void clearCargoCache() {
        apiClass = null;
        api = null;
        cargoPlugin = null;
        getGroupMethod = null;
        getPrefixMethod = null;
        getNicknameColorMethod = null;
        getChatColorMethod = null;
        setChatColorMethod = null;
        getChatColorsMethod = null;
        getDefaultChatColorMethod = null;
    }
}
