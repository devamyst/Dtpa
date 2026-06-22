# TrickyTPA

Paper 1.21+/Folia plugin — teleport requests with GUI confirm/accept, countdown, movement cancel, and much more.

## Build

```bash
JAVA_HOME=/usr/lib/jvm/zulu-21-amd64 ./gradlew build
```

Output: `build/libs/TrickyTPA.jar` (shadowJar relocates ACF + Triumph GUI into `net.trickycreations.trickytpa.libs.*`)

Java 21, Gradle 8.8. **Java 26+ will not work with Gradle 8.8** — set `JAVA_HOME` to a Java 21 JDK.

## Key structure

| Path | Role |
|---|---|
| `TrickyTPA.java` | Main class — saves default config, creates `TpaManager`, registers ACF commands + listener, starts Folia-compatible `TpaActionBarTask` via `GlobalRegionScheduler` |
| `commands/tpa/` | ACF `BaseCommand` subclasses |
| `tpa/struct/TpaManager.java` | All mutable state in-memory — `ConcurrentMap<String,TpaRequest>` keyed `senderUUID:receiverUUID`, plus 4 `ConcurrentHashSet<UUID>` for toggles, cooldown map, blocklist map, expiration `ScheduledTask` tracking |
| `tpa/model/TpaRequest.java` | Simple POCO — sender, receiver, type (TPA / TPA_HERE) |
| `enums/Messages.java` | Config-path enum — each value is a `config.yml` `messages.*` path; `send(player, params…)` reads config every call |
| `enums/Settings.java` | Same pattern for `settings.*` — type-safe generic `get(Class<T>)` |
| `utilities/teleport/TeleportUtils.java` | Per-player Folia `EntityScheduler` countdown with exact x/y/z movement comparison; safe landing finder; damage-cancel support |
| `utilities/strings/CC.java` | Adventure `Component` helpers; hex color (`#rrggbb`) and `&`-code translation; PAPI integration; title/action-bar wrappers |
| `gui/RequestGui.java` | 3-row confirm GUI shown to the **sender** before the request is dispatched |
| `gui/AcceptGui.java` | 3-row accept/deny GUI shown to the **receiver** |
| `listener/TpaListener.java` | `EntityDamageEvent` handler — cancels active teleport countdown when the player takes damage |
| `config.yml` | Single file driving all messages, GUI layouts, timings, sounds, cooldown, world blacklist, max requests, distance limit, blocklist, and damage-cancel |
| `databases.yml` | Database backend selection — `mysql`, `h2`, or `sqlite`; connection details for each |
| `storage/Storage.java` | Interface defining all persistence operations (requests, toggles, cooldowns, blocklists) |
| `storage/EmbeddedStorage.java` | H2/SQLite backend — file-based, no RAM cache, all queries hit the DB directly via a single synchronized `Connection` |
| `storage/MySQLStorage.java` | MySQL via HikariCP connection pool — active requests cached in a `ConcurrentHashMap` for fast access; toggles/cooldowns/blocklists read from DB each call |
| `storage/DatabaseManager.java` | Factory that reads `databases.yml`, creates the appropriate `Storage` implementation, and falls back to SQLite on failure |
| `config/ConfigMigrator.java` | On startup, compares the default `config.yml` / `databases.yml` (from jar) against disk and inserts any missing keys — old configs automatically gain new settings |

## Folia compatibility

- **No `BukkitRunnable`**, no `Bukkit.getScheduler()` anywhere.
- Player-specific tasks use `player.getScheduler().runAtFixedRate()` / `.runDelayed()`.
- Global tasks use `Bukkit.getGlobalRegionScheduler().runAtFixedRate()` / `.runDelayed()`.
- `plugin.yml` has `folia-supported: true`.

## New features (vs original)

| Feature | Config path | Description |
|---|---|---|
| **Cooldown** | `settings.cooldown.*` | Configurable per-target or global cooldown between requests; bypassable with `trickytpa.bypass.cooldown` |
| **World blacklist** | `settings.world_blacklist.*` | Block TPA in/from certain worlds; bypassable with `trickytpa.bypass.world` |
| **Max requests** | `settings.max_requests.*` | Limit outgoing pending requests per player; bypassable with `trickytpa.bypass.maxrequests` |
| **Distance limit** | `settings.distance_limit.*` | Max allowed distance for TPA (not TPAHere); bypassable with `trickytpa.bypass.distance` |
| **Damage cancel** | `settings.damage_cancel.*` | Taking damage cancels the teleport countdown |
| **Blocklist** | `/tpablacklist <player>` | Block specific players from sending you requests |
| **TPA list** | `/tpalist [player]` | View your pending outgoing/incoming requests (admins can view others') |
| **TPA admin** | `/tpaadmin clear\|reload\|info\|remove` | Admin utilities for managing the TPA system |
| **Cancel by target** | `/tpcancel <player>` | Cancel a specific outgoing request by target name |

## Conventions

- **All Bukkit API calls on the owning region thread.** Player tasks spawned via `EntityScheduler` run on that player's region; global tasks via `GlobalRegionScheduler`.
- **Messages are fully config-driven** — every user-facing string lives in `config.yml` and is referenced through the `Messages` enum. Never hardcode display strings.
- **ACF command completions** use `@CommandCompletion("@players")` for player-name tab-completion.
- **GUIs** use Triumph GUI builder; items sourced from `XMaterial` (cross-version material matching); player heads from `SkullCreator`.
- **Lombok** throughout: `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@AllArgsConstructor`, `@UtilityClass`. The main plugin instance is `@Getter private static TrickyTPA instance`.

## Database

| Backend | Tables | RAM cache | Connection |
|---|---|---|---|
| **MySQL** | `trickytpa_tpa_requests`, `player_toggles`, `cooldowns`, `blocklist`, `audit_log` | Active `tpa_requests` cached in `ConcurrentHashMap` | HikariCP pool |
| **H2** | Same tables (SQL compat mode) | None | Single synchronized `Connection` |
| **SQLite** | Same tables | None | Single synchronized `Connection` |

All player state (toggles, cooldowns, blocklist) is read from DB on every call — designed for correctness over speed with the assumption these queries are fast (< 1ms). MySQL additionally caches active TPA requests in RAM for instant iteration.

`ConfigMigrator` (`config/ConfigMigrator.java`) runs on startup: it reads the default `config.yml` / `databases.yml` from the jar, compares against disk, and inserts any missing keys. Old configs automatically gain new settings without manual edits.

## Notable quirks

- `TpAcceptCommand` / `TpaDenyCommand` — **player = receiver (the one running the command), target = sender** (who sent the original request). This is reversed from the natural reading.
- `TpaManager` keys are `senderUUID + ":" + receiverUUID` — the `getRequest(sender, receiver)` and `cancelRequest(sender)` use this same composite key.
- Request expiration is handled inline via `GlobalRegionScheduler.runDelayed`. The old `TpaRequestRunnable` class is a stub.
- Movement cancellation uses exact `==` comparison on `getX()`, `getY()`, `getZ()` — any movement (including client-side server correction) will cancel. This is intentional anti-grief.
- `DamageCancel` is checked server-side on `EntityDamageEvent` (MONITOR priority, cancelled events ignored). If the player is mid-teleport the countdown is aborted and custom messages/sounds play.
- `/tpaadmin clear` clears ALL pending requests across all players.
- Cooldown defaults are stored in the enum `Settings` values, but `getMaxRequestsPerPlayer()`, `getMaxDistance()` and `DamageCancel` notify are read directly from config for flexibility.
