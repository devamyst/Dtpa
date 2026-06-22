package net.trickycreations.trickytpa.listener;

import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.enums.Settings;
import net.trickycreations.trickytpa.utilities.teleport.TeleportUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class TpaListener implements Listener {

    private final TrickyTPA plugin;

    public TpaListener(TrickyTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!Settings.DAMAGE_CANCEL_ENABLED.get(Boolean.class)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        TeleportUtils.cancelOnDamage(player);
    }
}
