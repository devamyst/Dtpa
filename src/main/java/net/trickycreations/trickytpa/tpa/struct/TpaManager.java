package net.trickycreations.trickytpa.tpa.struct;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.enums.Messages;
import net.trickycreations.trickytpa.enums.Settings;
import net.trickycreations.trickytpa.storage.Storage;
import net.trickycreations.trickytpa.tpa.model.TpaRequest;
import net.trickycreations.trickytpa.tpa.model.TpaType;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RequiredArgsConstructor
public class TpaManager {

    private final TrickyTPA instance;
    private final Storage storage;

    // Runtime-only: tracks scheduled expiration tasks per request key.
    // When a request is accepted/denied/expired/cancelled, the task is cancelled.
    private final ConcurrentMap<String, ScheduledTask> expirationTasks = new ConcurrentHashMap<>();

    // Runtime-only: tracks active teleporting players (player UUID -> partner UUID or key).
    // Used by TpaListener to cancel on damage.
    // This is NOT persisted — it's purely runtime state.
    @Getter
    private final ConcurrentMap<UUID, UUID> teleportingPlayers = new ConcurrentHashMap<>();

    // ─── Request lifecycle ────────────────────────────────────────────────────

    public boolean sendRequest(TpaRequest request) {
        Player sender = Objects.requireNonNull(request.getSender(), "Sender must be online");
        Player receiver = Objects.requireNonNull(request.getReceiver(), "Receiver must be online");
        UUID sUuid = request.getSenderUuid();
        UUID rUuid = request.getReceiverUuid();

        // Check world blacklist
        if (isWorldBlocked(sender)) {
            Messages.WORLD_BLACKLIST_SENDER.send(sender);
            return false;
        }
        if (isWorldBlocked(receiver)) {
            Messages.WORLD_BLACKLIST_RECEIVER.send(sender);
            return false;
        }

        // Check max requests
        if (isMaxRequestsReached(sender)) {
            Messages.MAX_REQUESTS_REACHED.send(sender, "{max}", String.valueOf(getMaxRequestsPerPlayer()));
            return false;
        }

        // Check blocklist
        if (storage.isPlayerBlocked(rUuid, sUuid)) {
            Messages.BLOCKED_ATTEMPT.send(sender, "{player}", request.getReceiverName());
            return false;
        }

        // Check distance (only for TPA, not TPAHere)
        if (request.getType() == TpaType.TPA && isTooFar(sender, receiver)) {
            int dist = (int) sender.getLocation().distance(receiver.getLocation());
            Messages.DISTANCE_TOO_FAR.send(sender,
                    "{distance}", String.valueOf(dist),
                    "{max}", String.valueOf(getMaxDistance()));
            return false;
        }

        // Check cooldown
        long cd = getCooldownTime(sender, receiver);
        if (cd > 0) {
            Messages.COOLDOWN_ACTIVE.send(sender, "{time}", String.valueOf(cd));
            return false;
        }

        String k = request.getKey();

        // Cancel any previous expiration task for this key
        ScheduledTask oldTask = expirationTasks.remove(k);
        if (oldTask != null) oldTask.cancel();

        // Save to storage
        int expireSecs = Settings.EXPIRED_TIME.get(Integer.class);
        request.setExpiresAt(System.currentTimeMillis() + expireSecs * 1000L);
        storage.saveRequest(request);

        // Notify
        Messages.REQUEST_SENT.send(sender,
                "{receiver}", request.getReceiverName(),
                "{type}", getTypeLabel(request.getType()));
        Messages.REQUEST_RECEIVED.send(receiver,
                "{sender}", request.getSenderName(),
                "{type}", getTypeLabel(request.getType()));
        sendClickableAcceptDeny(receiver, request);

        playSound(sender, "request_sent");
        playSound(receiver, "request_received");

        // Schedule expiration
        long expireTicks = expireSecs * 20L;
        ScheduledTask expireTask = Bukkit.getGlobalRegionScheduler()
                .runDelayed(instance, task -> expireRequest(k, sUuid, rUuid), expireTicks);
        expirationTasks.put(k, expireTask);

        return true;
    }

    private void expireRequest(String key, UUID senderUuid, UUID receiverUuid) {
        expirationTasks.remove(key);
        TpaRequest req = storage.getRequest(senderUuid, receiverUuid);
        if (req == null) return;
        storage.removeRequest(senderUuid, receiverUuid);
        Player sender = req.getSender();
        Player receiver = req.getReceiver();
        String sName = req.getSenderName();
        String rName = req.getReceiverName();
        if (sender != null)
            CC.send(sender, "&cYour TPA request to &e" + rName + " &chas expired.");
        if (receiver != null)
            CC.send(receiver, "&cThe TPA request from &e" + sName + " &chas expired.");
    }

