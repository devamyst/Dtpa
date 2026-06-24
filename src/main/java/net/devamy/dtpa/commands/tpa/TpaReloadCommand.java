package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpareload")
@CommandPermission("dtpa.command.tpareload")
public class TpaReloadCommand extends BaseCommand {
    @Default
    public void command(Player player) {
        DTPA plugin = DTPA.getInstance();
        plugin.reloadConfig();
        plugin.getWikiGenerator().generate();
        CC.send(player, "&aDTPA config reloaded.");
    }
}
