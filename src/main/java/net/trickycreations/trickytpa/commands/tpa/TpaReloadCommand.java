package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpareload")
@CommandPermission("trickytpa.command.tpareload")
public class TpaReloadCommand extends BaseCommand {
    @Default
    public void command(Player player) {
        TrickyTPA.getInstance().reloadConfig();
        CC.send(player, "&aTrickyTPA config reloaded.");
    }
}
