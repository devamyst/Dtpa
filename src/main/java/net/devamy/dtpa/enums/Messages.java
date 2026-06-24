package net.devamy.dtpa.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@AllArgsConstructor
@Getter
public enum Messages {
    PLAYER_NOT_FOUND("player_not_found"),
    ENTER_PLAYER("enter_player"),
    USAGE_TPAHERE("usage_tpahere"),
    USAGE_TPA("usage_tpa"),
    CANT_TPA_YOURSELF("cant_tpa_yourself"),
    NO_REQUESTS("no_requests"),
    REQUEST_SENT("request_sent"),
    REQUEST_RECEIVED("request_received"),
    RECEIVER_ACCEPT("receiver_accept"),
    SENDER_NOTIFY_ACCEPT("sender_notify_accept"),
    RECEIVER_REFUSE("receiver_refuse"),
    SENDER_NOTIFY_REFUSE("sender_notify_refuse"),
    TELEPORT_CANCELLED("teleport.messages.cancelled"),
    TELEPORT_CANCELLED_DAMAGE("teleport.messages.cancelled_damage"),
    TELEPORT_SUCCESS("teleport.messages.success"),
    TELEPORT_PROGRESS("teleport.messages.progress"),
    TELEPORT_ACTION_BAR_PROGRESS("teleport.action_bar.progress"),
    TELEPORT_ACTION_BAR_CANCEL("teleport.action_bar.cancel"),
    TELEPORT_ACTION_BAR_SUCCESS("teleport.action_bar.success"),
    TELEPORT_TITLE_PROGRESS_TITLE("teleport.title.progress_title"),
    TELEPORT_TITLE_PROGRESS_SUB_TITLE("teleport.title.progress_sub_title"),
    TELEPORT_TITLE_CANCELLED_TITLE("teleport.title.cancelled_title"),
    TELEPORT_TITLE_CANCELLED_SUB_TITLE("teleport.title.cancelled_sub_title"),
    TELEPORT_TITLE_SUCCESS_TITLE("teleport.title.success_title"),
    TELEPORT_TITLE_SUCCESS_SUB_TITLE("teleport.title.success_sub_title"),
    COOLDOWN_ACTIVE("cooldown.active"),
    WORLD_BLACKLIST_SENDER("world_blacklist.sender_blocked"),
    WORLD_BLACKLIST_RECEIVER("world_blacklist.receiver_blocked"),
    WORLD_BLACKLIST_BOTH("world_blacklist.both_blocked"),
    MAX_REQUESTS_REACHED("max_requests.reached"),
    DISTANCE_TOO_FAR("distance.too_far"),
    BLOCKED_ATTEMPT("blocklist.blocked_attempt");

    private final String path;

    public void send(Player player, Object... params) {
        CC.send(player, DTPA.getInstance().getConfig().getString("messages." + path), params);
    }

    public void sendActionBar(Player player, Object... params) {
        CC.sendActionBar(player, DTPA.getInstance().getConfig().getString("messages." + path), params);
    }

    public String get() {
        return DTPA.getInstance().getConfig().getString("messages." + path);
    }
}
