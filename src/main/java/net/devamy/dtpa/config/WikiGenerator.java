package net.devamy.dtpa.config;

import net.devamy.dtpa.DTPA;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class WikiGenerator {

    private final DTPA plugin;
    private final Map<String, String> descriptions = new LinkedHashMap<>();

    public WikiGenerator(DTPA plugin) {
        this.plugin = plugin;
        loadDescriptions();
    }

    private void loadDescriptions() {
        descriptions.put("sounds.request_sent", "Sound played to the sender when a request is dispatched");
        descriptions.put("sounds.request_received", "Sound played to the receiver when a request arrives");
        descriptions.put("sounds.request_accept", "Sound played when a request is accepted");
        descriptions.put("sounds.request_deny", "Sound played when a request is denied");
        descriptions.put("sounds.teleport_success", "Sound played on successful teleport");
        descriptions.put("sounds.teleport_cancelled", "Sound played when teleport is cancelled");
        descriptions.put("settings.tpa_countdown", "Countdown seconds before teleport (0 = instant) [int]");
        descriptions.put("settings.expired_time", "Seconds until a pending request expires [int]");
        descriptions.put("settings.teleport.action_bar", "Show teleport countdown in action bar [bool]");
        descriptions.put("settings.teleport.title", "Show teleport countdown as title [bool]");
        descriptions.put("settings.format.tpa", "Display label for TPA requests in messages [string]");
        descriptions.put("settings.format.tpa_here", "Display label for TPAHere requests in messages [string]");
        descriptions.put("settings.cooldown.enabled", "Enable cooldown between requests [bool]");
        descriptions.put("settings.cooldown.seconds", "Cooldown duration in seconds [int]");
        descriptions.put("settings.cooldown.per_target", "Per-target cooldown (true) or global (false) [bool]");
        descriptions.put("settings.cooldown.bypass_permission", "Permission node that bypasses cooldown [string]");
        descriptions.put("settings.world_blacklist.enabled", "Block TPA in/from certain worlds [bool]");
        descriptions.put("settings.world_blacklist.worlds", "List of world names where TPA is blocked [list]");
        descriptions.put("settings.world_blacklist.bypass_permission", "Permission node that bypasses world blacklist [string]");
        descriptions.put("settings.max_requests.enabled", "Limit pending outgoing requests per player [bool]");
        descriptions.put("settings.max_requests.per_player", "Max pending outgoing requests allowed [int]");
        descriptions.put("settings.max_requests.bypass_permission", "Permission node that bypasses max requests [string]");
        descriptions.put("settings.distance_limit.enabled", "Max distance check for TPA (not TPAHere) [bool]");
        descriptions.put("settings.distance_limit.max_blocks", "Maximum distance in blocks [int]");
        descriptions.put("settings.distance_limit.bypass_permission", "Permission node that bypasses distance limit [string]");
        descriptions.put("settings.damage_cancel.enabled", "Cancel teleport countdown on damage [bool]");
        descriptions.put("settings.damage_cancel.notify", "Notify player when damage cancels teleport [bool]");
        descriptions.put("settings.actionbar.interval_ticks", "Ticks between DTPAAuto action bar updates [int]");
        descriptions.put("messages.*", "All config-driven user-facing messages — see config.yml for each path");
        descriptions.put("guis.sent_confirm.*", "GUI layout for the sender confirmation screen (title, rows, slots, items)");
        descriptions.put("guis.accept_request.*", "GUI layout for the receiver accept/deny screen (title, rows, slots, items)");
    }

    public void generate() {
        File wikiFile = new File(plugin.getDataFolder(), "wiki.txt");
        FileConfiguration config = plugin.getConfig();
        FileConfiguration dbConfig = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "databases.yml"));

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("  DTPA — Teleport Request Plugin (v").append(plugin.getDescription().getVersion()).append(")\n");
        sb.append("  Paper 1.21+ / Folia\n");
        sb.append("================================================================================\n\n");

        sb.append("COMMANDS\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(formatCmd("/tpa <player>", "Send a TPA request to a player"));
        sb.append(formatCmd("/tpahere <player>", "Send a TPAHere request (brings player to you)"));
        sb.append(formatCmd("/tpaccept <player>", "Accept an incoming TPA request"));
        sb.append(formatCmd("/tpadeny <player>", "Deny an incoming TPA request"));
        sb.append(formatCmd("/tpatoggle", "Toggle receiving TPA requests on/off"));
        sb.append(formatCmd("/tpaheretoggle", "Toggle receiving TPAHere requests on/off"));
        sb.append(formatCmd("/tpauto", "Toggle auto-accepting all TPA requests"));
        sb.append(formatCmd("/tpatoggleconfirm", "Toggle the confirm GUI on/off"));
        sb.append(formatCmd("/tpcancel [player]", "Cancel your outgoing request(s)"));
        sb.append(formatCmd("/tpalist [player]", "View pending requests (admins can view others)"));
        sb.append(formatCmd("/tpablacklist <player>", "Block/unblock a player from sending you requests"));
        sb.append(formatCmd("/tpareload", "Reload the plugin configuration"));
        sb.append(formatCmd("/tpaadmin clear", "Clear all pending requests"));
        sb.append(formatCmd("/tpaadmin reload", "Reload configuration from console"));
        sb.append(formatCmd("/tpaadmin info <player>", "Show TPA settings for a player"));
        sb.append(formatCmd("/tpaadmin remove <sender> <receiver>", "Remove a specific request"));
        sb.append("\n");

        sb.append("PERMISSIONS\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(formatPerm("dtpa.command.tpa", "Use /tpa", true));
        sb.append(formatPerm("dtpa.command.tpahere", "Use /tpahere", true));
        sb.append(formatPerm("dtpa.command.tpaccept", "Use /tpaccept", true));
        sb.append(formatPerm("dtpa.command.tpadeny", "Use /tpadeny", true));
        sb.append(formatPerm("dtpa.command.tpatoggle", "Use /tpatoggle", true));
        sb.append(formatPerm("dtpa.command.tpaheretoggle", "Use /tpaheretoggle", true));
        sb.append(formatPerm("dtpa.command.tpauto", "Use /tpauto", true));
        sb.append(formatPerm("dtpa.command.tpatoggleconfirm", "Use /tpatoggleconfirm", true));
        sb.append(formatPerm("dtpa.command.tpcancel", "Use /tpcancel", true));
        sb.append(formatPerm("dtpa.command.tpareload", "Use /tpareload", false));
        sb.append(formatPerm("dtpa.command.tpalist", "Use /tpalist", true));
        sb.append(formatPerm("dtpa.command.tpablacklist", "Use /tpablacklist", true));
        sb.append(formatPerm("dtpa.command.tpaadmin", "Use /tpaadmin", false));
        sb.append(formatPerm("dtpa.bypass.cooldown", "Bypass cooldown", false));
        sb.append(formatPerm("dtpa.bypass.world", "Bypass world blacklist", false));
        sb.append(formatPerm("dtpa.bypass.maxrequests", "Bypass max request limit", false));
        sb.append(formatPerm("dtpa.bypass.distance", "Bypass distance limit", false));
        sb.append("\n");

        sb.append("CONFIGURATION (config.yml)\n");
        sb.append("--------------------------------------------------------------------------------\n\n");
        appendConfigSection(sb, config, "", 0);

        sb.append("\nDATABASE (databases.yml)\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("  type:              Backend type — mysql, h2, or sqlite [string]\n");
        sb.append("  sqlite.filename:   SQLite database filename [string]\n");
        sb.append("  h2.filename:       H2 database filename [string]\n");
        sb.append("  h2.options:        H2 connection options (e.g. ;MODE=MySQL) [string]\n");
        sb.append("  mysql.host:        MySQL host address [string]\n");
        sb.append("  mysql.port:        MySQL port [int]\n");
        sb.append("  mysql.database:    MySQL database name [string]\n");
        sb.append("  mysql.username:    MySQL username [string]\n");
        sb.append("  mysql.password:    MySQL password [string]\n");
        sb.append("  mysql.useSSL:      Whether to use SSL for MySQL [bool]\n");
        sb.append("  mysql.pool_size:   HikariCP connection pool size [int]\n");
        sb.append("  mysql.connection_timeout: Connection timeout in ms [int]\n");
        sb.append("  mysql.table_prefix: MySQL table prefix [string]\n");
        sb.append("\n");

        sb.append("================================================================================\n");
        sb.append("  Wiki auto-generated by DTPA v").append(plugin.getDescription().getVersion());
        sb.append(" — regenerated on each startup and /tpareload\n");
        sb.append("================================================================================\n");

        try {
            plugin.getDataFolder().mkdirs();
            Files.writeString(wikiFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
            plugin.getLogger().info("Generated wiki.txt (" + sb.length() + " bytes)");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write wiki.txt: " + e.getMessage());
        }
    }

    private void appendConfigSection(StringBuilder sb, ConfigurationSection section, String path, int depth) {
        for (String key : section.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (section.isConfigurationSection(key)) {
                sb.append(indent(depth)).append("  ").append(key).append(":\n");
                appendConfigSection(sb, section.getConfigurationSection(key), fullPath, depth + 1);
            } else {
                String raw = section.getString(key);
                String desc = descriptions.getOrDefault(fullPath, "");
                String marker = desc.isEmpty() ? "" : "  // " + desc;
                if (raw != null && raw.length() > 60) {
                    raw = raw.substring(0, 57) + "...";
                }
                sb.append(indent(depth)).append("  ").append(key).append(": ");
                if (section.isList(key)) {
                    sb.append("<list>").append(marker).append("\n");
                } else if (section.isBoolean(key)) {
                    sb.append(raw).append(marker).append("\n");
                } else if (section.isInt(key)) {
                    sb.append(raw).append(marker).append("\n");
                } else {
                    sb.append("\"").append(raw).append("\"").append(marker).append("\n");
                }
            }
        }
    }

    private String indent(int depth) {
        return depth == 0 ? "" : "  ".repeat(depth);
    }

    private String formatCmd(String cmd, String desc) {
        return String.format("  %-30s %s\n", cmd, desc);
    }

    private String formatPerm(String perm, String desc, boolean defaultTrue) {
        return String.format("  %-35s %s (default: %s)\n", perm, desc, defaultTrue ? "all" : "op");
    }
}
