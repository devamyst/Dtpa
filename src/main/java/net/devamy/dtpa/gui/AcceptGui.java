package net.devamy.dtpa.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.utilities.skull.SkullCreator;
import net.devamy.dtpa.utilities.strings.CC;
import net.devamy.dtpa.utilities.universal.XMaterial;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Confirmation GUI shown to the RECEIVER of a TPA request.
 * sender   = the player who sent /tpa (wants to teleport)
 * receiver = the player who receives this GUI (decides accept/deny)
 */
public class AcceptGui {
    private final Player sender;
    private final Player receiver;

    public AcceptGui(Player sender, Player receiver) {
        this.sender   = sender;
        this.receiver = receiver;
    }

    public void open() {
        FileConfiguration config = DTPA.getInstance().getConfig();

        Gui gui = Gui.gui()
                .title(CC.component(
                        config.getString("guis.accept_request.title", "Response Request"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()))
                .rows(config.getInt("guis.accept_request.rows", 3))
                .disableAllInteractions()
                .create();

        // ── Info item (sender's head — the person who sent the request) ───
        GuiItem infoItem = createItem(
                SkullCreator.itemFromUuid(sender.getUniqueId()),
                CC.replace(config.getString("guis.accept_request.info.display_name", "&e{sender}"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()),
                config.getStringList("guis.accept_request.info.lore"),
                true,
                config.getBoolean("guis.accept_request.info.glow", false)
        );

        // ── Accept button ─────────────────────────────────────────────────
        GuiItem acceptItem = createItem(
                config.getString("guis.accept_request.accept.type", "LIME_STAINED_GLASS_PANE").toUpperCase(),
                CC.replace(config.getString("guis.accept_request.accept.display_name", "&aAccept"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()),
                config.getStringList("guis.accept_request.accept.lore"),
                true,
                config.getBoolean("guis.accept_request.accept.glow", false)
        );

        acceptItem.setAction(event -> {
            receiver.closeInventory();
            // receiver is the one accepting; sender sent the request
            DTPA.getInstance().getTpaManager().acceptRequest(receiver, sender);
        });

        // ── Deny button ───────────────────────────────────────────────────
        GuiItem denyItem = createItem(
                config.getString("guis.accept_request.refuse.type", "RED_STAINED_GLASS_PANE").toUpperCase(),
                CC.replace(config.getString("guis.accept_request.refuse.display_name", "&cDeny"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()),
                config.getStringList("guis.accept_request.refuse.lore"),
                true,
                config.getBoolean("guis.accept_request.refuse.glow", false)
        );

        denyItem.setAction(event -> {
            receiver.closeInventory();
            DTPA.getInstance().getTpaManager().refuseRequest(receiver, sender);
        });

        gui.setItem(config.getInt("guis.accept_request.info.slot",   13), infoItem);
        gui.setItem(config.getInt("guis.accept_request.accept.slot", 16), acceptItem);
        gui.setItem(config.getInt("guis.accept_request.refuse.slot", 10), denyItem);

        gui.open(receiver);
    }

    private GuiItem createItem(String material, String displayName, List<String> lore,
                               boolean hideFlags, boolean glow) {
        return ItemBuilder.from(XMaterial.matchXMaterial(material).orElse(XMaterial.BARRIER).parseMaterial())
                .name(CC.component(displayName))
                .lore(CC.component(lore))
                .flags(hideFlags ? ItemFlag.values() : null)
                .glow(glow)
                .asGuiItem();
    }

    private GuiItem createItem(ItemStack itemStack, String displayName, List<String> lore,
                               boolean hideFlags, boolean glow) {
        return ItemBuilder.from(itemStack)
                .name(CC.component(displayName))
                .lore(CC.component(lore))
                .flags(hideFlags ? ItemFlag.values() : null)
                .glow(glow)
                .asGuiItem();
    }
}
