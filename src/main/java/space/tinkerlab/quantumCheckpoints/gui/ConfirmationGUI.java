// src/main/java/space/tinkerlab/quantumCheckpoints/gui/ConfirmationGUI.java
package space.tinkerlab.quantumCheckpoints.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.*;

/**
 * Manages the confirmation GUI for checkpoint restoration.
 */
public class ConfirmationGUI {

    private static final int GUI_SIZE = 27; // 3 rows
    private static final int CONFIRM_SLOT = 11;
    private static final int INFO_SLOT = 13;
    private static final int CANCEL_SLOT = 15;

    private final QuantumCheckpoints plugin;

    /** Tracks pending restore confirmations by player UUID */
    private final Map<UUID, Checkpoint> pendingRestores;

    /**
     * Creates a new ConfirmationGUI manager.
     *
     * @param plugin the main plugin instance
     */
    public ConfirmationGUI(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.pendingRestores = new HashMap<>();
    }

    /**
     * Opens the restoration confirmation GUI for a player.
     *
     * @param player     the player to show the GUI to
     * @param checkpoint the checkpoint being restored
     */
    public void openRestoreConfirmation(Player player, Checkpoint checkpoint) {
        ConfirmationHolder holder = new ConfirmationHolder(checkpoint);
        Inventory gui = Bukkit.createInventory(
                holder,
                GUI_SIZE,
                Component.text("Restore Checkpoint?", NamedTextColor.DARK_PURPLE)
        );

        // Fill with glass panes
        ItemStack filler = createFillerItem();
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, filler);
        }

        // Create confirm button
        gui.setItem(CONFIRM_SLOT, createConfirmItem());

        // Create info display
        gui.setItem(INFO_SLOT, createInfoItem(checkpoint));

        // Create cancel button
        gui.setItem(CANCEL_SLOT, createCancelItem());

        pendingRestores.put(player.getUniqueId(), checkpoint);
        player.openInventory(gui);
    }

    /**
     * Creates the filler glass pane item.
     */
    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the confirm button item.
     */
    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✓ RESTORE", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to restore your", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("saved state.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (plugin.getConfigManager().isPenaltyEnabled()) {
            lore.add(Component.text("⚠ Warning:", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("A random item will be lost!", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the info display item showing checkpoint details.
     */
    private ItemStack createInfoItem(Checkpoint checkpoint) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Checkpoint Info", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Created: ", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(checkpoint.getFormattedTime(), NamedTextColor.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.text("Saved State:", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Health: ", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(String.format("%.1f", checkpoint.getPlayerState().getHealth()), NamedTextColor.RED)));
        lore.add(Component.text("• Food: ", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(checkpoint.getPlayerState().getFoodLevel() + "/20", NamedTextColor.GOLD)));
        lore.add(Component.text("• XP Level: ", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(String.valueOf(checkpoint.getPlayerState().getTotalExperience()), NamedTextColor.GREEN)));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the cancel button item.
     */
    private ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✗ CANCEL", NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to cancel", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("and keep current state.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handles a click in the confirmation GUI.
     *
     * @param player       the player who clicked
     * @param holder       the inventory holder
     * @param slot         the slot that was clicked
     */
    public void handleClick(Player player, ConfirmationHolder holder, int slot) {
        if (slot == CONFIRM_SLOT) {
            player.closeInventory();
            Checkpoint checkpoint = holder.getCheckpoint();

            // Verify checkpoint still exists and is valid
            Checkpoint current = plugin.getCheckpointManager().getCheckpointAt(checkpoint.getLocation());
            if (current == null || !current.getCheckpointId().equals(checkpoint.getCheckpointId())) {
                MessageUtil.error(player, "This checkpoint no longer exists.");
                return;
            }

            if (!current.isEnabled()) {
                MessageUtil.error(player, "This checkpoint is currently disabled.");
                return;
            }

            plugin.getCheckpointManager().restoreCheckpoint(player, checkpoint);
            MessageUtil.success(player, "Your state has been restored!");

            if (plugin.getConfigManager().isPenaltyEnabled()) {
                MessageUtil.warn(player, "A random item was lost as a restoration penalty.");
            }
        } else if (slot == CANCEL_SLOT) {
            player.closeInventory();
            MessageUtil.info(player, "Restoration cancelled.");
        }
    }

    /**
     * Handles the GUI being closed.
     *
     * @param player the player who closed the GUI
     * @param holder the inventory holder
     */
    public void handleClose(Player player, ConfirmationHolder holder) {
        pendingRestores.remove(player.getUniqueId());
    }

    /**
     * Custom InventoryHolder to identify confirmation GUIs.
     */
    public static class ConfirmationHolder implements InventoryHolder {
        private final Checkpoint checkpoint;

        public ConfirmationHolder(Checkpoint checkpoint) {
            this.checkpoint = checkpoint;
        }

        public Checkpoint getCheckpoint() {
            return checkpoint;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}