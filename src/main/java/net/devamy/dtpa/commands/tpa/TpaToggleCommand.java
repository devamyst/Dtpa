package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpatoggle")
@CommandPermission("dtpa.command.tpatoggle")
public class TpaToggleCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean disabled = DTPA.getInstance().getTpaManager().toggleTpa(p);
        CC.send(p, disabled ? "&cDTPA is now &ldisabled&r&c. Players cannot send you DTPA requests."
                            : "&aDTPA is now &lenabled&r&a. Players can send you DTPA requests.");
    }
}
