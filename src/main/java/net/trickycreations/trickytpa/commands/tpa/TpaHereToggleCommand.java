package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpaheretoggle")
@CommandPermission("trickytpa.command.tpaheretoggle")
public class TpaHereToggleCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean disabled = TrickyTPA.getInstance().getTpaManager().toggleTpaHere(p);
        CC.send(p, disabled ? "&cTPAHere is now &ldisabled&r&c. Players cannot send you TPAHere requests."
                            : "&aTPAHere is now &lenabled&r&a. Players can send you TPAHere requests.");
    }
}
