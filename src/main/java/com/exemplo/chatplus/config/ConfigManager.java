package com.exemplo.chatplus.config;

import com.exemplo.chatplus.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Centralises every access to config.yml and messages.yml.
 */
public final class ConfigManager {

    private static final String PATH_LOCAL_ENABLED = "chat.local.ativado";
    private static final String PATH_LOCAL_RANGE = "chat.local.alcance";
    private static final String PATH_LOCAL_FORMAT = "chat.local.formato";
    private static final String PATH_MESSAGE_LIMIT = "chat.limite-mensagem";

    private static final String PATH_GLOBAL_ENABLED = "chat.global.ativado";
    private static final String PATH_GLOBAL_FORMAT = "chat.global.formato";

    private static final String PATH_STAFF_ENABLED = "chat.staff.ativado";
    private static final String PATH_STAFF_FORMAT = "chat.staff.formato";

    private static final int DEFAULT_RANGE = 50;
    private static final int DEFAULT_MESSAGE_LIMIT = 256;
    private static final String DEFAULT_LOCAL_FORMAT = "&7[L] &f{player}&7: &f{message}";
    private static final String DEFAULT_GLOBAL_FORMAT = "&6[G] &f{player}&7: &f{message}";
    private static final String DEFAULT_STAFF_FORMAT = "&c[S] &f{player}&7: &f{message}";

    private static final Map<String, String> DEFAULT_MESSAGES = buildDefaultMessages();

    private final JavaPlugin plugin;
    private final File messagesFile;
    private FileConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        loadMessages();
    }

    private void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        FileConfiguration loaded = YamlConfiguration.loadConfiguration(messagesFile);

        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Não foi possível carregar os valores padrão de messages.yml, usando apenas o arquivo em disco.",
                    exception);
        }

        this.messages = loaded;
    }

    public boolean isLocalChatEnabled() {
        return plugin.getConfig().getBoolean(PATH_LOCAL_ENABLED, true);
    }

    public int getLocalChatRange() {
        int range = plugin.getConfig().getInt(PATH_LOCAL_RANGE, DEFAULT_RANGE);
        return range > 0 ? range : DEFAULT_RANGE;
    }

    public int getMaxMessageLength() {
        int limit = plugin.getConfig().getInt(PATH_MESSAGE_LIMIT, DEFAULT_MESSAGE_LIMIT);
        return limit > 0 ? Math.min(limit, 1024) : DEFAULT_MESSAGE_LIMIT;
    }

    public boolean isMessageLengthValid(String message) {
        return message != null && message.codePointCount(0, message.length()) <= getMaxMessageLength();
    }

    public String getLocalChatFormat() {
        return getConfiguredString(PATH_LOCAL_FORMAT, DEFAULT_LOCAL_FORMAT);
    }

    public boolean isGlobalChatEnabled() {
        return plugin.getConfig().getBoolean(PATH_GLOBAL_ENABLED, true);
    }

    public String getGlobalChatFormat() {
        return getConfiguredString(PATH_GLOBAL_FORMAT, DEFAULT_GLOBAL_FORMAT);
    }

    public boolean isStaffChatEnabled() {
        return plugin.getConfig().getBoolean(PATH_STAFF_ENABLED, true);
    }

    public String getStaffChatFormat() {
        return getConfiguredString(PATH_STAFF_FORMAT, DEFAULT_STAFF_FORMAT);
    }

    public String getMessage(String key) {
        String fallback = DEFAULT_MESSAGES.getOrDefault(key, "");
        String raw = messages != null ? messages.getString(key, fallback) : fallback;
        if (raw == null) {
            raw = fallback;
        }
        return MessageUtil.colorize(raw);
    }

    private String getConfiguredString(String path, String fallback) {
        String value = plugin.getConfig().getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, String> buildDefaultMessages() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("sem-permissao", "&cVocê não possui permissão para utilizar este chat.");
        defaults.put("chat-global-desativado", "&cO chat global está desativado.");
        defaults.put("chat-local-desativado", "&cO chat local está desativado.");
        defaults.put("chat-staff-desativado", "&cO chat da staff está desativado.");
        defaults.put("uso-global", "&cUso correto: /g <mensagem>");
        defaults.put("uso-local", "&cUso correto: /l <mensagem>");
        defaults.put("uso-staff", "&cUso correto: /s <mensagem>");
        defaults.put("uso-chat-reload", "&cUso correto: /chat reload");
        defaults.put("mensagem-vazia", "&cVocê precisa informar uma mensagem.");
        defaults.put("mensagem-muito-longa", "&cA mensagem excede o limite permitido.");
        defaults.put("apenas-jogadores", "&cApenas jogadores podem utilizar este comando.");
        defaults.put("ninguem-por-perto", "&7Ninguém está por perto para ver sua mensagem.");
        defaults.put("reload-sucesso", "&aConfiguração recarregada com sucesso.");
        return Collections.unmodifiableMap(defaults);
    }
}
