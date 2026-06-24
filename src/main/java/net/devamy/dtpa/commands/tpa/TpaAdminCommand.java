package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("tpaadmin")
@CommandPermission("dtpa.command.tpaadmin")
public class TpaAdminCommand extends BaseCommand {

    @Default
    public void command(Player player) {
        CC.send(player, "&8=== &6DTPA Admin Commands &8===");
        CC.send(player, " &7/tpaadmin clear &8- &7Clear all pending DTPA requests");
        CC.send(player, " &7/tpaadmin reload &8- &7Reload configuration");
        CC.send(player, " &7/tpaadmin info <player> &8- &7Show TPA info for a player");
    }

    @Subcommand("clear")
    public void clear(Player player) {
        DTPA plugin = DTPA.getInstance();
        int count = plugin.getTpaManager().getAllRequests().size();
        plugin.getTpaManager().clearAllRequests();
        CC.send(player, "&aCleared &e" + count + " &apending DTPA requests.");
    }

    @Subcommand("reload")
    public void reload(Player player) {
        DTPA plugin = DTPA.getInstance();
        plugin.reloadConfig();
        plugin.getWikiGenerator().generate();
        CC.send(player, "&aConfiguration reloaded.");
    }

    @Subcommand("info")
    @CommandCompletion("@players")
    public void info(Player player, @Optional String targetName) {
        if (targetName == null) { CC.send(player, "&cUsage: /tpaadmin info <player>"); return; }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) { CC.send(player, "&cPlayer not found online."); return; }

        DTPA plugin = DTPA.getInstance();
        CC.send(player, "&8=== &6DTPA Info: &e" + target.getName() + " &8===");
        CC.send(player, "  &7DTPA Disabled: &e" + plugin.getTpaManager().isTpaDisabled(target.getUniqueId()));
        CC.send(player, "  &7DTPAHere Disabled: &e" + plugin.getTpaManager().isTpaHereDisabled(target.getUniqueId()));
        CC.send(player, "  &7Auto-Accept: &e" + plugin.getTpaManager().isAutoAccept(target.getUniqueId()));
        CC.send(player, "  &7Confirm GUI Disabled: &e" + plugin.getTpaManager().isConfirmDisabled(target.getUniqueId()));
        CC.send(player, "  &7Pending Outgoing: &e" + plugin.getTpaManager().getPendingOutgoing(target).size());
        CC.send(player, "  &7Pending Incoming: &e" + plugin.getTpaManager().getPendingIncoming(target).size());
    }

    @Subcommand("remove")
    @CommandCompletion("@players @players")
    public void remove(Player player, @Optional String senderName, @Optional String receiverName) {
        if (senderName == null || receiverName == null) {
            CC.send(player, "&cUsage: /tpaadmin remove <sender> <receiver>");
            return;
        }
        Player sender = Bukkit.getPlayer(senderName);
        Player receiver = Bukkit.getPlayer(receiverName);
        if (sender == null || receiver == null) {
            CC.send(player, "&cOne or both players not found online.");
            return;
        }
        if (DTPA.getInstance().getTpaManager().getRequest(sender, receiver) == null) {
            CC.send(player, "&cNo DTPA request found between &e" + sender.getName()
                    + " &cand &e" + receiver.getName() + "&c.");
            return;
        }
        DTPA.getInstance().getTpaManager().silentRemoveRequest(sender, receiver);
        CC.send(player, "&aRemoved DTPA request from &e" + sender.getName()
                + " &ato &e" + receiver.getName() + "&a.");
    }
}
