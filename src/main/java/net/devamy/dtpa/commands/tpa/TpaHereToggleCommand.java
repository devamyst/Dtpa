package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpaheretoggle")
@CommandPermission("dtpa.command.tpaheretoggle")
public class TpaHereToggleCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean disabled = DTPA.getInstance().getTpaManager().toggleTpaHere(p);
        CC.send(p, disabled ? "&cDTPAHere is now &ldisabled&r&c. Players cannot send you DTPAHere requests."
                            : "&aDTPAHere is now &lenabled&r&a. Players can send you DTPAHere requests.");
    }
}
