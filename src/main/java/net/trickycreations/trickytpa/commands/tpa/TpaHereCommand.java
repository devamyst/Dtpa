package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.enums.Messages;
import net.trickycreations.trickytpa.gui.RequestGui;
import net.trickycreations.trickytpa.tpa.model.TpaType;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("tpahere")
@CommandPermission("trickytpa.command.tpahere")
public class TpaHereCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        if (targetName == null) { Messages.USAGE_TPAHERE.send(player); return; }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) { Messages.PLAYER_NOT_FOUND.send(player); return; }
        if (player.equals(target)) { Messages.CANT_TPA_YOURSELF.send(player); return; }

        TrickyTPA plugin = TrickyTPA.getInstance();
        if (plugin.getTpaManager().isTpaHereDisabled(target.getUniqueId())) {
            CC.send(player, "&c" + target.getName() + " has TPAHere disabled.");
            return;
        }
        if (plugin.getTpaManager().hasPendingRequest(player, target)) {
            CC.send(player, "&cYou already have a pending TPAHere request to &e" + target.getName() + "&c.");
            return;
        }

        if (plugin.getTpaManager().isConfirmDisabled(player.getUniqueId())) {
            TpaCommand.handleDirectSend(plugin, player, target, TpaType.TPA_HERE);
        } else {
            new RequestGui(player, target, TpaType.TPA_HERE).open();
        }
    }
}
