package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpatoggle")
@CommandPermission("trickytpa.command.tpatoggle")
public class TpaToggleCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean disabled = TrickyTPA.getInstance().getTpaManager().toggleTpa(p);
        CC.send(p, disabled ? "&cTPA is now &ldisabled&r&c. Players cannot send you TPA requests."
                            : "&aTPA is now &lenabled&r&a. Players can send you TPA requests.");
    }
}
