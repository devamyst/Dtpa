package net.devamy.dtpa;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.devamy.dtpa.commands.tpa.*;
import net.devamy.dtpa.config.ConfigMigrator;
import net.devamy.dtpa.config.WikiGenerator;
import net.devamy.dtpa.listener.TpaListener;
import net.devamy.dtpa.storage.DatabaseManager;
import net.devamy.dtpa.storage.Storage;
import net.devamy.dtpa.tasks.TpaActionBarTask;
import net.devamy.dtpa.tpa.struct.TpaManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Getter
public final class DTPA extends JavaPlugin {
    @Getter private static DTPA instance;
    private TpaManager tpaManager;
    private TpaActionBarTask actionBarTask;
    private DatabaseManager databaseManager;
    private Storage storage;
    private WikiGenerator wikiGenerator;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Migrate config.yml — adds missing keys from jar defaults
        new ConfigMigrator(this).migrate();

        // Load JDBC drivers (H2, MySQL, SQLite) from plugins/DTPA/libs/
        try {
            new LibraryLoader(getDataFolder(), getLogger()).load();
        } catch (Exception e) {
            getLogger().severe("Failed to load database libraries: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Database
        databaseManager = new DatabaseManager(this);
        storage = databaseManager.init();

        tpaManager = new TpaManager(this, storage);
        wikiGenerator = new WikiGenerator(this);
        wikiGenerator.generate();
        registerCommands();
        registerListeners();
        actionBarTask = new TpaActionBarTask(this);
        actionBarTask.start();

        getLogger().info("DTPA enabled (Folia + DB).");
    }

    @Override
    public void onDisable() {
        if (actionBarTask != null) actionBarTask.stop();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("DTPA disabled.");
    }

    private void registerCommands() {
        PaperCommandManager manager = new PaperCommandManager(this);
        List.of(
            new TpaCommand(),
            new TpaHereCommand(),
            new TpAcceptCommand(this),
            new TpaDenyCommand(this),
            new TpaToggleCommand(),
            new TpaHereToggleCommand(),
            new TpAutoCommand(),
            new TpaToggleConfirmCommand(),
            new TpCancelCommand(),
            new TpaReloadCommand(),
            new TpaListCommand(),
            new TpaBlockCommand(),
            new TpaAdminCommand()
        ).forEach(manager::registerCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TpaListener(this), this);
    }
}
