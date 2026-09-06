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
 *
 * <p>Nothing outside this class should read a {@link FileConfiguration}
 * directly - that keeps every path string, every default value and all the
 * "what if this key/file is missing or invalid" handling in one place, and
 * makes {@code /chat reload} trivial and safe to implement.</p>
 *
 * <p>This class only ever reads Bukkit permission nodes elsewhere in the
 * plugin (via {@code CommandSender#hasPermission}); it does not define,
 * store or resolve permissions itself, so a future standalone permissions
 * plugin can take over {@code chat.global}, {@code chat.staff} and
 * {@code chat.admin} without any change here.</p>
 */
public final class ConfigManager {

    private static final String PATH_LOCAL_ENABLED = "chat.local.ativado";
    private static final String PATH_LOCAL_RANGE = "chat.local.alcance";
    private static final String PATH_LOCAL_FORMAT = "chat.local.formato";

    private static final String PATH_GLOBAL_ENABLED = "chat.global.ativado";
    private static final String PATH_GLOBAL_FORMAT = "chat.global.formato";

    private static final String PATH_STAFF_ENABLED = "chat.staff.ativado";
    private static final String PATH_STAFF_FORMAT = "chat.staff.formato";

    private static final int DEFAULT_RANGE = 50;
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

    /**
     * Loads config.yml and messages.yml from disk, creating them from the
     * jar's bundled defaults first if they don't exist yet.
     *
     * <p>Safe to call repeatedly - this is exactly what {@code /chat reload}
     * calls, and it never throws even if a file is missing or malformed;
     * it simply falls back to the built-in defaults.</p>
     */
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

    // ---------------------------------------------------------------
    // Chat local
    // ---------------------------------------------------------------

    public boolean isLocalChatEnabled() {
        return plugin.getConfig().getBoolean(PATH_LOCAL_ENABLED, true);
    }

    public int getLocalChatRange() {
        int range = plugin.getConfig().getInt(PATH_LOCAL_RANGE, DEFAULT_RANGE);
        return range > 0 ? range : DEFAULT_RANGE;
    }

    public String getLocalChatFormat() {
        return plugin.getConfig().getString(PATH_LOCAL_FORMAT, DEFAULT_LOCAL_FORMAT);
    }

    // ---------------------------------------------------------------
    // Chat global
    // ---------------------------------------------------------------

    public boolean isGlobalChatEnabled() {
        return plugin.getConfig().getBoolean(PATH_GLOBAL_ENABLED, true);
    }

    public String getGlobalChatFormat() {
        return plugin.getConfig().getString(PATH_GLOBAL_FORMAT, DEFAULT_GLOBAL_FORMAT);
    }

    // ---------------------------------------------------------------
    // Chat da staff
    // ---------------------------------------------------------------

    public boolean isStaffChatEnabled() {
        return plugin.getConfig().getBoolean(PATH_STAFF_ENABLED, true);
    }

    public String getStaffChatFormat() {
        return plugin.getConfig().getString(PATH_STAFF_FORMAT, DEFAULT_STAFF_FORMAT);
    }

    // ---------------------------------------------------------------
    // messages.yml
    // ---------------------------------------------------------------

    /**
     * Returns the already colorized message configured under {@code key},
     * falling back to this plugin's built-in default text (also colorized)
     * if the key or the file itself is missing/invalid.
     */
    public String getMessage(String key) {
        String fallback = DEFAULT_MESSAGES.getOrDefault(key, "");
        String raw = messages != null ? messages.getString(key, fallback) : fallback;
        return MessageUtil.colorize(raw);
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
        defaults.put("apenas-jogadores", "&cApenas jogadores podem utilizar este comando.");
        defaults.put("ninguem-por-perto", "&7Ninguém está por perto para ver sua mensagem.");
        defaults.put("reload-sucesso", "&aConfiguração recarregada com sucesso.");
        return Collections.unmodifiableMap(defaults);
    }
}
