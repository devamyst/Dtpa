package net.devamy.dtpa.storage;

import net.devamy.dtpa.DTPA;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class DatabaseManager {

    private final DTPA plugin;
    private Storage storage;

    public DatabaseManager(DTPA plugin) {
        this.plugin = plugin;
    }

    public Storage init() {
        saveDefaultDatabasesYml();
        FileConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "databases.yml"));

        String type = config.getString("type", "sqlite").toLowerCase();

        try {
            switch (type) {
                case "mysql" -> {
                    String host = config.getString("mysql.host", "localhost");
                    int port = config.getInt("mysql.port", 3306);
                    String db = config.getString("mysql.database", "dtpa");
                    String user = config.getString("mysql.username", "root");
                    String pass = config.getString("mysql.password", "");
                    boolean ssl = config.getBoolean("mysql.useSSL", false);
                    int pool = config.getInt("mysql.pool_size", 10);
                    int timeout = config.getInt("mysql.connection_timeout", 5000);
                    String prefix = config.getString("mysql.table_prefix", "dtpa_");
                    storage = new MySQLStorage(plugin, host, port, db, user, pass, ssl, pool, timeout, prefix);
                }
                case "h2" -> {
                    String filename = config.getString("h2.filename", "dtpa");
                    String options = config.getString("h2.options", "");
                    String jdbcUrl = "jdbc:h2:./" + plugin.getDataFolder() + "/" + filename + options;
                    storage = new EmbeddedStorage(plugin, jdbcUrl);
                }
                default -> { // sqlite
                    String filename = config.getString("sqlite.filename", "dtpa.db");
                    String jdbcUrl = "jdbc:sqlite:" + plugin.getDataFolder() + "/" + filename;
                    storage = new EmbeddedStorage(plugin, jdbcUrl);
                }
            }
            storage.init();
            plugin.getLogger().info("Database connected: " + type);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database (" + type + "): " + e.getMessage());
            e.printStackTrace();
            // Fallback to SQLite
            try {
                String jdbcUrl = "jdbc:sqlite:" + plugin.getDataFolder() + "/dtpa.db";
                storage = new EmbeddedStorage(plugin, jdbcUrl);
                storage.init();
                plugin.getLogger().warning("Fell back to SQLite storage.");
            } catch (Exception ex) {
                plugin.getLogger().severe("Fallback SQLite also failed: " + ex.getMessage());
            }
        }
        return storage;
    }

    public void close() {
        if (storage != null) {
            storage.close();
            storage = null;
        }
    }

    public Storage getStorage() {
        return storage;
    }

    private void saveDefaultDatabasesYml() {
        File file = new File(plugin.getDataFolder(), "databases.yml");
        if (!file.exists()) {
            plugin.saveResource("databases.yml", false);
        }
    }
}
