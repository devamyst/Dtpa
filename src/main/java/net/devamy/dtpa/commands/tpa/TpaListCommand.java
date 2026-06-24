package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.tpa.model.TpaRequest;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.List;

@CommandAlias("tpalist")
@CommandPermission("dtpa.command.tpalist")
public class TpaListCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        if (targetName != null) {
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                CC.send(player, "&cPlayer not found.");
                return;
            }
            if (!player.equals(target) && !player.hasPermission("dtpa.command.tpaadmin")) {
                CC.send(player, "&cYou can only view your own requests.");
                return;
            }
            listFor(player, target);
            return;
        }
        listFor(player, player);
    }

    private void listFor(Player viewer, Player target) {
        DTPA plugin = DTPA.getInstance();
        List<TpaRequest> outgoing = plugin.getTpaManager().getPendingOutgoing(target);
        List<TpaRequest> incoming = plugin.getTpaManager().getPendingIncoming(target);

        boolean isAdmin = !viewer.equals(target);

        if (outgoing.isEmpty() && incoming.isEmpty()) {
            CC.send(viewer, isAdmin
                    ? "&e" + target.getName() + " &chas no pending DTPA requests."
                    : "&cYou have no pending DTPA requests.");
            return;
        }

        if (isAdmin) {
            CC.send(viewer, "&8=== &6Pending DTPA Requests for &e" + target.getName() + " &8===");
        } else {
            CC.send(viewer, "&8=== &6Your Pending DTPA Requests &8===");
        }

        int index = 1;
        if (!outgoing.isEmpty()) {
            CC.send(viewer, " &7Outgoing:");
            for (TpaRequest req : outgoing) {
                String label = plugin.getTpaManager().getTypeLabel(req.getType());
                CC.send(viewer, "   &e#" + (index++) + " &7-> &e" + req.getReceiverName()
                        + " &8(&7" + label + "&8)");
            }
        }
        if (!incoming.isEmpty()) {
            CC.send(viewer, " &7Incoming:");
            for (TpaRequest req : incoming) {
                String label = plugin.getTpaManager().getTypeLabel(req.getType());
                CC.send(viewer, "   &e#" + (index++) + " &7<- &e" + req.getSenderName()
                        + " &8(&7" + label + "&8)");
            }
        }
    }
}