    public void acceptRequest(Player receiver, Player sender) {
        TpaRequest req = removeRequest(sender.getUniqueId(), receiver.getUniqueId());
        if (req == null) {
            Messages.NO_REQUESTS.send(receiver);
            return;
        }
        Messages.RECEIVER_ACCEPT.send(receiver, "{type}", getTypeLabel(req.getType()));
        Messages.SENDER_NOTIFY_ACCEPT.send(sender, "{type}", getTypeLabel(req.getType()));
        playSound(receiver, "request_accept");
        playSound(sender, "request_accept");
        applyCooldown(sender, receiver);
        req.startTeleport();
    }

    public void refuseRequest(Player receiver, Player sender) {
        TpaRequest req = removeRequest(sender.getUniqueId(), receiver.getUniqueId());
        if (req == null) {
            Messages.NO_REQUESTS.send(receiver);
            return;
        }
        Messages.RECEIVER_REFUSE.send(receiver, "{type}", getTypeLabel(req.getType()));
        Messages.SENDER_NOTIFY_REFUSE.send(sender, "{type}", getTypeLabel(req.getType()));
        playSound(receiver, "request_deny");
        playSound(sender, "request_deny");
        applyCooldown(sender, receiver);
    }

    private TpaRequest removeRequest(UUID senderUuid, UUID receiverUuid) {
        String k = senderUuid + ":" + receiverUuid;
        ScheduledTask task = expirationTasks.remove(k);
        if (task != null) task.cancel();
        TpaRequest req = storage.getRequest(senderUuid, receiverUuid);
        if (req != null) storage.removeRequest(senderUuid, receiverUuid);
        return req;
    }

    public TpaRequest getRequest(Player sender, Player receiver) {
        return storage.getRequest(sender.getUniqueId(), receiver.getUniqueId());
    }

    public boolean hasPendingRequest(Player sender, Player receiver) {
        return getRequest(sender, receiver) != null;
    }

    public void cancelRequest(Player sender) {
        cancelRequest(sender, null);
    }

    public void cancelRequest(Player sender, String targetName) {
        UUID senderUuid = sender.getUniqueId();
        List<TpaRequest> outgoing = storage.getRequestsBySender(senderUuid);
        List<TpaRequest> toCancel = new ArrayList<>();

        for (TpaRequest req : outgoing) {
            if (targetName == null || req.getReceiverName().equalsIgnoreCase(targetName)) {
                toCancel.add(req);
            }
        }

        if (toCancel.isEmpty()) {
            if (targetName != null) {
                CC.send(sender, "&cYou have no pending TPA request to &e" + targetName + "&c.");
            } else {
                CC.send(sender, "&cYou have no pending outgoing TPA requests.");
            }
            return;
        }

        for (TpaRequest req : toCancel) {
            String k = req.getKey();
            ScheduledTask task = expirationTasks.remove(k);
            if (task != null) task.cancel();
            storage.removeRequest(req.getSenderUuid(), req.getReceiverUuid());

            CC.send(sender, "&cYour TPA request to &e" + req.getReceiverName() + " &chas been cancelled.");
            Player receiver = req.getReceiver();
            if (receiver != null) {
                CC.send(receiver, "&cThe TPA request from &e" + sender.getName() + " &chas been cancelled.");
            }
        }
    }

    public void clearAllRequests() {
        for (ScheduledTask task : expirationTasks.values()) {
            task.cancel();
        }
        expirationTasks.clear();
        storage.clearAllRequests();
    }

    // ─── Clickable chat ──────────────────────────────────────────────────────

