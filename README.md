# SkyVillagerRain

A lightweight Paper plugin for Minecraft 1.21.8 that periodically creates a villager-rain event: actual villagers spawn above eligible players and fall naturally using Minecraft physics.

## Requirements
- Minecraft/Paper 1.21.8
- Java 21
- No external plugins are required.

## Installation
1. Build the project with `./gradlew build` (Windows: `gradlew.bat build`).
2. Copy `build/libs/SkyVillagerRain-1.0.0.jar` to the server's `plugins` directory.
3. Restart the Paper server.
4. Edit `plugins/SkyVillagerRain/config.yml` if desired.
5. Use `/villagerrain reload` after configuration changes.

## Default event timing
Automatic starts are 120 seconds apart. The 25-second event duration does **not** add to the interval.

```text
0:00 — plugin starts its 120-second wait
2:00 — event starts
2:00–2:25 — villagers spawn every 1 second
4:00 — next event starts
4:00–4:25 — villagers spawn every 1 second
6:00 — next event starts
```

## Commands
- `/villagerrain` — Show help/status.
- `/villagerrain start` — Start an event immediately.
- `/villagerrain stop` — Stop the active event.
- `/villagerrain reload` — Reload and validate configuration.
- `/villagerrain status` — Show event status and configuration summary.
- `/vrain` — Alias for `/villagerrain`.

Admin subcommands require `skyvillagerrain.admin` (default: OP).

## Permissions
- `skyvillagerrain.admin` — Allows administration, including start/stop/reload/status. Default: OP.
- `skyvillagerrain.use` — Allows a player to be eligible for Villager Rain. Default: true.

When `players.require-use-permission` is true, players without `skyvillagerrain.use` are skipped.

## Configuration
`config.yml` controls:
- Event interval and duration.
- Spawn interval and villagers per player.
- Height and horizontal radius.
- Maximum safe-location attempts.
- Joiner, spectator, dead-player, and permission rules.
- Villager profession, invulnerability, silence, baby state, and cleanup.
- All player-facing messages.

Invalid numeric values are replaced with safe defaults and logged as warnings rather than disabling the plugin.

## Gameplay behavior
- Villagers are real `Villager` entities.
- They spawn above the player's current position and are not teleported onto the player.
- Horizontal offsets are randomized within the configured radius.
- Spawning happens on the main server thread.
- Multiple worlds are supported; a player's current world is used on each spawn cycle.
- Disconnected, dead, spectator, or permission-blocked players are skipped according to configuration.
- Plugin-spawned villagers are marked with a PersistentDataContainer key so cleanup never targets normal villagers.
- Events cannot overlap, and scheduled tasks are cancelled on shutdown.
- A player joining during an active event is eligible on the next spawn cycle when `include-players-who-join-during-event` is enabled.

## Build

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

The resulting plugin JAR is written to `build/libs/`.

## Troubleshooting
- If the plugin does not load, verify that the server is Paper 1.21.8 and is running Java 21.
- If a command says you lack permission, use an OP account or grant `skyvillagerrain.admin`.
- If no villagers spawn, check `skyvillagerrain.use`, player state settings, and the server console for configuration warnings.
- If villagers disappear at event end, check `villager.remove-after-event`.
