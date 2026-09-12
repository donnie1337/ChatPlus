package com.exemplo.chatplus.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Centraliza a integracao do ChatPlus com o CargoPlus. */
public final class CargoPlusBridge {
    private static final String API_CLASS_NAME = "com.cargoplus.api.CargoPlusAPI";

    private Class<?> apiClass;
    private Object api;
    private Plugin cargoPlugin;
    private Method getGroupMethod;
    private Method getPrefixMethod;
    private Method getAnimatedPrefixMethod;
    private Method getNicknameColorMethod;
    private Method getChatColorMethod;
    private Method setChatColorMethod;
    private Method hasCargoPermissionMethod;
    private Method getChatColorsMethod;
    private Method getDefaultChatColorMethod;

    private Plugin clanPlugin;
    private Method getPlayerTagMethod;

    public synchronized boolean refresh() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (plugin == null || !plugin.isEnabled()) {
            clearCargoCache();
            return false;
        }

        try {
            Class<?> clazz = Class.forName(API_CLASS_NAME, true, plugin.getClass().getClassLoader());
            var registration = Bukkit.getServicesManager().getRegistration(clazz);
            if (registration == null) {
                clearCargoCache();
                cargoPlugin = plugin;
                apiClass = clazz;
                refreshClanBridge();
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
                try {
                    getAnimatedPrefixMethod = providerClass.getMethod("getAnimatedPrefix", UUID.class);
                } catch (NoSuchMethodException ignored) {
                    getAnimatedPrefixMethod = null;
                }
                getNicknameColorMethod = providerClass.getMethod("getNicknameColor", UUID.class);
                getChatColorMethod = providerClass.getMethod("getChatColor", UUID.class);
                setChatColorMethod = providerClass.getMethod("setChatColor", UUID.class, String.class);
                hasCargoPermissionMethod = providerClass.getMethod("hasCargoPermission", UUID.class, String.class);
                getChatColorsMethod = providerClass.getMethod("getChatColors");
                getDefaultChatColorMethod = providerClass.getMethod("getDefaultChatColor");
            }
            refreshClanBridge();
            return provider != null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            clearCargoCache();
            cargoPlugin = plugin;
            refreshClanBridge();
            return false;
        }
    }

    public String getGroup(UUID uuid) {
        Object value = invoke("getGroup", uuid);
        return value instanceof String && !((String) value).isBlank() ? (String) value : "desconhecido";
    }

    public String getPrefix(UUID uuid) {
        Object animated = invoke("getAnimatedPrefix", uuid);
        if (animated instanceof String) return (String) animated;
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

    public boolean hasCargoPermission(UUID uuid, String permission) {
        Object value = invoke("hasCargoPermission", uuid, permission);
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

    /** Retorna a tag do ClanPlus exatamente como foi configurada, incluindo cores. */
    public String getClanTag(UUID uuid) {
        if (uuid == null) return "";
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ClanPlus");
        if (plugin == null || !plugin.isEnabled()) {
            clanPlugin = null;
            getPlayerTagMethod = null;
            return "";
        }
        if (clanPlugin != plugin || getPlayerTagMethod == null) refreshClanBridge();
        if (getPlayerTagMethod == null) return "";
        try {
            Object result = getPlayerTagMethod.invoke(clanPlugin, uuid);
            return result instanceof String ? (String) result : "";
        } catch (ReflectiveOperationException | LinkageError ex) {
            getPlayerTagMethod = null;
            return "";
        }
    }

    private synchronized void refreshClanBridge() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ClanPlus");
        if (plugin == null || !plugin.isEnabled()) {
            clanPlugin = null;
            getPlayerTagMethod = null;
            return;
        }
        try {
            clanPlugin = plugin;
            getPlayerTagMethod = plugin.getClass().getMethod("getPlayerTag", UUID.class);
        } catch (ReflectiveOperationException | LinkageError ex) {
            clanPlugin = plugin;
            getPlayerTagMethod = null;
        }
    }

    private synchronized Object invoke(String methodName, Object... args) {
        if (api == null || apiClass == null || cargoPlugin == null || !cargoPlugin.isEnabled()) {
            if (!refresh()) return null;
        } else if (!isCurrentServiceProvider()) {
            if (!refresh()) return null;
        }

        Method method = switch (methodName) {
            case "getGroup" -> getGroupMethod;
            case "getPrefix" -> getPrefixMethod;
            case "getAnimatedPrefix" -> getAnimatedPrefixMethod;
            case "getNicknameColor" -> getNicknameColorMethod;
            case "getChatColor" -> getChatColorMethod;
            case "setChatColor" -> setChatColorMethod;
            case "hasCargoPermission" -> hasCargoPermissionMethod;
            case "getChatColors" -> getChatColorsMethod;
            case "getDefaultChatColor" -> getDefaultChatColorMethod;
            default -> null;
        };
        if (method == null || api == null) return null;

        try {
            return method.invoke(api, args);
        } catch (ReflectiveOperationException | LinkageError ex) {
            if ("getAnimatedPrefix".equals(methodName)) {
                getAnimatedPrefixMethod = null;
                return null;
            }
            api = null;
            return null;
        }
    }

    private boolean isCurrentServiceProvider() {
        if (apiClass == null || api == null) return false;
        try {
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return false;
            Method providerMethod = registration.getClass().getMethod("getProvider");
            return providerMethod.invoke(registration) == api;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private void clearCargoCache() {
        apiClass = null;
        api = null;
        cargoPlugin = null;
        getGroupMethod = null;
        getPrefixMethod = null;
        getAnimatedPrefixMethod = null;
        getNicknameColorMethod = null;
        getChatColorMethod = null;
        setChatColorMethod = null;
        hasCargoPermissionMethod = null;
        getChatColorsMethod = null;
        getDefaultChatColorMethod = null;
    }
}
