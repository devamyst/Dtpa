package net.devamy.dtpa.commands.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.strings.CC;
import org.bukkit.entity.Player;

@CommandAlias("tpauto")
@CommandPermission("dtpa.command.tpauto")
public class TpAutoCommand extends BaseCommand {
    @Default
    public void command(Player p) {
        boolean enabled = DTPA.getInstance().getTpaManager().toggleAutoAccept(p);
        CC.send(p, enabled ? "&aDTPAAuto is now &lenabled&r&a. All DTPA requests will be automatically accepted."
                            : "&cDTPAAuto is now &ldisabled&r&c. You will be prompted before accepting requests.");
    }
}
