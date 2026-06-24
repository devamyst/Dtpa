package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import org.bukkit.entity.Player;

@CommandAlias("tpcancel")
@CommandPermission("dtpa.command.tpcancel")
public class TpCancelCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        DTPA.getInstance().getTpaManager().cancelRequest(player, targetName);
    }
}
