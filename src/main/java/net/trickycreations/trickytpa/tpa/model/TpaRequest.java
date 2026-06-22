package net.trickycreations.trickytpa.tpa.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.trickycreations.trickytpa.utilities.teleport.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class TpaRequest {
    private UUID senderUuid, receiverUuid;
    private String senderName, receiverName;
    private TpaType type;
    private long createdAt;
    private long expiresAt;

    public TpaRequest(Player sender, Player receiver, TpaType type) {
        this(sender.getUniqueId(), receiver.getUniqueId(),
                sender.getName(), receiver.getName(),
                type, System.currentTimeMillis(), 0L);
    }

    public TpaRequest(UUID senderUuid, UUID receiverUuid,
                      String senderName, String receiverName,
                      TpaType type) {
        this(senderUuid, receiverUuid, senderName, receiverName,
                type, System.currentTimeMillis(), 0L);
    }

    public Player getSender() {
        return Bukkit.getPlayer(senderUuid);
    }

    public Player getReceiver() {
        return Bukkit.getPlayer(receiverUuid);
    }

    public String getKey() {
        return senderUuid + ":" + receiverUuid;
    }

    public void startTeleport() {
        if (type == TpaType.TPA_HERE)
            TeleportUtils.startCountdownTeleport(getReceiver(), getSender().getLocation(), getSender());
        else
            TeleportUtils.startCountdownTeleport(getSender(), getReceiver().getLocation(), getReceiver());
    }
}
