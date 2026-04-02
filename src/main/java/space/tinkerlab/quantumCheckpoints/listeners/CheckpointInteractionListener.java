package space.tinkerlab.quantumCheckpoints.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.metadata.MetadataValue;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;
import space.tinkerlab.quantumCheckpoints.visual.BeamManager;

import java.util.List;
import java.util.UUID;

/**
 * Listens for player interactions with checkpoint beam entities.
 * Uses entity metadata to identify which checkpoint was interacted with,
 * avoiding location-based lookup issues.
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
     * Handles right-click interactions on any armor stand that belongs to a checkpoint beam.
     * The checkpoint ID is stored in entity metadata, so we look up directly by ID
     * rather than trying to match by location.
     */
    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (!(entity instanceof ArmorStand armorStand)) {
            return;
        }

        if (!armorStand.hasMetadata(BeamManager.CHECKPOINT_METADATA_KEY)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        // Extract checkpoint ID from entity metadata
        UUID checkpointId = getCheckpointIdFromMetadata(armorStand);
        if (checkpointId == null) {
            MessageUtil.error(player, "This checkpoint no longer exists.");
            return;
        }

        // Look up checkpoint by ID from the manager
        Checkpoint checkpoint = findCheckpointById(checkpointId);
        if (checkpoint == null) {
            MessageUtil.error(player, "This checkpoint no longer exists.");
            return;
        }

        if (!checkpoint.getOwnerId().equals(player.getUniqueId())) {
            MessageUtil.error(player, "This checkpoint belongs to " + checkpoint.getOwnerName() + ".");
            return;
        }

        if (!checkpoint.isEnabled()) {
            MessageUtil.error(player, "This checkpoint is currently disabled.");
            return;
        }

        plugin.getConfirmationGUI().openRestoreConfirmation(player, checkpoint);
    }

    /**
     * Extracts the checkpoint UUID from an armor stand's metadata.
     *
     * @param armorStand the armor stand to read metadata from
     * @return the checkpoint UUID, or null if metadata is missing or malformed
     */
    private UUID getCheckpointIdFromMetadata(ArmorStand armorStand) {
        List<MetadataValue> values = armorStand.getMetadata(BeamManager.CHECKPOINT_METADATA_KEY);
        if (values.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(values.get(0).asString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Finds a checkpoint by its UUID.
     *
     * @param checkpointId the checkpoint UUID
     * @return the checkpoint, or null if not found
     */
    private Checkpoint findCheckpointById(UUID checkpointId) {
        return plugin.getCheckpointManager().getAllCheckpoints().stream()
                .filter(cp -> cp.getCheckpointId().equals(checkpointId))
                .findFirst()
                .orElse(null);
    }
}