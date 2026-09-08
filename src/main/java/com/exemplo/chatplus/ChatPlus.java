package com.exemplo.chatplus;

import com.exemplo.chatplus.command.ChatColorCommand;
import com.exemplo.chatplus.command.ChatColorGui;
import com.exemplo.chatplus.command.ChatCommand;
import com.exemplo.chatplus.command.GlobalChatCommand;
import com.exemplo.chatplus.command.LocalChatCommand;
import com.exemplo.chatplus.command.StaffChatCommand;
import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.listener.ChatListener;
import com.exemplo.chatplus.listener.UnknownCommandListener;
import com.exemplo.chatplus.service.ChatColorService;
import com.exemplo.chatplus.service.ChatDelayService;
import com.exemplo.chatplus.service.ChatService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Entry point of ChatPlus. */
public final class ChatPlus extends JavaPlugin {
    private ConfigManager configManager;
    private ChatColorService chatColorService;
    private ChatColorGui chatColorGui;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.load();

        ChatDelayService chatDelayService = new ChatDelayService(configManager);
        ChatService chatService = new ChatService(configManager, chatDelayService);
        this.chatColorService = new ChatColorService();
        this.chatColorGui = new ChatColorGui(this, chatColorService);

        registerCommand("l", new LocalChatCommand(configManager, chatService));
        registerCommand("g", new GlobalChatCommand(configManager, chatService));
        registerCommand("s", new StaffChatCommand(configManager, chatService));
        registerCommand("cor", new ChatColorCommand(chatColorService, chatColorGui));

        ChatCommand chatCommand = new ChatCommand(configManager);
        PluginCommand chatPluginCommand = getCommand("chat");
        if (chatPluginCommand != null) {
            chatPluginCommand.setExecutor(chatCommand);
            chatPluginCommand.setTabCompleter(chatCommand);
        } else {
            getLogger().warning("Comando /chat nao encontrado no plugin.yml.");
        }

        getServer().getPluginManager().registerEvents(chatColorGui, this);
        getServer().getPluginManager().registerEvents(
                new ChatListener(this, configManager, chatService, chatDelayService), this);
        getServer().getPluginManager().registerEvents(new UnknownCommandListener(configManager), this);

        getLogger().info("ChatPlus habilitado com sucesso.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatPlus desabilitado.");
    }

    public String getCurrentChatColor(Player player) {
        if (chatColorService == null || player == null) return ChatColor.WHITE.toString();
        return chatColorService.resolve(chatColorService.getCurrentColorName(player)).toString();
    }

    public String getCurrentChatColorName(Player player) {
        if (chatColorService == null || player == null) return "branco";
        return chatColorService.getCurrentColorName(player);
    }

    /** Public integration API for trusted system messages from other plugins. */
    public void sendSystemMessage(Player player, String message) {
        if (player == null || !player.isOnline() || message == null) return;
        final String prefix = configManager != null ? configManager.getMessage("prefixo-sistema") : "";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    public void openChatColorGui(Player player) {
        if (chatColorGui != null && player != null) chatColorGui.open(player);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if ("cor".equalsIgnoreCase(name)) {
                command.setPermission(null);
            }
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                command.setTabCompleter(completer);
            }
        } else {
            getLogger().warning("Comando /" + name + " nao encontrado no plugin.yml.");
        }
    }
}
