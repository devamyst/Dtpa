package net.devamy.dtpa.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.tpa.model.TpaRequest;
import net.devamy.dtpa.tpa.model.TpaType;
import net.devamy.dtpa.utilities.skull.SkullCreator;
import net.devamy.dtpa.utilities.strings.CC;
import net.devamy.dtpa.utilities.universal.XMaterial;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Confirmation GUI shown to the SENDER before the TPA request is sent.
 * sender  = the player who typed /tpa
 * receiver = the target they want to teleport to/from
 */
public class RequestGui {
    private final Player sender;
    private final Player receiver;
    private final TpaType type;

    public RequestGui(Player sender, Player receiver, TpaType type) {
        this.sender   = sender;
        this.receiver = receiver;
        this.type     = type;
    }

    public void open() {
        FileConfiguration config = DTPA.getInstance().getConfig();

        Gui gui = Gui.gui()
                .title(CC.component(
                        config.getString("guis.sent_confirm.title", "Confirm Request"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()))
                .rows(config.getInt("guis.sent_confirm.rows", 3))
                .disableAllInteractions()
                .create();

        // ── Info item (receiver's head) ───────────────────────────────────
        GuiItem infoItem = createItem(
                SkullCreator.itemFromUuid(receiver.getUniqueId()),
                CC.replace(config.getString("guis.sent_confirm.info.display_name", "&e{receiver}"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()),
                config.getStringList("guis.sent_confirm.info.lore"),
                true,
                config.getBoolean("guis.sent_confirm.info.glow", false)
        );

        // ── Confirm button ────────────────────────────────────────────────
        GuiItem confirmItem = createItem(
                config.getString("guis.sent_confirm.accept.type", "LIME_STAINED_GLASS_PANE").toUpperCase(),
                CC.replace(config.getString("guis.sent_confirm.accept.display_name", "&aConfirm"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()),
                config.getStringList("guis.sent_confirm.accept.lore"),
                true,
                config.getBoolean("guis.sent_confirm.accept.glow", false)
        );

        confirmItem.setAction(event -> {
            sender.closeInventory();
            DTPA plugin = DTPA.getInstance();
            boolean receiverDisabledTpa     = type == TpaType.TPA     && plugin.getTpaManager().isTpaDisabled(receiver.getUniqueId());
            boolean receiverDisabledTpaHere = type == TpaType.TPA_HERE && plugin.getTpaManager().isTpaHereDisabled(receiver.getUniqueId());
            if (receiverDisabledTpa || receiverDisabledTpaHere) {
                CC.send(sender, "&c" + receiver.getName() + " has DTPA disabled.");
                return;
            }
            TpaRequest req = new TpaRequest(sender, receiver, type);
            if (!plugin.getTpaManager().sendRequest(req)) return;
            if (plugin.getTpaManager().isAutoAccept(receiver.getUniqueId())) {
                plugin.getTpaManager().acceptRequest(receiver, sender);
                return;
            }
            if (!plugin.getTpaManager().isConfirmDisabled(receiver.getUniqueId())) {
                new AcceptGui(sender, receiver).open();
            }
        });

        // ── Cancel button ─────────────────────────────────────────────────
        GuiItem cancelItem = createItem(
                config.getString("guis.sent_confirm.refuse.type", "RED_STAINED_GLASS_PANE").toUpperCase(),
                CC.replace(config.getString("guis.sent_confirm.refuse.display_name", "&cCancel"),
                        "{sender}", sender.getName(), "{receiver}", receiver.getName()),
                config.getStringList("guis.sent_confirm.refuse.lore"),
                true,
                config.getBoolean("guis.sent_confirm.refuse.glow", false)
        );

        cancelItem.setAction(event -> {
            sender.closeInventory();
            CC.send(sender, "&cDTPA request cancelled.");
        });

        gui.setItem(config.getInt("guis.sent_confirm.info.slot",   13), infoItem);
        gui.setItem(config.getInt("guis.sent_confirm.accept.slot", 16), confirmItem);
        gui.setItem(config.getInt("guis.sent_confirm.refuse.slot", 10), cancelItem);

        gui.open(sender);
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
