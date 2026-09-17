package com.exemplo.chatplus.service;

import com.exemplo.chatplus.ChatPlus;
import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.hook.CargoHook;
import com.exemplo.chatplus.hook.ClanHook;
import com.exemplo.chatplus.util.MessageUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatService {
    private final ChatPlus plugin;
    private final ConfigManager config;
    private final CargoHook cargo;
    private final ClanHook clan;

    public ChatService(ChatPlus plugin, ConfigManager config, CargoHook cargo, ClanHook clan) {
        this.plugin = plugin;
        this.config = config;
        this.cargo = cargo;
        this.clan = clan;
    }

    public void sendChat(Player player, String message) {
        String cargoColor = cargo.getColor(player.getUniqueId());
        String nickname = cargo.getPrefix(player.getUniqueId()) + player.getName();
        BaseComponent[] components = TextComponent.fromLegacyText("§e§lᴄʜᴀᴛ §8• §r");
        for (BaseComponent component : components) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, player.getName()));
        }
        components = appendPlayerNickname(components, player, nickname, cargoColor);
        components = appendMessage(components, message);
        for (BaseComponent component : components) {
            Bukkit.getOnlinePlayers().forEach(target -> target.spigot().sendMessage(component));
        }
    }

    private BaseComponent[] appendPlayerNickname(BaseComponent[] base, Player player, String nickname, String cargoColor) {
        java.util.ArrayList<BaseComponent> components = new java.util.ArrayList<>();
        java.util.Collections.addAll(components, base);
        HoverEvent nicknameHover = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(buildPlayerProfileHover(player, nickname, cargoColor)).create()
        );
        BaseComponent[] nicknameComponents = TextComponent.fromLegacyText(nickname);
        for (BaseComponent component : nicknameComponents) {
            component.setHoverEvent(nicknameHover);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, player.getName()));
            components.add(component);
        }
        return components.toArray(new BaseComponent[0]);
    }

    private BaseComponent[] appendMessage(BaseComponent[] base, String message) {
        java.util.ArrayList<BaseComponent> components = new java.util.ArrayList<>();
        java.util.Collections.addAll(components, base);
        BaseComponent[] messageComponents = TextComponent.fromLegacyText("§f: " + MessageUtil.colorize(message));
        java.util.Collections.addAll(components, messageComponents);
        return components.toArray(new BaseComponent[0]);
    }

    private String buildPlayerProfileHover(Player player, String nickname, String cargoColor) {
        String safeCargoColor = cargoColor == null || cargoColor.isBlank() ? "§f" : cargoColor;
        String cargoValue = cargo.getGroup(player.getUniqueId());
        String clanValue = getClan(player);
        boolean hasClan = clanValue != null && !clanValue.isBlank();
        String moneyValue = getMoney(player);
        double kdr = getKdr(player);

        StringBuilder lore = new StringBuilder();
        lore.append("ㅤ§f§l◆ INFORMAÇÕES ◆ㅤ\n");
        lore.append("ㅤ\n");
        lore.append("ㅤ  §7ᴊᴏɢᴀᴅᴏʀ §8• ").append(safeCargoColor).append(nickname.replaceFirst("§[0-9a-fk-or]", "")).append("\n");
        lore.append("ㅤ  §7ᴄᴀʀɢᴏ §8• ").append(safeCargoColor).append(capitalizeGroupName(cargoValue)).append("\n");
        if (hasClan) lore.append("ㅤ  §7ᴄʟᴀɴ §8• §f[").append(clanValue).append("§f]\n");
        else lore.append("ㅤ  §7ᴄʟᴀɴ §8• §7Nenhum\n");
        lore.append("ㅤ  §7ᴍᴏᴇᴅᴀs §8• §f").append(moneyValue).append("\n");
        lore.append("ㅤ  §7ᴋᴅʀ §8• §f").append(String.format(Locale.US, "%.2f", kdr)).append("\n");
        lore.append("ㅤ  §7ᴛᴇᴍᴘᴏ ᴏɴʟɪɴᴇ §8• §f").append(formatOnlineTime(player)).append("\n");
        lore.append("ㅤ\n");
        lore.append("\nㅤ§bClique aqui para interagir com este jogador.");
        return lore.toString();
    }

    private String getClan(Player player) {
        try {
            String value = clan.getClan(player.getUniqueId());
            return value == null ? "" : value;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String getMoney(Player player) {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null) return "0,00";
            Object provider = economyClass.cast(((org.bukkit.plugin.RegisteredServiceProvider<?>) registration).getProvider());
            Method balanceMethod = economyClass.getMethod("getBalance", OfflinePlayer.class);
            Object balance = balanceMethod.invoke(provider, player);
            if (balance instanceof Number number) {
                DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("pt", "BR"));
                DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
                return format.format(number.doubleValue());
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {}
        return "0,00";
    }

    private double getKdr(Player player) {
        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = player.getStatistic(Statistic.DEATHS);
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    private String formatOnlineTime(Player player) {
        long minutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L;
        long days = minutes / (24L * 60L);
        long hours = (minutes % (24L * 60L)) / 60L;
        long mins = minutes % 60L;
        if (days > 0) return days + "d " + hours + "h " + mins + "m";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    private String capitalizeGroupName(String group) {
        if (group == null || group.isBlank()) return "Membro";
        return group.substring(0, 1).toUpperCase(Locale.ROOT) + group.substring(1).toLowerCase(Locale.ROOT);
    }

    private void addClanTag(List<BaseComponent> components, String clanTag, String cargoColor) {
        BaseComponent[] tag = TextComponent.fromLegacyText("§8[" + MessageUtil.colorize(clanTag) + "§8]");
        for (BaseComponent component : tag) {
            components.add(component);
        }
    }

    private HoverEvent buildCargoHover(String cargoColor, String cargoName) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(cargoColor + "Cargo: " + capitalizeGroupName(cargoName)).create());
    }

    private void decorateChatTypeHover(BaseComponent[] components, String chatType) { }
}
