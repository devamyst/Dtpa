package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Set;
import java.util.UUID;

@CommandAlias("tpablacklist|tpablock")
@CommandPermission("dtpa.command.tpablacklist")
public class TpaBlockCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        if (targetName == null) {
            showBlockList(player);
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            CC.send(player, "&cPlayer not found.");
            return;
        }
        if (player.equals(target)) {
            CC.send(player, "&cYou cannot block yourself.");
            return;
        }

        DTPA plugin = DTPA.getInstance();
        if (plugin.getTpaManager().isPlayerBlocked(player, target)) {
            plugin.getTpaManager().unblockPlayer(player, target);
            CC.send(player, "&aUnblocked &e" + target.getName() + "&a. They can now send you DTPA requests.");
        } else {
            plugin.getTpaManager().blockPlayer(player, target);
            CC.send(player, "&aBlocked &e" + target.getName() + "&a. They can no longer send you DTPA requests.");

            // Cancel any pending requests between both players
            plugin.getTpaManager().cancelRequest(target, player.getName());
            plugin.getTpaManager().cancelRequest(player, target.getName());
        }
    }

    private void showBlockList(Player player) {
        Set<UUID> blocked = DTPA.getInstance().getTpaManager().getBlockedPlayers(player);
        if (blocked.isEmpty()) {
            CC.send(player, "&8=== &6Your Blocked Players &8===");
            CC.send(player, " &7You haven't blocked anyone.");
            return;
        }
        CC.send(player, "&8=== &6Your Blocked Players &8===");
        int i = 1;
        for (UUID uuid : blocked) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) {
                CC.send(player, " &e#" + (i++) + " &7" + name);
            }
        }
    }
}
