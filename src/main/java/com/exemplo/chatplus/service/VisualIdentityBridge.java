package com.exemplo.chatplus.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/** Integra o ChatPlus ao sistema visual central do UtilidadesPlus sem dependência de compilação. */
public final class VisualIdentityBridge {
    private Plugin utilPlugin;
    private Method getVisualText;
    private Method icon;
    private Method format;

    public synchronized void refresh() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (plugin == null || !plugin.isEnabled()) {
            utilPlugin = null;
            getVisualText = null;
            icon = null;
            format = null;
            return;
        }
        if (utilPlugin == plugin && icon != null && format != null) return;
        try {
            Method getter = plugin.getClass().getMethod("getVisualText");
            Object visual = getter.invoke(plugin);
            if (visual == null) throw new IllegalStateException("VisualText indisponível");
            utilPlugin = plugin;
            getVisualText = getter;
            icon = visual.getClass().getMethod("icon", String.class);
            format = visual.getClass().getMethod("format", String.class);
        } catch (ReflectiveOperationException | LinkageError ex) {
            utilPlugin = plugin;
            getVisualText = null;
            icon = null;
            format = null;
        }
    }

    public String icon(String name) {
        refresh();
        if (icon == null || getVisualText == null || utilPlugin == null) return "";
        try {
            Object visual = getVisualText.invoke(utilPlugin);
            Object result = icon.invoke(visual, name);
            return result instanceof String ? (String) result : "";
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    public String format(String text) {
        refresh();
        if (format == null || getVisualText == null || utilPlugin == null) return text == null ? "" : text;
        try {
            Object visual = getVisualText.invoke(utilPlugin);
            Object result = format.invoke(visual, text);
            return result instanceof String ? (String) result : text;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return text == null ? "" : text;
        }
    }
}
