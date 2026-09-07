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

    public synchronized boolean refresh() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (plugin == null || !plugin.isEnabled()) {
            cargoPlugin = null;
            apiClass = null;
            api = null;
            return false;
        }

        try {
            Class<?> clazz = Class.forName(API_CLASS_NAME, true, plugin.getClass().getClassLoader());
            var registration = Bukkit.getServicesManager().getRegistration(clazz);
            if (registration == null) {
                cargoPlugin = plugin;
                apiClass = clazz;
                api = null;
                return false;
            }
            Object provider = registration.getClass().getMethod("getProvider").invoke(registration);
            cargoPlugin = plugin;
            apiClass = clazz;
            api = provider;
            return provider != null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            cargoPlugin = plugin;
            apiClass = null;
            api = null;
            return false;
        }
    }

    public String getPrefix(UUID uuid) {
        Object value = invoke("getPrefix", new Class<?>[]{UUID.class}, uuid);
        return value instanceof String ? (String) value : "";
    }

    public String getNicknameColor(UUID uuid) {
        Object value = invoke("getNicknameColor", new Class<?>[]{UUID.class}, uuid);
        return value instanceof String ? (String) value : "";
    }

    public String getChatColor(UUID uuid) {
        Object value = invoke("getChatColor", new Class<?>[]{UUID.class}, uuid);
        return value instanceof String ? (String) value : "";
    }

    public boolean setChatColor(Player player, String color) {
        if (player == null) return false;
        Object value = invoke("setChatColor", new Class<?>[]{UUID.class, String.class}, player.getUniqueId(), color);
        return value instanceof Boolean && (Boolean) value;
    }

    public Map<String, String> getChatColors() {
        Object value = invoke("getChatColors", new Class<?>[0]);
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
        Object value = invoke("getDefaultChatColor", new Class<?>[0]);
        return value instanceof String ? ((String) value).toLowerCase(Locale.ROOT) : "branco";
    }

    /**
     * Lê a configuração exclusiva do /cor no SistemaUtil.
     * O ChatPlus não mantém uma segunda paleta/configuração quando o SistemaUtil está ativo.
     */
    public synchronized FileConfiguration getCorConfig() {
        Plugin util = Bukkit.getPluginManager().getPlugin("SistemaUtil");
        if (util == null || !util.isEnabled()) return null;
        try {
            Method method = util.getClass().getMethod("getCorConfig");
            Object value = method.invoke(util);
            return value instanceof FileConfiguration config ? config : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private synchronized Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (api == null || apiClass == null || cargoPlugin == null || !cargoPlugin.isEnabled()) {
            if (!refresh()) return null;
        }
        try {
            return apiClass.getMethod(methodName, parameterTypes).invoke(api, args);
        } catch (ReflectiveOperationException | LinkageError ex) {
            api = null;
            return null;
        }
    }
}
