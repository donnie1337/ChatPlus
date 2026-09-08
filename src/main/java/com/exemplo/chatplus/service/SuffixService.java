package com.exemplo.chatplus.service;

import com.exemplo.chatplus.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Resolve sufixos por prioridade/permissão e deixa o ranking desacoplado do chat. */
public final class SuffixService {
    private final ConfigManager config;
    private final VisualIdentityBridge visual;

    public SuffixService(ConfigManager config, VisualIdentityBridge visual) {
        this.config = config;
        this.visual = visual;
    }

    public String resolve(Player player) {
        if (player == null || !config.isSuffixEnabled()) return "";
        ConfigurationSection section = config.getPlugin().getConfig().getConfigurationSection("sufixos.lista");
        if (section == null) return "";

        List<SuffixRule> rules = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection rule = section.getConfigurationSection(key);
            if (rule == null) continue;
            String permission = rule.getString("permissao", "");
            if (permission.isBlank() || !player.hasPermission(permission)) continue;
            rules.add(new SuffixRule(
                    rule.getInt("prioridade", 0),
                    rule.getString("icone", ""),
                    rule.getString("texto", "")
            ));
        }

        rules.sort(Comparator.comparingInt(SuffixRule::priority).reversed());
        if (rules.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (SuffixRule rule : rules) {
            if (!rule.icon().isBlank()) result.append(visual.icon(rule.icon()));
            if (!rule.text().isBlank()) result.append(rule.text());
        }
        return result.toString();
    }

    private record SuffixRule(int priority, String icon, String text) {}
}
