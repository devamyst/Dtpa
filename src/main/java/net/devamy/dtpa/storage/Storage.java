package net.devamy.dtpa.storage;

import net.devamy.dtpa.tpa.model.TpaRequest;
import net.devamy.dtpa.tpa.model.TpaType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface Storage {

    void init() throws Exception;
    void close();

    // ─── Requests ─────────────────────────────────────────────────────────

    void saveRequest(TpaRequest request);
    void removeRequest(UUID senderUuid, UUID receiverUuid);

    /** Atomically removes and returns the request, or null if not found. */
    TpaRequest removeAndGetRequest(UUID senderUuid, UUID receiverUuid);

    /** Returns null if not found. */
    TpaRequest getRequest(UUID senderUuid, UUID receiverUuid);

    /** All active (non-expired) requests. */
    Map<String, TpaRequest> getAllRequests();

    List<TpaRequest> getRequestsBySender(UUID sender);
    List<TpaRequest> getRequestsByReceiver(UUID receiver);
    int getRequestCountBySender(UUID sender);
    void clearAllRequests();
    void cleanupExpired(long cutoffEpochMs);

    // ─── Player Toggles ───────────────────────────────────────────────────

    boolean isTpaDisabled(UUID player);
    void setTpaDisabled(UUID player, boolean disabled);

    boolean isTpaHereDisabled(UUID player);
    void setTpaHereDisabled(UUID player, boolean disabled);

    boolean isAutoAccept(UUID player);
    void setAutoAccept(UUID player, boolean enabled);

    boolean isConfirmDisabled(UUID player);
    void setConfirmDisabled(UUID player, boolean disabled);

    // ─── Cooldowns ────────────────────────────────────────────────────────

    Long getCooldown(String key);
    void setCooldown(String key, long endTime);
    void removeCooldown(String key);

    // ─── Blocklist ────────────────────────────────────────────────────────

    void blockPlayer(UUID blocker, UUID blocked);
    void unblockPlayer(UUID blocker, UUID blocked);
    boolean isPlayerBlocked(UUID blocker, UUID blocked);
    Set<UUID> getBlockedPlayers(UUID player);
}
