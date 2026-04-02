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
 */
public class CheckpointManager {

    private final QuantumCheckpoints plugin;
    private final Map<String, Checkpoint> checkpointsByLocation;
    private final Map<UUID, Set<UUID>> checkpointsByPlayer;
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
     * Attempts to create a checkpoint. If the player already has a checkpoint
     * within proximity, returns a proximity conflict so the caller can ask
     * for override confirmation before proceeding.
     *
     * @param player   the player creating the checkpoint
     * @param location the desired location
     * @return the result of the creation attempt
     */
    public CheckpointResult createCheckpoint(Player player, Location location) {
        if (!plugin.getConfigManager().isCheckpointsEnabled()) {
            return CheckpointResult.failure("Checkpoints are currently disabled.");
        }

        // Only check the creating player's own checkpoints for proximity conflicts.
        // Different players are allowed to have checkpoints near each other.
        Checkpoint ownNearby = findNearbyOwnedCheckpoint(player.getUniqueId(), location);
        if (ownNearby != null) {
            return CheckpointResult.proximityConflict(ownNearby);
        }

        return executeCreate(player, location);
    }

    /**
     * Force-creates a checkpoint after the player confirmed overriding their
     * own nearby checkpoint.
     *
     * @param player      the player creating the checkpoint
     * @param location    the desired location
     * @param conflicting the player's own checkpoint to remove first
     * @return the result of the creation attempt
     */
    public CheckpointResult forceCreateCheckpoint(Player player, Location location, Checkpoint conflicting) {
        if (!plugin.getConfigManager().isCheckpointsEnabled()) {
            return CheckpointResult.failure("Checkpoints are currently disabled.");
        }

        removeCheckpoint(conflicting);
        return executeCreate(player, location);
    }

    /**
     * Shared creation logic after proximity checks have passed.
     */
    private CheckpointResult executeCreate(Player player, Location location) {
        if (!handleCost(player)) {
            return CheckpointResult.failure("Insufficient resources. Cost: " +
                    plugin.getConfigManager().getCostDescription());
        }

        enforceCheckpointLimit(player);

        PlayerState state = new PlayerState(player);
        Checkpoint checkpoint = new Checkpoint(player.getUniqueId(), player.getName(), location, state);
        addCheckpoint(checkpoint);
        plugin.getBeamManager().createBeam(checkpoint);

        return CheckpointResult.success(checkpoint);
    }

    /**
     * Finds a checkpoint owned by the specified player within the configured proximity radius.
     *
     * @param playerId the owner to match
     * @param location the location to search around
     * @return the nearby owned checkpoint, or null if none found
     */
    private Checkpoint findNearbyOwnedCheckpoint(UUID playerId, Location location) {
        double radius = plugin.getConfigManager().getProximityRadius();

        return getCheckpointsForPlayer(playerId).stream()
                .filter(cp -> cp.getLocation().getWorld().equals(location.getWorld()))
                .filter(cp -> {
                    Location cpLoc = cp.getLocation();
                    return Math.abs(cpLoc.getBlockX() - location.getBlockX()) <= radius
                            && Math.abs(cpLoc.getBlockY() - location.getBlockY()) <= radius
                            && Math.abs(cpLoc.getBlockZ() - location.getBlockZ()) <= radius;
                })
                .findFirst()
                .orElse(null);
    }

    private boolean handleCost(Player player) {
        ItemStack cost = plugin.getConfigManager().getCheckpointCost();
        if (cost == null) {
            return true;
        }
        if (!player.getInventory().containsAtLeast(cost, cost.getAmount())) {
            return false;
        }
        player.getInventory().removeItem(cost);
        return true;
    }

    private void enforceCheckpointLimit(Player player) {
        int limit = plugin.getConfigManager().getCheckpointLimit();
        Set<UUID> playerCheckpoints = checkpointsByPlayer.get(player.getUniqueId());

        if (playerCheckpoints == null || playerCheckpoints.size() < limit) {
            return;
        }

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
     * Restores a checkpoint state to a player, applying penalty if configured.
     * Destroys the checkpoint after restoration.
     *
     * @param player     the player to restore
     * @param checkpoint the checkpoint to restore from
     */
    public void restoreCheckpoint(Player player, Checkpoint checkpoint) {
        checkpoint.getPlayerState().restore(player);

        if (plugin.getConfigManager().isPenaltyEnabled()) {
            applyRestorationPenalty(player);
        }

        removeCheckpoint(checkpoint);
    }

    private void applyRestorationPenalty(Player player) {
        List<Integer> occupiedSlots = new ArrayList<>();

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < 36; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                occupiedSlots.add(i);
            }
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir()) {
                occupiedSlots.add(36 + i);
            }
        }

        if (player.getInventory().getItemInOffHand().getType() != org.bukkit.Material.AIR) {
            occupiedSlots.add(40);
        }

        if (!occupiedSlots.isEmpty()) {
            int slot = occupiedSlots.get(ThreadLocalRandom.current().nextInt(occupiedSlots.size()));
            player.getInventory().setItem(slot, null);
        }
    }

    /**
     * Gets a checkpoint by its unique ID.
     *
     * @param id the checkpoint UUID
     * @return the checkpoint, or null if not found
     */
    public Checkpoint getCheckpointById(UUID id) {
        return checkpointsById.get(id);
    }

    public Checkpoint getCheckpointAt(Location location) {
        return checkpointsByLocation.get(Checkpoint.createLocationKey(location));
    }

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

    public Collection<Checkpoint> getAllCheckpoints() {
        return Collections.unmodifiableCollection(checkpointsById.values());
    }

    public void clearAllCheckpoints() {
        for (Checkpoint checkpoint : checkpointsById.values()) {
            plugin.getBeamManager().removeBeam(checkpoint);
        }
        checkpointsById.clear();
        checkpointsByLocation.clear();
        checkpointsByPlayer.clear();
    }

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

    public int getCheckpointCount(UUID playerId) {
        Set<UUID> checkpointIds = checkpointsByPlayer.get(playerId);
        return checkpointIds != null ? checkpointIds.size() : 0;
    }

    /**
     * Encapsulates the result of a checkpoint creation attempt.
     */
    public static class CheckpointResult {
        private final boolean success;
        private final boolean proximityConflict;
        private final String message;
        private final Checkpoint checkpoint;

        private CheckpointResult(boolean success, boolean proximityConflict,
                                 String message, Checkpoint checkpoint) {
            this.success = success;
            this.proximityConflict = proximityConflict;
            this.message = message;
            this.checkpoint = checkpoint;
        }

        public static CheckpointResult success(Checkpoint checkpoint) {
            return new CheckpointResult(true, false, null, checkpoint);
        }

        public static CheckpointResult failure(String message) {
            return new CheckpointResult(false, false, message, null);
        }

        public static CheckpointResult proximityConflict(Checkpoint conflicting) {
            return new CheckpointResult(false, true, null, conflicting);
        }

        public boolean isSuccess() { return success; }
        public boolean isProximityConflict() { return proximityConflict; }
        public String getMessage() { return message; }
        public Checkpoint getCheckpoint() { return checkpoint; }
    }
}