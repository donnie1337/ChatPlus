package com.exemplo.chatplus.util;

import org.bukkit.ChatColor;

import java.util.Map;

/** Helpers for safe message formatting and command argument handling. */
public final class MessageUtil {

    private MessageUtil() {
        // Utility class.
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Removes player-controlled legacy formatting codes before the message is
     * inserted into a trusted chat template. The chat color is then supplied
     * exclusively by CargoPlus based on the player's saved /cor selection.
     */
    public static String sanitizePlayerText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("(?i)[&§][0-9A-FK-OR]", "");
    }

    /**
     * Colorizes the trusted template first and only then inserts placeholder
     * values. Placeholder text is never interpreted as part of the template.
     */
    public static String apply(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        String result = colorize(template);
        if (placeholders == null || placeholders.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(key, value);
        }
        return result;
    }

    public static String join(String[] args, int startIndex) {
        if (args == null || startIndex < 0 || startIndex >= args.length) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }
}
