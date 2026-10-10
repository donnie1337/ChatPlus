package com.exemplo.chatplus.command;

import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.service.PlaytimeTestService;
import com.exemplo.chatplus.util.MessageUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InsigniasGui implements Listener {

    private static final int MAIN_SIZE = 27;
    private static final int ARSENAL_SIZE = 54;
    private static final int OWNED_MIN_SIZE = 36;
    private static final int OWNED_SLOT = 11;
    private static final int ARSENAL_SLOT = 15;
    private static final int EMPTY_SLOT_36 = 13;

    private static final int[] BADGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final ConfigManager config;
    private final PlaytimeTestService playtimeTests;

    public InsigniasGui(ConfigManager config, PlaytimeTestService playtimeTests) {
        this.config = config;
        this.playtimeTests = playtimeTests;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(Type.MAIN), MAIN_SIZE, "§8Insígnias");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(player);
            headMeta.setDisplayName("§bSuas insígnias");
            headMeta.setLore(List.of(
                    "",
                    "§7Veja todas as insígnias",
                    "§7que você já conquistou.",
                    "",
                    "§eClique para visualizar"
            ));
            head.setItemMeta(headMeta);
        }
        inventory.setItem(OWNED_SLOT, head);

        inventory.setItem(ARSENAL_SLOT, item(Material.BOOK, "§eArsenal de insígnias", List.of(
                "",
                "§7Veja todas as insígnias",
                "§7disponíveis no servidor.",
                "",
                "§eClique para visualizar"
        )));

        player.openInventory(inventory);
    }

    public void openOwned(Player player) {
        List<Badge> badges = ownedBadges(player);
        int size = ownedInventorySize(badges.size());
        Inventory inventory = Bukkit.createInventory(new Holder(Type.OWNED), size,
                "§8Insígnias §7→ §fSuas insígnias");

        if (badges.isEmpty()) {
            inventory.setItem(EMPTY_SLOT_36, item(Material.GRAY_DYE, "§7Nenhuma insígnia", List.of(
                    "",
                    "§7Você ainda não conquistou",
                    "§7nenhuma insígnia."
            )));
        } else {
            fillBadges(inventory, badges, true);
        }

        inventory.setItem(backSlot(size), item(Material.ARROW, "§cVoltar", List.of(
                "",
                "§7Voltar ao menu de insígnias."
        )));
        player.openInventory(inventory);
    }

    public void openArsenal(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(Type.ARSENAL), ARSENAL_SIZE,
                "§8Insígnias §7→ §fArsenal");

        fillBadges(inventory, arsenalBadges(), false);

        inventory.setItem(backSlot(ARSENAL_SIZE), item(Material.ARROW, "§cVoltar", List.of(
                "",
                "§7Voltar ao menu de insígnias."
        )));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (holder.type() == Type.MAIN) {
            if (slot == OWNED_SLOT) openOwned(player);
            else if (slot == ARSENAL_SLOT) openArsenal(player);
            return;
        }

        if (slot == backSlot(event.getView().getTopInventory().getSize())) open(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private int ownedInventorySize(int badgeCount) {
        if (badgeCount <= 14) return OWNED_MIN_SIZE;
        if (badgeCount <= 21) return 45;
        return 54;
    }

    private int backSlot(int inventorySize) {
        return inventorySize - 5;
    }

    private void fillBadges(Inventory inventory, List<Badge> badges, boolean owned) {
        int index = 0;
        for (Badge badge : badges) {
            if (index >= BADGE_SLOTS.length) break;
            inventory.setItem(BADGE_SLOTS[index++], badgeItem(badge, owned));
        }
    }

    private List<Badge> ownedBadges(Player player) {
        List<Badge> badges = new ArrayList<>();
        long playedHours = playedHours(player);

        for (Badge badge : playtimeBadges()) {
            if (playedHours >= badge.requiredHours()) {
                badges.add(badge);
            }
        }

        String skill = currentTopSkill(player);
        String skillTag = currentTopSkillTag(player);
        if (!skill.isBlank() && !skillTag.isBlank()) {
            badges.add(skillBadge(skill, skillTag));
        }

        String magnataTag = currentMagnataTag(player);
        if (!magnataTag.isBlank()) {
            badges.add(magnataBadge(magnataTag));
        }

        return badges;
    }

    private List<Badge> arsenalBadges() {
        List<Badge> badges = new ArrayList<>(playtimeBadges());
        for (Map.Entry<String, String> entry : allSkillBadges().entrySet()) {
            badges.add(skillBadge(entry.getKey(), entry.getValue()));
        }
        badges.add(magnataBadge("§a[$]"));
        return badges;
    }

    private List<Badge> playtimeBadges() {
        List<Badge> badges = new ArrayList<>();
        for (Map<?, ?> entry : config.getPlaytimeTagRanges()) {
            Object rawHours = entry.get("horas");
            Object rawTag = entry.get("tag");
            if (!(rawHours instanceof Number number) || rawTag == null) continue;

            long hours = Math.max(0L, number.longValue());
            String tag = String.valueOf(rawTag).trim();
            if (tag.isBlank()) continue;

            badges.add(new Badge(
                    tag,
                    "§eInsígnia de tempo",
                    hours,
                    "§7Conquistada ao atingir",
                    "§f" + hours + " horas §7de tempo jogado."
            ));
        }

        badges.sort(Comparator.comparingLong(Badge::requiredHours));
        return badges;
    }

    private Badge skillBadge(String skill, String tag) {
        return new Badge(
                tag == null || tag.isBlank() ? "§d[⚡]" : tag.trim(),
                "§d" + skill,
                0L,
                "§7Insígnia exclusiva do §fTop 1",
                "§7em §f" + skill + "§7."
        );
    }

    private Badge magnataBadge(String tag) {
        return new Badge(
                tag == null || tag.isBlank() ? "§a[$]" : tag.trim(),
                "§aMagnata",
                0L,
                "§7Insígnia exclusiva do §fTop 1",
                "§7no ranking de §fCoins§7."
        );
    }

    private ItemStack badgeItem(Badge badge, boolean owned) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(badge.line1());
        lore.add(badge.line2());
        if (owned) {
            lore.add("");
            lore.add("§aInsígnia conquistada");
        }

        return item(
                Material.NAME_TAG,
                MessageUtil.colorize(badge.tag()) + " §r" + badge.name(),
                lore
        );
    }

    private long playedHours(Player player) {
        return playtimeTests.resolveHours(player);
    }

    private String currentMagnataTag(Player player) {
        Plugin economia = Bukkit.getPluginManager().getPlugin("EconomiaPlus");
        if (economia == null || !economia.isEnabled() || player == null) return "";

        try {
            Method method = economia.getClass().getMethod("getMagnataChatTag", java.util.UUID.class);
            Object result = method.invoke(economia, player.getUniqueId());
            return result == null ? "" : String.valueOf(result).trim();
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private String currentTopSkill(Player player) {
        Plugin habilidades = Bukkit.getPluginManager().getPlugin("HabilidadesPlus");
        if (habilidades == null || !habilidades.isEnabled()) return "";
        try {
            Method method = habilidades.getClass().getMethod(
                    "getTop1SkillDisplayName", java.util.UUID.class);
            Object result = method.invoke(habilidades, player.getUniqueId());
            return result == null ? "" : String.valueOf(result).trim();
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private String currentTopSkillTag(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "";
        try {
            String tag = PlaceholderAPI.setPlaceholders(player, "%habilidade_tag%");
            return tag == null || tag.equals("%habilidade_tag%") ? "" : tag.trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private Map<String, String> allSkillBadges() {
        Plugin habilidades = Bukkit.getPluginManager().getPlugin("HabilidadesPlus");
        if (habilidades == null || !habilidades.isEnabled()) return Map.of();

        try {
            Method method = habilidades.getClass().getMethod("getTop1Insignias");
            Object result = method.invoke(habilidades);
            if (!(result instanceof Map<?, ?> raw)) return Map.of();

            Map<String, String> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                converted.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return converted;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return Map.of();
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private record Badge(String tag, String name, long requiredHours, String line1, String line2) {}

    private record Holder(Type type) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private enum Type {
        MAIN,
        OWNED,
        ARSENAL
    }
}
