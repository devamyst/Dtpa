package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpatoggleconfirm")
@CommandPermission("trickytpa.command.tpatoggleconfirm")
public class TpaToggleConfirmCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean disabled = TrickyTPA.getInstance().getTpaManager().toggleConfirm(p);
        CC.send(p, disabled
                ? "&cConfirm GUI is now &ldisabled&r&c. Requests will be sent and accepted via chat commands."
                : "&aConfirm GUI is now &lenabled&r&a. You will see a GUI when sending or receiving requests.");
    }
}
