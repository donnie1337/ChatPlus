package com.exemplo.chatplus;

import com.exemplo.chatplus.command.ChatCommand;
import com.exemplo.chatplus.command.GlobalChatCommand;
import com.exemplo.chatplus.command.LocalChatCommand;
import com.exemplo.chatplus.command.StaffChatCommand;
import com.exemplo.chatplus.config.ConfigManager;
import com.exemplo.chatplus.listener.ChatListener;
import com.exemplo.chatplus.service.ChatDelayService;
import com.exemplo.chatplus.service.ChatService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point of ChatPlus.
 */
public final class ChatPlus extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.load();

        ChatDelayService chatDelayService = new ChatDelayService(configManager);
        ChatService chatService = new ChatService(configManager, chatDelayService);

        registerCommand("l", new LocalChatCommand(configManager, chatService));
        registerCommand("g", new GlobalChatCommand(configManager, chatService));
        registerCommand("s", new StaffChatCommand(configManager, chatService));

        ChatCommand chatCommand = new ChatCommand(configManager);
        PluginCommand chatPluginCommand = getCommand("chat");
        if (chatPluginCommand != null) {
            chatPluginCommand.setExecutor(chatCommand);
            chatPluginCommand.setTabCompleter(chatCommand);
        } else {
            getLogger().warning("Comando /chat não encontrado no plugin.yml.");
        }

        getServer().getPluginManager().registerEvents(
                new ChatListener(this, configManager, chatService, chatDelayService), this);

        getLogger().info("ChatPlus habilitado com sucesso.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatPlus desabilitado.");
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("Comando /" + name + " não encontrado no plugin.yml.");
        }
    }
}
