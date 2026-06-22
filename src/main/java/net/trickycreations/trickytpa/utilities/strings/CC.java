package net.trickycreations.trickytpa.utilities.strings;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CC {

    private static final Pattern UNICODE_PATTERN = Pattern.compile("\\\\u[a-fA-F0-9]{4}");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?<!&)#([a-fA-F0-9]{6})");

    public static String hexColor(String str) {
        Matcher unicodeMatcher = UNICODE_PATTERN.matcher(str);
        while (unicodeMatcher.find()) {
            String code = unicodeMatcher.group();
            str = str.replace(code, Character.toString((char) Integer.parseInt(code.replace("\\u", ""), 16)));
        }

        Matcher hexColorMatcher = HEX_COLOR_PATTERN.matcher(str);
        StringBuffer buffer = new StringBuffer();
        while (hexColorMatcher.find()) {
            hexColorMatcher.appendReplacement(buffer, "&#" + hexColorMatcher.group(1));
        }
        hexColorMatcher.appendTail(buffer);
        return buffer.toString();
    }

    public static Component hexColorComponent(String str) {
        return Component.text(hexColor(str));
    }

    public static String translate(String message) {
        return hexColor(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static List<String> translate(List<String> messages) {
        List<String> translated = new ArrayList<>(messages.size());
        for (String message : messages) {
            translated.add(translate(message));
        }
        return translated;
    }

    public static List<Component> component(List<String> messages) {
        List<Component> components = new ArrayList<>(messages.size());
        for (String message : messages) {
            components.add(component(message));
        }
        return components;
    }

    public static Component component(String message) {
        return Component.text(translate(message));
    }

    public static String translate(String message, Object... params) {
        return translate(replace(message, params));
    }

    public static Component component(String message, Object... params) {
        return component(replace(message, params));
    }

    public static String translate(Player player, String message) {
        return PlaceholderAPI.setPlaceholders(player, translate(message));
    }

    public static Component component(Player player, String message) {
        return component(translate(player, message));
    }

    public static String translate(Player player, String message, Object... params) {
        return translate(player, replace(message, params));
    }

    public static Component component(Player player, String message, Object... params) {
        return component(player, replace(message, params));
    }

    public static String replace(String message, Object... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("Parameters should be in key-value pairs.");
        }

        for (int i = 0; i < params.length; i += 2) {
            message = message.replace(params[i].toString(), params[i + 1].toString());
        }
        return message;
    }

    public static Component replaceComponent(String message, Object... params) {
        return Component.text(replace(translate(message), params));
    }

    public static void send(Player player, String message, Object... params) {
        player.sendMessage(component(player, replace(message, params)));
    }

    public static void sendTitle(Player player, String title, String subTitle, int seconds) {
        player.showTitle(Title.title(
                component(player, title),
                component(player, subTitle),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(seconds), Duration.ofSeconds(1))
        ));
    }

    public static void sendActionBar(Player player, String text, Object... params) {
        player.sendActionBar(component(player, replace(text, params)));
    }
}