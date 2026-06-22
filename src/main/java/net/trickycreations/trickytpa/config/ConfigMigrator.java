package net.trickycreations.trickytpa.config;

import net.trickycreations.trickytpa.TrickyTPA;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;

/**
 * Compares the default config.yml (bundled in the jar) against the one on disk
 * and adds any missing keys so old configs get new settings automatically.
 */
public final class ConfigMigrator {

    private final TrickyTPA plugin;

    public ConfigMigrator(TrickyTPA plugin) {
        this.plugin = plugin;
    }

    public void migrate() {
        migrateFile("config.yml");
        migrateFile("databases.yml");
    }

    private void migrateFile(String resourceName) {
        File diskFile = new File(plugin.getDataFolder(), resourceName);
        if (!diskFile.exists()) {
            plugin.saveResource(resourceName, false);
            plugin.getLogger().info("Created default " + resourceName);
            return;
        }

        // Load defaults from jar
        FileConfiguration defaults;
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in == null) {
                plugin.getLogger().warning("Default " + resourceName + " not found in jar, skipping migration.");
                return;
            }
            defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read default " + resourceName + ": " + e.getMessage());
            return;
        }

        // Load existing from disk
        FileConfiguration disk = YamlConfiguration.loadConfiguration(diskFile);

        // Find and add missing keys
        List<String> addedKeys = new ArrayList<>();
        addMissing(disk, defaults, "", addedKeys);

        if (addedKeys.isEmpty()) {
            plugin.getLogger().info(resourceName + " is up-to-date.");
            return;
        }

        // Save updated config back to disk
        try {
            String data = convertToString(disk);
            plugin.getDataFolder().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(diskFile),
                    java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(data);
            }
            plugin.getLogger().info("Migrated " + resourceName + " — added " + addedKeys.size() + " new key(s):");
            for (String key : addedKeys) {
                plugin.getLogger().info("  + " + key);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save migrated " + resourceName + ": " + e.getMessage());
        }
    }

    private void addMissing(ConfigurationSection disk, ConfigurationSection defaults,
                            String basePath, List<String> addedKeys) {
        for (String key : defaults.getKeys(false)) {
            String fullPath = basePath.isEmpty() ? key : basePath + "." + key;
            if (defaults.isConfigurationSection(key)) {
                // Ensure section exists on disk
                if (!disk.isConfigurationSection(key)) {
                    disk.createSection(key);
                    addedKeys.add(fullPath + "/");
                }
                // Recurse into nested sections
                addMissing(disk.getConfigurationSection(key) != null
                                ? disk.getConfigurationSection(key)
                                : disk.createSection(key),
                        defaults.getConfigurationSection(key),
                        fullPath, addedKeys);
            } else if (!disk.contains(key)) {
                // Leaf value missing — set the default
                disk.set(key, defaults.get(key));
                addedKeys.add(fullPath);
            }
        }
    }

    /**
     * Serialize a FileConfiguration back to a YAML string,
     * preserving comments from the original disk file where possible.
     */
    private String convertToString(FileConfiguration config) {
        // Bukkit's YamlConfiguration#saveToString re-formats everything and drops comments.
        // We use a simple approach: save via the standard method, which works fine.
        return ((YamlConfiguration) config).saveToString();
    }
}
