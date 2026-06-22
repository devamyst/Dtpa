package net.trickycreations.trickytpa;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.trickycreations.trickytpa.commands.tpa.*;
import net.trickycreations.trickytpa.config.ConfigMigrator;
import net.trickycreations.trickytpa.listener.TpaListener;
import net.trickycreations.trickytpa.storage.DatabaseManager;
import net.trickycreations.trickytpa.storage.Storage;
import net.trickycreations.trickytpa.tasks.TpaActionBarTask;
import net.trickycreations.trickytpa.tpa.struct.TpaManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Getter
public final class TrickyTPA extends JavaPlugin {
    @Getter private static TrickyTPA instance;
    private TpaManager tpaManager;
    private TpaActionBarTask actionBarTask;
    private DatabaseManager databaseManager;
    private Storage storage;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Migrate config.yml — adds missing keys from jar defaults
        new ConfigMigrator(this).migrate();

        // Database
        databaseManager = new DatabaseManager(this);
        storage = databaseManager.init();

        tpaManager = new TpaManager(this, storage);
        registerCommands();
        registerListeners();
        actionBarTask = new TpaActionBarTask(this);
        actionBarTask.start();

        getLogger().info("TrickyTPA enabled (Folia + DB).");
    }

    @Override
    public void onDisable() {
        if (actionBarTask != null) actionBarTask.stop();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("TrickyTPA disabled.");
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
