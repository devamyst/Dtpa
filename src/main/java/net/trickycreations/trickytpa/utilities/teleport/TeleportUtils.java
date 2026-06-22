package net.trickycreations.trickytpa.utilities.teleport;

import com.google.common.collect.Maps;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.experimental.UtilityClass;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.enums.Messages;
import net.trickycreations.trickytpa.enums.Settings;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public final class TeleportUtils {
    private static final Map<UUID, ScheduledTask> teleportTasks = Maps.newConcurrentMap();
    private static final Map<UUID, Player> teleportPartners = Maps.newConcurrentMap();

    public void startCountdownTeleport(Player player, Location destination, Player partner) {
        int seconds = Settings.TPA_COUNTDOWN.get(Integer.class);
        cancelExistingTeleport(player);
        if (seconds <= 0) {
            handleTeleportSuccess(player, destination);
            return;
        }

        Location initialLocation = player.getLocation();
        double initialX = initialLocation.getX();
        double initialY = initialLocation.getY();
        double initialZ = initialLocation.getZ();

        teleportPartners.put(player.getUniqueId(), partner);

        AtomicInteger cooldown = new AtomicInteger(seconds);

        TrickyTPA plugin = TrickyTPA.getInstance();
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin,
                scheduledTask -> {
                    if (!player.isOnline() || hasPlayerMoved(player, initialX, initialY, initialZ)) {
                        cancelTeleport(player);
                        if (player.isOnline()) {
                            handleTeleportFailure(player);
                        }
                        return;
                    }

                    int remaining = cooldown.getAndDecrement();
                    if (remaining <= 1) {
                        handleTeleportSuccess(player, destination);
                        cancelTeleport(player);
                        return;
                    }

                    handleTeleportProgress(player, remaining - 1);
                },
                null, 1L, 20L);

        teleportTasks.put(player.getUniqueId(), task);
    }

    public boolean cancelOnDamage(Player player) {
        ScheduledTask task = teleportTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            teleportPartners.remove(player.getUniqueId());
            if (player.isOnline() && Settings.DAMAGE_CANCEL_ENABLED.get(Boolean.class)) {
                boolean notify = TrickyTPA.getInstance().getConfig()
                        .getBoolean("settings.damage_cancel.notify", true);
                if (notify) {
                    CC.send(player, Messages.TELEPORT_CANCELLED_DAMAGE.get());
                    if (Settings.TELEPORT_ACTION_BAR.get(Boolean.class)) {
                        Messages.TELEPORT_ACTION_BAR_CANCEL.sendActionBar(player);
                    }
                }
                playSound(player, "teleport_cancelled");
            }
            return true;
        }
        return false;
    }

    private void cancelExistingTeleport(Player player) {
        ScheduledTask existing = teleportTasks.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
            teleportPartners.remove(player.getUniqueId());
        }
    }

    private boolean hasPlayerMoved(Player player, double initialX, double initialY, double initialZ) {
        Location loc = player.getLocation();
        return loc.getX() != initialX || loc.getY() != initialY || loc.getZ() != initialZ;
    }

    private void handleTeleportFailure(Player player) {
        Messages.TELEPORT_CANCELLED.send(player);
        if (Settings.TELEPORT_ACTION_BAR.get(Boolean.class))
            Messages.TELEPORT_ACTION_BAR_CANCEL.sendActionBar(player);
        if (Settings.TELEPORT_TITLE.get(Boolean.class))
            CC.sendTitle(player, Messages.TELEPORT_TITLE_CANCELLED_TITLE.get(),
                    Messages.TELEPORT_TITLE_CANCELLED_SUB_TITLE.get(), 2);
        playSound(player, "teleport_cancelled");
    }

    private void handleTeleportSuccess(Player player, Location destination) {
        Location safe = findSafeLocation(destination);
        if (safe == null) {
            CC.send(player, "&cNo safe location found to teleport to.");
            playSound(player, "teleport_cancelled");
            teleportPartners.remove(player.getUniqueId());
            return;
        }
        player.teleportAsync(safe);
        Messages.TELEPORT_SUCCESS.send(player);
        if (Settings.TELEPORT_ACTION_BAR.get(Boolean.class))
            Messages.TELEPORT_ACTION_BAR_SUCCESS.sendActionBar(player);
        if (Settings.TELEPORT_TITLE.get(Boolean.class))
            CC.sendTitle(player, Messages.TELEPORT_TITLE_SUCCESS_TITLE.get(),
                    Messages.TELEPORT_TITLE_SUCCESS_SUB_TITLE.get(), 2);
        playSound(player, "teleport_success");
        teleportPartners.remove(player.getUniqueId());
    }

    private void handleTeleportProgress(Player player, int remaining) {
        Messages.TELEPORT_PROGRESS.send(player, "{time}", String.valueOf(remaining));
        if (Settings.TELEPORT_ACTION_BAR.get(Boolean.class))
            Messages.TELEPORT_ACTION_BAR_PROGRESS.sendActionBar(player, "{time}", String.valueOf(remaining));
        if (Settings.TELEPORT_TITLE.get(Boolean.class))
            CC.sendTitle(player,
                    Messages.TELEPORT_TITLE_PROGRESS_TITLE.get().replace("{time}", String.valueOf(remaining)),
                    Messages.TELEPORT_TITLE_PROGRESS_SUB_TITLE.get().replace("{time}", String.valueOf(remaining)),
                    2);
    }

    public void cancelTeleport(Player player) {
        ScheduledTask task = teleportTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            teleportPartners.remove(player.getUniqueId());
        }
    }

    public Location findSafeLocation(Location location) {
        Location loc = location.clone();

        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().subtract(0, 1, 0).getBlock();

        if (!feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid()) {
            return loc.add(0.5, 0, 0.5);
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -3; y <= 3; y++) {
                    Location test = loc.clone().add(x, y, z);
                    Block tFeet = test.getBlock();
                    Block tHead = test.clone().add(0, 1, 0).getBlock();
                    Block tGround = test.clone().subtract(0, 1, 0).getBlock();
                    if (!tFeet.getType().isSolid() && !tHead.getType().isSolid() && tGround.getType().isSolid()) {
                        return test.add(0.5, 0, 0.5);
                    }
                }
            }
        }
        return null;
    }

    private void playSound(Player player, String path) {
        String soundName = TrickyTPA.getInstance().getConfig().getString("sounds." + path);
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) { }
        }
    }
}
