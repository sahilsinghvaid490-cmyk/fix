package com.skyvillagerrain.SkyVillagerRain.event;

import com.skyvillagerrain.SkyVillagerRain.SkyVillagerRainPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class VillagerRainManager {
    private static final int DEFAULT_INTERVAL_SECONDS = 120;
    private static final int DEFAULT_DURATION_SECONDS = 25;
    private static final int DEFAULT_SPAWN_INTERVAL_TICKS = 20;
    private static final int DEFAULT_VILLAGERS_PER_PLAYER = 1;
    private static final int DEFAULT_HEIGHT = 30;
    private static final int DEFAULT_RADIUS = 5;
    private static final int DEFAULT_ATTEMPTS = 5;

    private final SkyVillagerRainPlugin plugin;
    private final NamespacedKey markerKey;
    private BukkitTask automaticTask;
    private BukkitTask eventTask;
    private boolean eventActive;
    private long eventEndTick;
    private int intervalSeconds;
    private int durationSeconds;
    private int spawnIntervalTicks;
    private int villagersPerPlayer;
    private int heightAbovePlayer;
    private int horizontalRadius;
    private int maxLocationAttempts;
    private boolean includeJoiners;
    private boolean requireUsePermission;
    private boolean skipSpectators;
    private boolean skipDeadPlayers;
    private String profession;
    private boolean invulnerable;
    private boolean silent;
    private boolean baby;
    private boolean removeAfterEvent;
    private final Set<UUID> trackedVillagers = new HashSet<>();
    private final Set<UUID> eventParticipants = new HashSet<>();

    public VillagerRainManager(SkyVillagerRainPlugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "spawned");
        loadConfiguration();
    }

    public void reloadConfiguration() {
        loadConfiguration();
    }

    private void loadConfiguration() {
        intervalSeconds = positiveInt("event.interval-seconds", DEFAULT_INTERVAL_SECONDS);
        durationSeconds = positiveInt("event.duration-seconds", DEFAULT_DURATION_SECONDS);
        spawnIntervalTicks = positiveInt("event.spawn-interval-ticks", DEFAULT_SPAWN_INTERVAL_TICKS);
        villagersPerPlayer = positiveInt("event.villagers-per-player", DEFAULT_VILLAGERS_PER_PLAYER);
        heightAbovePlayer = minimumInt("spawn.height-above-player", DEFAULT_HEIGHT, 1);
        horizontalRadius = minimumInt("spawn.horizontal-radius", DEFAULT_RADIUS, 0);
        maxLocationAttempts = minimumInt("spawn.max-location-attempts", DEFAULT_ATTEMPTS, 1);
        includeJoiners = plugin.getConfig().getBoolean("players.include-players-who-join-during-event", true);
        requireUsePermission = plugin.getConfig().getBoolean("players.require-use-permission", true);
        skipSpectators = plugin.getConfig().getBoolean("players.skip-spectators", true);
        skipDeadPlayers = plugin.getConfig().getBoolean("players.skip-dead-players", true);
        profession = plugin.getConfig().getString("villager.profession", "NONE");
        invulnerable = plugin.getConfig().getBoolean("villager.invulnerable", false);
        silent = plugin.getConfig().getBoolean("villager.silent", false);
        baby = plugin.getConfig().getBoolean("villager.baby", false);
        removeAfterEvent = plugin.getConfig().getBoolean("villager.remove-after-event", false);
    }

    private int positiveInt(String path, int fallback) {
        int value = plugin.getConfig().getInt(path, fallback);
        if (value <= 0) {
            plugin.getLogger().warning(path + " must be greater than 0. Using default: " + fallback);
            return fallback;
        }
        return value;
    }

    private int minimumInt(String path, int fallback, int minimum) {
        int value = plugin.getConfig().getInt(path, fallback);
        if (value < minimum) {
            plugin.getLogger().warning(path + " must be at least " + minimum + ". Using default: " + fallback);
            return fallback;
        }
        return value;
    }

    public void startAutomaticSchedule() {
        cancelAutomaticTask();
        long intervalTicks = Math.max(1L, intervalSeconds * 20L);
        automaticTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!eventActive) startEvent(false);
        }, intervalTicks, intervalTicks);
    }

    public boolean startEvent(boolean manual) {
        if (eventActive) return false;
        eventActive = true;
        long eventStartTick = plugin.getServer().getCurrentTick();
        eventEndTick = eventStartTick + durationSeconds * 20L;
        trackedVillagers.clear();
        eventParticipants.clear();
        if (!includeJoiners) {
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (isEligible(player)) eventParticipants.add(player.getUniqueId());
            });
        }
        plugin.getServer().broadcastMessage(plugin.message("event-start"));

        long spawnInterval = Math.max(1L, spawnIntervalTicks);
        eventTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::runSpawnCycle, 0L, spawnInterval);
        return true;
    }

    private void runSpawnCycle() {
        if (!eventActive || plugin.getServer().getCurrentTick() >= eventEndTick) {
            finishEvent();
            return;
        }
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (!includeJoiners && !eventParticipants.contains(player.getUniqueId())) return;
            if (!isEligible(player)) return;
            for (int i = 0; i < villagersPerPlayer; i++) {
                Location spawnLocation = findSafeSpawnLocation(player.getLocation());
                if (spawnLocation != null) spawnVillager(spawnLocation);
            }
        });
    }

    private boolean isEligible(org.bukkit.entity.Player player) {
        if (!player.isOnline()) return false;
        if (skipDeadPlayers && player.isDead()) return false;
        if (skipSpectators && player.getGameMode() == GameMode.SPECTATOR) return false;
        if (requireUsePermission && !player.hasPermission("skyvillagerrain.use")) return false;
        return true;
    }

    private Location findSafeSpawnLocation(Location playerLocation) {
        World world = playerLocation.getWorld();
        if (world == null) return null;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int worldMin = world.getMinHeight();
        int worldMax = world.getMaxHeight();
        double playerY = playerLocation.getY();

        for (int attempt = 0; attempt < maxLocationAttempts; attempt++) {
            double angle = random.nextDouble(0, Math.PI * 2.0);
            double distance = horizontalRadius == 0 ? 0.0 : Math.sqrt(random.nextDouble()) * horizontalRadius;
            double x = playerLocation.getX() + Math.cos(angle) * distance;
            double z = playerLocation.getZ() + Math.sin(angle) * distance;
            double y = Math.min(worldMax - 2.0, Math.max(worldMin + 1.0, playerY + heightAbovePlayer));
            Location candidate = new Location(world, x, y, z);
            if (isSafeSpawn(candidate, playerLocation)) return candidate;
        }
        return null;
    }

    private boolean isSafeSpawn(Location location, Location playerLocation) {
        World world = location.getWorld();
        if (world == null) return false;
        if (location.getY() < world.getMinHeight() + 1 || location.getY() >= world.getMaxHeight() - 1) return false;
        if (!location.getBlock().isPassable()) return false;
        Location feet = location.clone().subtract(0, 1, 0);
        if (!feet.getBlock().isPassable()) return false;
        double dx = location.getX() - playerLocation.getX();
        double dz = location.getZ() - playerLocation.getZ();
        return (dx * dx + dz * dz) > 0.36;
    }

    private void spawnVillager(Location location) {
        Entity entity = location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        if (!(entity instanceof Villager villager)) {
            entity.remove();
            return;
        }
        villager.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        villager.setInvulnerable(invulnerable);
        villager.setSilent(silent);
        villager.setAdult();
        if (baby) villager.setBaby();
        applyProfession(villager);
        trackedVillagers.add(villager.getUniqueId());
    }

    private void applyProfession(Villager villager) {
        try {
            Villager.Profession selected = Villager.Profession.valueOf(profession.toUpperCase(java.util.Locale.ROOT));
            villager.setProfession(selected);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid villager profession '" + profession + "'. Falling back to NONE.");
            villager.setProfession(Villager.Profession.NONE);
        }
    }

    public boolean stopEvent(boolean broadcast) {
        if (!eventActive) return false;
        finishEvent(broadcast);
        return true;
    }

    private void finishEvent() {
        finishEvent(true);
    }

    private void finishEvent(boolean broadcast) {
        eventActive = false;
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }
        if (removeAfterEvent) cleanupTrackedVillagers();
        trackedVillagers.removeIf(uuid -> {
            Entity entity = findEntity(uuid);
            return entity == null || !entity.isValid();
        });
        if (broadcast) plugin.getServer().broadcastMessage(plugin.message("event-end"));
    }

    private void cleanupTrackedVillagers() {
        for (UUID uuid : new HashSet<>(trackedVillagers)) {
            Entity entity = findEntity(uuid);
            if (entity instanceof Villager && isPluginVillager(entity)) entity.remove();
        }
        trackedVillagers.clear();
    }

    private Entity findEntity(UUID uuid) {
        for (World world : plugin.getServer().getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    private boolean isPluginVillager(Entity entity) {
        return entity.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public void shutdown() {
        cancelAutomaticTask();
        if (eventTask != null) eventTask.cancel();
        eventTask = null;
        eventActive = false;
        if (removeAfterEvent) cleanupTrackedVillagers();
        trackedVillagers.clear();
        eventParticipants.clear();
    }

    private void cancelAutomaticTask() {
        if (automaticTask != null) {
            automaticTask.cancel();
            automaticTask = null;
        }
    }

    public boolean isEventActive() { return eventActive; }
    public long getRemainingSeconds() {
        if (!eventActive) return 0;
        long remainingTicks = Math.max(0L, eventEndTick - plugin.getServer().getCurrentTick());
        return (remainingTicks + 19L) / 20L;
    }
    public int getTrackedVillagerCount() {
        trackedVillagers.removeIf(uuid -> {
            Entity entity = findEntity(uuid);
            return entity == null || !entity.isValid();
        });
        return trackedVillagers.size();
    }
    public int getIntervalSeconds() { return intervalSeconds; }
    public int getDurationSeconds() { return durationSeconds; }
}
