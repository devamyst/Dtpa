package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import org.bukkit.entity.Player;

@CommandAlias("tpcancel")
@CommandPermission("trickytpa.command.tpcancel")
public class TpCancelCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        TrickyTPA.getInstance().getTpaManager().cancelRequest(player, targetName);
    }
}
