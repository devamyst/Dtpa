package net.devamy.dtpa.tasks;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TpaActionBarTask {

    private final DTPA plugin;
    private ScheduledTask task;

    public TpaActionBarTask(DTPA plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int interval = plugin.getConfig().getInt("settings.actionbar.interval_ticks", 20);
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            String msg = plugin.getConfig().getString("messages.tpauto-actionbar", "&bTPAuto &7is active");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getTpaManager().isAutoAccept(p.getUniqueId())) {
                    CC.sendActionBar(p, msg);
                }
            }
        }, 20L, Math.max(interval, 1));
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
