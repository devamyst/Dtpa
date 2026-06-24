package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import lombok.RequiredArgsConstructor;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.enums.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("tpaccept")
@CommandPermission("dtpa.command.tpaccept")
@RequiredArgsConstructor
public class TpAcceptCommand extends BaseCommand {
    private final DTPA instance;

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        if (targetName == null) { Messages.ENTER_PLAYER.send(player); return; }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) { Messages.PLAYER_NOT_FOUND.send(player); return; }
        instance.getTpaManager().acceptRequest(player, target);
    }
}
