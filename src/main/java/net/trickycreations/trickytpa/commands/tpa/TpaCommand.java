package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.enums.Messages;
import net.trickycreations.trickytpa.gui.RequestGui;
import net.trickycreations.trickytpa.tpa.model.TpaRequest;
import net.trickycreations.trickytpa.tpa.model.TpaType;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("tpa")
@CommandPermission("trickytpa.command.tpa")
public class TpaCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        if (targetName == null) { Messages.USAGE_TPA.send(player); return; }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) { Messages.PLAYER_NOT_FOUND.send(player); return; }
        if (player.equals(target)) { Messages.CANT_TPA_YOURSELF.send(player); return; }

        TrickyTPA plugin = TrickyTPA.getInstance();
        if (plugin.getTpaManager().isTpaDisabled(target.getUniqueId())) {
            CC.send(player, "&c" + target.getName() + " has TPA disabled.");
            return;
        }
        if (plugin.getTpaManager().hasPendingRequest(player, target)) {
            CC.send(player, "&cYou already have a pending TPA request to &e" + target.getName() + "&c.");
            return;
        }

        if (plugin.getTpaManager().isConfirmDisabled(player.getUniqueId())) {
            handleDirectSend(plugin, player, target, TpaType.TPA);
        } else {
            new RequestGui(player, target, TpaType.TPA).open();
        }
    }

    static void handleDirectSend(TrickyTPA plugin, Player sender, Player receiver, TpaType type) {
        TpaRequest req = new TpaRequest(sender, receiver, type);
        if (plugin.getTpaManager().isAutoAccept(receiver.getUniqueId())) {
            if (!plugin.getTpaManager().sendRequest(req)) return;
            plugin.getTpaManager().acceptRequest(receiver, sender);
        } else {
            if (!plugin.getTpaManager().sendRequest(req)) return;
            if (!plugin.getTpaManager().isConfirmDisabled(receiver.getUniqueId())) {
                new net.trickycreations.trickytpa.gui.AcceptGui(sender, receiver).open();
            }
        }
    }
}
