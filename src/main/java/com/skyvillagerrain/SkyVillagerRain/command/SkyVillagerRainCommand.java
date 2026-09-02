package com.skyvillagerrain.SkyVillagerRain.command;

import com.skyvillagerrain.SkyVillagerRain.SkyVillagerRainPlugin;
import com.skyvillagerrain.SkyVillagerRain.event.VillagerRainManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkyVillagerRainCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "skyvillagerrain.admin";
    private final SkyVillagerRainPlugin plugin;
    private final VillagerRainManager manager;

    public SkyVillagerRainCommand(SkyVillagerRainPlugin plugin, VillagerRainManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (!SetCommands.isValid(subcommand)) {
            sender.sendMessage(plugin.message("invalid-argument"));
            return true;
        }

        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(plugin.message("no-permission"));
            return true;
        }

        switch (subcommand) {
            case "start" -> {
                if (!manager.startEvent(true)) {
                    sender.sendMessage(plugin.message("event-already-active"));
                } else {
                    sender.sendMessage(plugin.message("event-forced"));
                }
            }
            case "stop" -> {
                if (!manager.stopEvent(true)) {
                    sender.sendMessage(plugin.message("event-not-active"));
                } else {
                    sender.sendMessage(plugin.message("event-stopped"));
                }
            }
            case "reload" -> {
                plugin.reloadPluginConfig();
                sender.sendMessage(plugin.message("reload-success"));
            }
            case "status" -> sendStatus(sender);
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "--- SkyVillagerRain Status ---");
        sender.sendMessage(ChatColor.GRAY + "Event active: " + ChatColor.WHITE + manager.isEventActive());
        if (manager.isEventActive()) {
            sender.sendMessage(ChatColor.GRAY + "Time remaining: " + ChatColor.WHITE + manager.getRemainingSeconds() + "s");
        }
        sender.sendMessage(ChatColor.GRAY + "Tracked villagers: " + ChatColor.WHITE + manager.getTrackedVillagerCount());
        sender.sendMessage(ChatColor.GRAY + "Event interval: " + ChatColor.WHITE + manager.getIntervalSeconds() + "s");
        sender.sendMessage(ChatColor.GRAY + "Event duration: " + ChatColor.WHITE + manager.getDurationSeconds() + "s");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }
        String input = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : SetCommands.VALUES) {
            if (value.startsWith(input)) result.add(value);
        }
        return result;
    }

    private static final class SetCommands {
        private static final List<String> VALUES = List.of("start", "stop", "reload", "status");
        private static boolean isValid(String value) { return VALUES.contains(value); }
    }
}
