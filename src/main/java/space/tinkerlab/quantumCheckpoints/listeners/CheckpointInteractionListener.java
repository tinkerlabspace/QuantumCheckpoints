// src/main/java/space/tinkerlab/quantumCheckpoints/listeners/CheckpointInteractionListener.java
package space.tinkerlab.quantumCheckpoints.listeners;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;
import space.tinkerlab.quantumCheckpoints.visual.BeamManager;

/**
 * Listens for player interactions with checkpoint entities.
 */
public class CheckpointInteractionListener implements Listener {

    private final QuantumCheckpoints plugin;

    /**
     * Creates a new CheckpointInteractionListener.
     *
     * @param plugin the main plugin instance
     */
    public CheckpointInteractionListener(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles right-click interactions on checkpoint armor stands.
     */
    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();

        // Check if this is a checkpoint armor stand
        if (!(entity instanceof ArmorStand armorStand)) {
            return;
        }

        if (!armorStand.hasMetadata(BeamManager.CHECKPOINT_METADATA_KEY)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        Location checkpointLocation = armorStand.getLocation();

        // Find the checkpoint at this location
        Checkpoint checkpoint = findCheckpointNear(checkpointLocation);

        if (checkpoint == null) {
            MessageUtil.error(player, "This checkpoint no longer exists.");
            return;
        }

        // Check ownership
        if (!checkpoint.getOwnerId().equals(player.getUniqueId())) {
            MessageUtil.error(player, "This checkpoint belongs to " + checkpoint.getOwnerName() + ".");
            return;
        }

        // Check if enabled
        if (!checkpoint.isEnabled()) {
            MessageUtil.error(player, "This checkpoint is currently disabled.");
            return;
        }

        // Open confirmation GUI
        plugin.getConfirmationGUI().openRestoreConfirmation(player, checkpoint);
    }

    /**
     * Finds a checkpoint near the specified location.
     * Searches within a small radius to account for armor stand positioning.
     *
     * @param location the location to search near
     * @return the checkpoint, or null if not found
     */
    private Checkpoint findCheckpointNear(Location location) {
        // Try exact location first
        Checkpoint checkpoint = plugin.getCheckpointManager().getCheckpointAt(location);
        if (checkpoint != null) {
            return checkpoint;
        }

        // Search nearby locations (within 2 blocks radius, same Y level give or take 3)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    Location searchLoc = location.clone().add(dx, dy, dz);
                    checkpoint = plugin.getCheckpointManager().getCheckpointAt(searchLoc);
                    if (checkpoint != null) {
                        return checkpoint;
                    }
                }
            }
        }

        return null;
    }
}