package net.trickycreations.trickytpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpauto")
@CommandPermission("trickytpa.command.tpauto")
public class TpAutoCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean enabled = TrickyTPA.getInstance().getTpaManager().toggleAutoAccept(p);
        CC.send(p, enabled ? "&aTPAuto is now &lenabled&r&a. All TPA requests will be automatically accepted."
                           : "&cTPAuto is now &ldisabled&r&c. You will be prompted before accepting requests.");
    }
}
