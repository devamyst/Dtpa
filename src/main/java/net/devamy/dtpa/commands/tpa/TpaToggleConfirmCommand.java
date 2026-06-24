package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpatoggleconfirm")
@CommandPermission("dtpa.command.tpatoggleconfirm")
public class TpaToggleConfirmCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean disabled = DTPA.getInstance().getTpaManager().toggleConfirm(p);
        CC.send(p, disabled
                ? "&cConfirm GUI is now &ldisabled&r&c. Requests will be sent and accepted via chat commands."
                : "&aConfirm GUI is now &lenabled&r&a. You will see a GUI when sending or receiving requests.");
    }
}
