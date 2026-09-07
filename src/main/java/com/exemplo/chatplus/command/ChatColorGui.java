package com.exemplo.chatplus.command;

import com.exemplo.chatplus.service.ChatColorService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatColorGui implements Listener {
    private static final String TITLE = "§8Escolha a cor do chat";
    private final JavaPlugin plugin;
    private final ChatColorService colors;

    public ChatColorGui(JavaPlugin plugin, ChatColorService colors) {
        this.plugin = plugin;
        this.colors = colors;
    }

    public void open(Player player) {
        Inventory inventory = plugin.getServer().createInventory(null, 27, TITLE);
        int slot = 10;
        String selected = colors.getCurrentColorName(player);

        for (Map.Entry<String, String> entry : colors.getColors().entrySet()) {
            ChatColor color = colors.resolve(entry.getKey());
            ItemStack item = new ItemStack(woolFor(color));
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            meta.setDisplayName(color + entry.getKey());
            List<String> lore = new ArrayList<>();
            lore.add("§7Cor do texto que você digita no chat.");
            lore.add("§7Exemplo: " + color + "Sua mensagem");
            if (entry.getKey().equalsIgnoreCase(selected)) {
                lore.add("§a✓ Cor atualmente selecionada");
            } else {
                lore.add("§eClique para selecionar");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);

            slot++;
            if (slot == 17) slot = 19;
            if (slot > 25) break;
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // A GUI e somente o inventario superior. Cliques no inventario do
        // jogador nunca podem selecionar uma cor nem mover itens.
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        if (event.getCursor() != null && !event.getCursor().getType().isAir()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        String selected = ChatColor.stripColor(meta.getDisplayName()).trim().toLowerCase(Locale.ROOT);
        if (!colors.getColors().containsKey(selected)) return;
        if (colors.setColor(player, selected)) {
            player.sendMessage(ChatColor.GREEN + "Cor do chat alterada para "
                    + colors.resolve(selected) + selected + "§r.");
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    private static Material woolFor(ChatColor color) {
        return switch (color) {
            case WHITE -> Material.WHITE_WOOL;
            case GRAY, DARK_GRAY -> Material.GRAY_WOOL;
            case BLACK -> Material.BLACK_WOOL;
            case RED -> Material.RED_WOOL;
            case GREEN -> Material.LIME_WOOL;
            case DARK_GREEN -> Material.GREEN_WOOL;
            case BLUE -> Material.BLUE_WOOL;
            case AQUA, DARK_AQUA -> Material.CYAN_WOOL;
            case YELLOW -> Material.YELLOW_WOOL;
            case GOLD -> Material.ORANGE_WOOL;
            case LIGHT_PURPLE -> Material.PINK_WOOL;
            case DARK_PURPLE -> Material.PURPLE_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }
}