    private void sendClickableAcceptDeny(Player receiver, TpaRequest req) {
        Component accept = Component.text(" [")
                .append(Component.text("✔ ACCEPT")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/tpaccept " + req.getSenderName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to accept"))))
                .append(Component.text("] ["))
                .append(Component.text("✘ DENY")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/tpadeny " + req.getSenderName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to deny"))))
                .append(Component.text("]"));
        receiver.sendMessage(accept);
    }

    // ─── Toggles (persisted to DB) ────────────────────────────────────────────

    public boolean toggleTpa(Player p) {
        UUID u = p.getUniqueId();
        boolean current = storage.isTpaDisabled(u);
        storage.setTpaDisabled(u, !current);
        return !current;
    }

    public boolean toggleTpaHere(Player p) {
        UUID u = p.getUniqueId();
        boolean current = storage.isTpaHereDisabled(u);
        storage.setTpaHereDisabled(u, !current);
        return !current;
    }

    public boolean toggleAutoAccept(Player p) {
        UUID u = p.getUniqueId();
        boolean current = storage.isAutoAccept(u);
        storage.setAutoAccept(u, !current);
        return !current;
    }

    public boolean toggleConfirm(Player p) {
        UUID u = p.getUniqueId();
        boolean current = storage.isConfirmDisabled(u);
        storage.setConfirmDisabled(u, !current);
        return !current;
    }

    public boolean isTpaDisabled(UUID u)        { return storage.isTpaDisabled(u); }
    public boolean isTpaHereDisabled(UUID u)    { return storage.isTpaHereDisabled(u); }
    public boolean isAutoAccept(UUID u)          { return storage.isAutoAccept(u); }
    public boolean isConfirmDisabled(UUID u)    { return storage.isConfirmDisabled(u); }

    // ─── Cooldown (persisted to DB) ───────────────────────────────────────────

    private boolean hasCooldown(Player player, Player target) {
        return getCooldownTime(player, target) > 0;
    }

    private long getCooldownTime(Player player, Player target) {
        if (!Settings.COOLDOWN_ENABLED.get(Boolean.class)) return 0;
        if (player.hasPermission("trickytpa.bypass.cooldown")) return 0;
        String k = buildCooldownKey(player, target);
        Long end = storage.getCooldown(k);
        if (end == null) return 0;
        long remaining = (end - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    private void applyCooldown(Player player, Player target) {
        if (!Settings.COOLDOWN_ENABLED.get(Boolean.class)) return;
        if (player.hasPermission("trickytpa.bypass.cooldown")) return;
        int seconds = Settings.COOLDOWN_SECONDS.get(Integer.class);
        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        storage.setCooldown(buildCooldownKey(player, target), endTime);
    }

    private String buildCooldownKey(Player player, Player target) {
        boolean perTarget = Settings.COOLDOWN_PER_TARGET.get(Boolean.class);
        if (perTarget) return "cd:" + player.getUniqueId() + ":" + target.getUniqueId();
        return "cd:" + player.getUniqueId();
    }

    // ─── World Blacklist ──────────────────────────────────────────────────────

    public boolean isWorldBlocked(Player player) {
        if (!Settings.WORLD_BLACKLIST_ENABLED.get(Boolean.class)) return false;
        if (player.hasPermission("trickytpa.bypass.world")) return false;
        List<String> worlds = instance.getConfig().getStringList("settings.world_blacklist.worlds");
        return worlds.contains(player.getWorld().getName());
    }

    // ─── Max Requests ─────────────────────────────────────────────────────────

    public boolean isMaxRequestsReached(Player player) {
        if (!Settings.MAX_REQUESTS_ENABLED.get(Boolean.class)) return false;
        if (player.hasPermission("trickytpa.bypass.maxrequests")) return false;
        int count = storage.getRequestCountBySender(player.getUniqueId());
        return count >= getMaxRequestsPerPlayer();
    }

    private int getMaxRequestsPerPlayer() {
        return instance.getConfig().getInt("settings.max_requests.per_player", 5);
    }

    // ─── Distance ─────────────────────────────────────────────────────────────

    public boolean isTooFar(Player sender, Player receiver) {
        if (!Settings.DISTANCE_LIMIT_ENABLED.get(Boolean.class)) return false;
        if (sender.hasPermission("trickytpa.bypass.distance")) return false;
        if (!sender.getWorld().equals(receiver.getWorld())) return true;
        return sender.getLocation().distance(receiver.getLocation()) > getMaxDistance();
    }

    private int getMaxDistance() {
        return instance.getConfig().getInt("settings.distance_limit.max_blocks", 1000);
    }

    // ─── Blocklist ────────────────────────────────────────────────────────────

    public boolean blockPlayer(Player blocker, Player target) {
        storage.blockPlayer(blocker.getUniqueId(), target.getUniqueId());
        return true;
    }

    public boolean unblockPlayer(Player blocker, Player target) {
        storage.unblockPlayer(blocker.getUniqueId(), target.getUniqueId());
        return true;
    }

    public boolean isPlayerBlocked(Player blocker, Player target) {
        return storage.isPlayerBlocked(blocker.getUniqueId(), target.getUniqueId());
    }

    public Set<UUID> getBlockedPlayers(Player player) {
        return storage.getBlockedPlayers(player.getUniqueId());
    }

    // ─── List requests ────────────────────────────────────────────────────────

    public List<TpaRequest> getPendingOutgoing(Player player) {
        return storage.getRequestsBySender(player.getUniqueId());
    }

    public List<TpaRequest> getPendingIncoming(Player player) {
        return storage.getRequestsByReceiver(player.getUniqueId());
    }

    public List<TpaRequest> getAllRequests() {
        return new ArrayList<>(storage.getAllRequests().values());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public String getTypeLabel(TpaType t) {
        return t == TpaType.TPA_HERE
                ? instance.getConfig().getString("settings.format.tpa_here", "TPA Here")
                : instance.getConfig().getString("settings.format.tpa", "TPA");
    }

    public void playSound(Player player, String path) {
        String soundName = instance.getConfig().getString("sounds." + path);
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) { }
        }
    }
}
