package com.skyvillagerrain.SkyVillagerRain;

import com.skyvillagerrain.SkyVillagerRain.command.SkyVillagerRainCommand;
import com.skyvillagerrain.SkyVillagerRain.event.VillagerRainManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyVillagerRainPlugin extends JavaPlugin {
    private VillagerRainManager rainManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        rainManager = new VillagerRainManager(this);

        SkyVillagerRainCommand command = new SkyVillagerRainCommand(this, rainManager);
        PluginCommand pluginCommand = getCommand("villagerrain");
        if (pluginCommand == null) {
            getLogger().severe("Failed to register /villagerrain command.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        rainManager.startAutomaticSchedule();
        getLogger().info("SkyVillagerRain enabled.");
        getLogger().info("Automatic Villager Rain events scheduled every "
                + rainManager.getIntervalSeconds() + " seconds.");
    }

    @Override
    public void onDisable() {
        if (rainManager != null) {
            rainManager.shutdown();
        }
        getLogger().info("SkyVillagerRain disabled.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (rainManager != null) {
            rainManager.reloadConfiguration();
        }
    }

    public String message(String path) {
        String raw = getConfig().getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
