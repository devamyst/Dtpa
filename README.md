# DTPA

Paper 1.21+/Folia plugin — teleport requests with GUI confirm/accept, countdown, movement cancel, and much more.

## Features

- **TPA / TPAHere** — request and accept teleports with a GUI
- **Cooldowns** — configurable per-target or global cooldown
- **World blacklist** — block TPA in/from certain worlds
- **Max requests** — limit outgoing pending requests per player
- **Distance limit** — enforce a max distance for TPA (not TPAHere)
- **Damage cancel** — taking damage cancels the teleport countdown
- **Blocklist** — `/tpablacklist <player>` to block specific players
- **TPA list** — `/tpalist [player]` to view pending requests
- **TPA admin** — `/tpaadmin clear|reload|info|remove`
- **Database support** — MySQL, H2, or SQLite
- **Folia compatible** — no BukkitRunnable or Bukkit.getScheduler()

## Requirements

- Paper 1.21+ (or Folia)
- Java 21
- (Optional) PlaceholderAPI

## Build

```bash
JAVA_HOME=/usr/lib/jvm/zulu-21-amd64 ./gradlew build
```

Output: `build/libs/DTPA.jar`

## Configuration

All messages, GUI layouts, timings, sounds, cooldowns, world blacklist, max requests, distance limits, blocklists, and damage-cancel settings are driven by `config.yml`. Database settings are in `databases.yml`.

## License

All Rights Reserved. See [LICENSE](LICENSE).
