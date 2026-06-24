package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import lombok.RequiredArgsConstructor;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.enums.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("tpadeny|tpacancel")
@CommandPermission("dtpa.command.tpadeny")
@RequiredArgsConstructor
public class TpaDenyCommand extends BaseCommand {
    private final DTPA instance;

    @Default
    @CommandCompletion("@players")
    public void command(Player player, @Optional String targetName) {
        if (targetName == null) { Messages.ENTER_PLAYER.send(player); return; }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) { Messages.PLAYER_NOT_FOUND.send(player); return; }
        instance.getTpaManager().refuseRequest(player, target);
    }
}
