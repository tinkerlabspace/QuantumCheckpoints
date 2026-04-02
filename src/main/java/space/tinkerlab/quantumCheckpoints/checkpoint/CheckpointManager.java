// src/main/java/space/tinkerlab/quantumCheckpoints/checkpoint/CheckpointManager.java
package space.tinkerlab.quantumCheckpoints.checkpoint;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Manages all checkpoint operations including creation, deletion, and restoration.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class CheckpointManager {

    private final QuantumCheckpoints plugin;

    /** Maps location keys to checkpoints for fast location-based lookup */
    private final Map<String, Checkpoint> checkpointsByLocation;

    /** Maps player UUIDs to their checkpoint IDs for ownership tracking */
    private final Map<UUID, Set<UUID>> checkpointsByPlayer;

    /** Maps checkpoint IDs to checkpoints for direct access */
    private final Map<UUID, Checkpoint> checkpointsById;

    /**
     * Creates a new CheckpointManager.
     *
     * @param plugin the main plugin instance
     */
    public CheckpointManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.checkpointsByLocation = new ConcurrentHashMap<>();
        this.checkpointsByPlayer = new ConcurrentHashMap<>();
        this.checkpointsById = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new checkpoint for a player at the specified location.
     * Handles limit enforcement, cost deduction, and duplicate location handling.
     *
     * @param player   the player creating the checkpoint
     * @param location the location for the checkpoint
     * @return the result of the creation attempt
     */
    public CheckpointResult createCheckpoint(Player player, Location location) {
        // Check if checkpoints are disabled
        if (!plugin.getConfigManager().isCheckpointsEnabled()) {
            return CheckpointResult.failure("Checkpoints are currently disabled on this server.");
        }

        // Check and handle cost
        if (!handleCost(player)) {
            return CheckpointResult.failure("You don't have enough resources to create a checkpoint.");
        }

        // Remove existing checkpoint at this location (if owned by this player)
        String locationKey = Checkpoint.createLocationKey(location);
        Checkpoint existing = checkpointsByLocation.get(locationKey);
        if (existing != null) {
            if (!existing.getOwnerId().equals(player.getUniqueId())) {
                return CheckpointResult.failure("There is already a checkpoint at this location owned by another player.");
            }
            removeCheckpoint(existing);
        }

        // Check and enforce limit (remove oldest if necessary)
        enforceCheckpointLimit(player);

        // Create the checkpoint
        PlayerState state = new PlayerState(player);
        Checkpoint checkpoint = new Checkpoint(
                player.getUniqueId(),
                player.getName(),
                location,
                state
        );

        // Store the checkpoint
        addCheckpoint(checkpoint);

        // Create visual beam
        plugin.getBeamManager().createBeam(checkpoint);

        return CheckpointResult.success(checkpoint);
    }

    /**
     * Handles the cost deduction for checkpoint creation.
     *
     * @param player the player to deduct from
     * @return true if cost was handled (either no cost or successfully deducted)
     */
    private boolean handleCost(Player player) {
        ItemStack cost = plugin.getConfigManager().getCheckpointCost();
        if (cost == null) {
            return true; // No cost configured
        }

        if (!player.getInventory().containsAtLeast(cost, cost.getAmount())) {
            return false;
        }

        player.getInventory().removeItem(cost);
        return true;
    }

    /**
     * Enforces the checkpoint limit for a player.
     * Removes the oldest checkpoint if the limit would be exceeded.
     *
     * @param player the player to check
     */
    private void enforceCheckpointLimit(Player player) {
        int limit = plugin.getConfigManager().getCheckpointLimit();
        Set<UUID> playerCheckpoints = checkpointsByPlayer.get(player.getUniqueId());

        if (playerCheckpoints == null || playerCheckpoints.size() < limit) {
            return;
        }

        // Find and remove the oldest checkpoint
        Checkpoint oldest = playerCheckpoints.stream()
                .map(checkpointsById::get)
                .filter(Objects::nonNull)
                .min(Comparator.comparingLong(Checkpoint::getCreationTime))
                .orElse(null);

        if (oldest != null) {
            removeCheckpoint(oldest);
        }
    }

    /**
     * Adds a checkpoint to all tracking maps.
     *
     * @param checkpoint the checkpoint to add
     */
    public void addCheckpoint(Checkpoint checkpoint) {
        checkpointsById.put(checkpoint.getCheckpointId(), checkpoint);
        checkpointsByLocation.put(checkpoint.getLocationKey(), checkpoint);

        checkpointsByPlayer
                .computeIfAbsent(checkpoint.getOwnerId(), k -> ConcurrentHashMap.newKeySet())
                .add(checkpoint.getCheckpointId());
    }

    /**
     * Removes a checkpoint from all tracking maps and its visual beam.
     *
     * @param checkpoint the checkpoint to remove
     */
    public void removeCheckpoint(Checkpoint checkpoint) {
        checkpointsById.remove(checkpoint.getCheckpointId());
        checkpointsByLocation.remove(checkpoint.getLocationKey());

        Set<UUID> playerCheckpoints = checkpointsByPlayer.get(checkpoint.getOwnerId());
        if (playerCheckpoints != null) {
            playerCheckpoints.remove(checkpoint.getCheckpointId());
            if (playerCheckpoints.isEmpty()) {
                checkpointsByPlayer.remove(checkpoint.getOwnerId());
            }
        }

        plugin.getBeamManager().removeBeam(checkpoint);
    }

    /**
     * Restores a checkpoint for a player, applying the penalty if configured.
     *
     * @param player     the player to restore
     * @param checkpoint the checkpoint to restore
     */
    public void restoreCheckpoint(Player player, Checkpoint checkpoint) {
        // Restore the player state
        checkpoint.getPlayerState().restore(player);

        // Apply penalty if enabled
        if (plugin.getConfigManager().isPenaltyEnabled()) {
            applyRestorationPenalty(player);
        }

        // Remove the checkpoint after restoration
        removeCheckpoint(checkpoint);
    }

    /**
     * Applies the restoration penalty by removing a random item from the inventory.
     * Can remove from main inventory or armor slots.
     *
     * @param player the player to apply penalty to
     */
    private void applyRestorationPenalty(Player player) {
        List<Integer> occupiedSlots = new ArrayList<>();

        // Check main inventory (slots 0-35)
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < 36; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                occupiedSlots.add(i);
            }
        }

        // Check armor slots (slots 36-39)
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir()) {
                occupiedSlots.add(36 + i);
            }
        }

        // Check off-hand (slot 40)
        if (player.getInventory().getItemInOffHand().getType() != org.bukkit.Material.AIR) {
            occupiedSlots.add(40);
        }

        if (!occupiedSlots.isEmpty()) {
            int randomSlot = occupiedSlots.get(ThreadLocalRandom.current().nextInt(occupiedSlots.size()));
            player.getInventory().setItem(randomSlot, null);
        }
    }

    /**
     * Gets a checkpoint at the specified location.
     *
     * @param location the location to check
     * @return the checkpoint, or null if none exists
     */
    public Checkpoint getCheckpointAt(Location location) {
        return checkpointsByLocation.get(Checkpoint.createLocationKey(location));
    }

    /**
     * Gets all checkpoints owned by a player.
     *
     * @param playerId the player's UUID
     * @return a list of checkpoints owned by the player
     */
    public List<Checkpoint> getCheckpointsForPlayer(UUID playerId) {
        Set<UUID> checkpointIds = checkpointsByPlayer.get(playerId);
        if (checkpointIds == null) {
            return Collections.emptyList();
        }

        return checkpointIds.stream()
                .map(checkpointsById::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(Checkpoint::getCreationTime))
                .collect(Collectors.toList());
    }

    /**
     * Gets all checkpoints in the server.
     *
     * @return a collection of all checkpoints
     */
    public Collection<Checkpoint> getAllCheckpoints() {
        return Collections.unmodifiableCollection(checkpointsById.values());
    }

    /**
     * Clears all checkpoints from the server.
     */
    public void clearAllCheckpoints() {
        // Remove all beams first
        for (Checkpoint checkpoint : checkpointsById.values()) {
            plugin.getBeamManager().removeBeam(checkpoint);
        }

        checkpointsById.clear();
        checkpointsByLocation.clear();
        checkpointsByPlayer.clear();
    }

    /**
     * Sets the enabled state for all checkpoints.
     *
     * @param enabled whether checkpoints should be enabled
     */
    public void setAllCheckpointsEnabled(boolean enabled) {
        for (Checkpoint checkpoint : checkpointsById.values()) {
            checkpoint.setEnabled(enabled);
            if (enabled) {
                plugin.getBeamManager().createBeam(checkpoint);
            } else {
                plugin.getBeamManager().removeBeam(checkpoint);
            }
        }
    }

    /**
     * Gets the count of checkpoints for a player.
     *
     * @param playerId the player's UUID
     * @return the number of checkpoints
     */
    public int getCheckpointCount(UUID playerId) {
        Set<UUID> checkpointIds = checkpointsByPlayer.get(playerId);
        return checkpointIds != null ? checkpointIds.size() : 0;
    }

    /**
     * Result class for checkpoint creation operations.
     */
    public static class CheckpointResult {
        private final boolean success;
        private final String message;
        private final Checkpoint checkpoint;

        private CheckpointResult(boolean success, String message, Checkpoint checkpoint) {
            this.success = success;
            this.message = message;
            this.checkpoint = checkpoint;
        }

        public static CheckpointResult success(Checkpoint checkpoint) {
            return new CheckpointResult(true, null, checkpoint);
        }

        public static CheckpointResult failure(String message) {
            return new CheckpointResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Checkpoint getCheckpoint() {
            return checkpoint;
        }
    }
}