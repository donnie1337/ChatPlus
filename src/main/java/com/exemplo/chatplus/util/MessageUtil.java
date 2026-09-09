package com.exemplo.chatplus.util;

import org.bukkit.ChatColor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helpers for safe message formatting and command argument handling. */
public final class MessageUtil {
    private static final Pattern GRADIENT = Pattern.compile("<gradient:(#[0-9a-fA-F]{6}):(#[0-9a-fA-F]{6})>(.*?)</gradient>", Pattern.DOTALL);

    private MessageUtil() {
        // Utility class.
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', convertGradients(text));
    }

    /**
     * Converts the MiniMessage-style gradient used by CargoPlus into legacy
     * RGB sequences understood by Spigot/Bungee TextComponent.
     */
    private static String convertGradients(String text) {
        String result = text;
        Matcher matcher = GRADIENT.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            int start = Integer.parseInt(matcher.group(1).substring(1), 16);
            int end = Integer.parseInt(matcher.group(2).substring(1), 16);
            String content = matcher.group(3);
            StringBuilder replacement = new StringBuilder();
            int visibleCharacters = content.codePointCount(0, content.length());
            int index = 0;
            for (int offset = 0; offset < content.length();) {
                int codePoint = content.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                if (character.equals("\n") || character.equals("\r")) {
                    replacement.append(character);
                    offset += Character.charCount(codePoint);
                    continue;
                }
                double progress = visibleCharacters <= 1 ? 0.0 : (double) index / (visibleCharacters - 1);
                int rgb = interpolate(start, end, progress);
                replacement.append(toLegacyHex(rgb)).append(character);
                index++;
                offset += Character.charCount(codePoint);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
            result = null;
        }
        if (result == null) {
            matcher.appendTail(buffer);
            return buffer.toString();
        }
        return result;
    }

    private static int interpolate(int start, int end, double progress) {
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;
        int r = (int) Math.round(sr + (er - sr) * progress);
        int g = (int) Math.round(sg + (eg - sg) * progress);
        int b = (int) Math.round(sb + (eb - sb) * progress);
        return (r << 16) | (g << 8) | b;
    }

    private static String toLegacyHex(int rgb) {
        String hex = String.format("%06X", rgb);
        StringBuilder builder = new StringBuilder("&x");
        for (int i = 0; i < hex.length(); i++) {
            builder.append('&').append(hex.charAt(i));
        }
        return builder.toString();
    }

    /**
     * Removes all player-controlled legacy color/formatting sequences,
     * including the six-digit legacy hex form, before insertion into a
     * trusted template. The chat color is supplied exclusively by CargoPlus.
     */
    public static String sanitizePlayerText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String sanitized = text.replaceAll("(?i)(?:[&§]x(?:[&§][0-9a-f]){6}|[&§][0-9a-fk-or])", "");
        return sanitized.replace("\u00a7", "");
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
