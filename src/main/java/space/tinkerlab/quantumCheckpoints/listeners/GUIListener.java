// src/main/java/space/tinkerlab/quantumCheckpoints/listeners/GUIListener.java
package space.tinkerlab.quantumCheckpoints.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.gui.ConfirmationGUI;

/**
 * Listens for GUI inventory events.
 */
public class GUIListener implements Listener {

    private final QuantumCheckpoints plugin;

    /**
     * Creates a new GUIListener.
     *
     * @param plugin the main plugin instance
     */
    public GUIListener(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles clicks within checkpoint GUIs.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (!(holder instanceof ConfirmationGUI.ConfirmationHolder confirmHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        plugin.getConfirmationGUI().handleClick(player, confirmHolder, slot);
    }

    /**
     * Handles GUI close events for cleanup.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ConfirmationGUI.ConfirmationHolder confirmHolder) {
            plugin.getConfirmationGUI().handleClose((Player) event.getPlayer(), confirmHolder);
        }
    }
}