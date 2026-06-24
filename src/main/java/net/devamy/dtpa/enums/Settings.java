package net.devamy.dtpa.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.devamy.dtpa.DTPA;

@AllArgsConstructor
@Getter
public enum Settings {
    EXPIRED_TIME("settings.expired_time"),
    TPA_COUNTDOWN("settings.tpa_countdown"),
    TELEPORT_TITLE("settings.teleport.title"),
    TELEPORT_ACTION_BAR("settings.teleport.action_bar"),
    COOLDOWN_ENABLED("settings.cooldown.enabled"),
    COOLDOWN_SECONDS("settings.cooldown.seconds"),
    COOLDOWN_PER_TARGET("settings.cooldown.per_target"),
    WORLD_BLACKLIST_ENABLED("settings.world_blacklist.enabled"),
    MAX_REQUESTS_ENABLED("settings.max_requests.enabled"),
    DISTANCE_LIMIT_ENABLED("settings.distance_limit.enabled"),
    DAMAGE_CANCEL_ENABLED("settings.damage_cancel.enabled");

    private final String path;

    public <T> T get(Class<T> clazz) {
        if (clazz == Boolean.class) {
            return clazz.cast(DTPA.getInstance().getConfig().getBoolean(path));
        }
        if (clazz == Integer.class) {
            return clazz.cast(DTPA.getInstance().getConfig().getInt(path));
        }
        if (clazz == String.class) {
            return clazz.cast(DTPA.getInstance().getConfig().getString(path));
        }
        if (clazz == Double.class) {
            return clazz.cast(DTPA.getInstance().getConfig().getDouble(path));
        }
        return clazz.cast(DTPA.getInstance().getConfig().get(path));
    }
}
