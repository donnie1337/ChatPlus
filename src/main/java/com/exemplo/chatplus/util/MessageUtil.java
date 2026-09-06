package com.exemplo.chatplus.util;

import org.bukkit.ChatColor;

import java.util.Map;

/**
 * Small set of static helpers for turning raw strings from config.yml /
 * messages.yml into what the player actually sees.
 *
 * <p>Colorizing text and substituting {placeholder} tokens is a pure,
 * stateless operation used everywhere in the plugin (channel formats,
 * messages.yml entries, etc.), so a static utility is enough - there is
 * no reason to instantiate anything here.</p>
 */
public final class MessageUtil {

    private MessageUtil() {
        // Utility class, not meant to be instantiated.
    }

    /**
     * Translates '&' colour codes (e.g. &a, &l, &c) into the section-symbol
     * codes the client understands.
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Replaces every {placeholder} present in the template with the matching
     * value from the given map, then colorizes the result.
     *
     * <p>Supporting a new placeholder anywhere in the plugin (channel
     * formats, future messages, etc.) only requires adding another entry to
     * the map passed in here - nothing in this method needs to change.</p>
     */
    public static String apply(String template, Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(entry.getKey(), value);
        }
        return colorize(result);
    }

    /**
     * Joins command arguments (e.g. from /l, /g, /s) back into a single
     * message, starting at {@code startIndex}, and trims the result.
     */
    public static String join(String[] args, int startIndex) {
        if (args == null || args.length <= startIndex) {
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
