package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.enums.Messages;
import net.devamy.dtpa.gui.RequestGui;
import net.devamy.dtpa.tpa.model.TpaType;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("tpahere")
@CommandPermission("dtpa.command.tpahere")
public class TpaHereCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        if (targetName == null) { Messages.USAGE_TPAHERE.send(player); return; }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) { Messages.PLAYER_NOT_FOUND.send(player); return; }
        if (player.equals(target)) { Messages.CANT_TPA_YOURSELF.send(player); return; }

        DTPA plugin = DTPA.getInstance();
        if (plugin.getTpaManager().isTpaHereDisabled(target.getUniqueId())) {
            CC.send(player, "&c" + target.getName() + " has DTPAHere disabled.");
            return;
        }
        if (plugin.getTpaManager().hasPendingRequest(player, target)) {
            CC.send(player, "&cYou already have a pending DTPAHere request to &e" + target.getName() + "&c.");
            return;
        }

        if (plugin.getTpaManager().isConfirmDisabled(player.getUniqueId())) {
            TpaCommand.handleDirectSend(plugin, player, target, TpaType.TPA_HERE);
        } else {
            new RequestGui(player, target, TpaType.TPA_HERE).open();
        }
    }
}
