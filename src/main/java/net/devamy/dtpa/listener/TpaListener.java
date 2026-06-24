package net.devamy.dtpa.listener;

import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.enums.Settings;
import net.devamy.dtpa.utilities.teleport.TeleportUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TpaListener implements Listener {

    private final DTPA plugin;

    public TpaListener(DTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!Settings.DAMAGE_CANCEL_ENABLED.get(Boolean.class)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        TeleportUtils.cancelOnDamage(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        TeleportUtils.cancelTeleport(event.getPlayer());
    }
}
