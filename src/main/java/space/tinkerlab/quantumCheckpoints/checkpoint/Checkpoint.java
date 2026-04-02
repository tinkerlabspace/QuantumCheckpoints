// src/main/java/space/tinkerlab/quantumCheckpoints/checkpoint/Checkpoint.java
package space.tinkerlab.quantumCheckpoints.checkpoint;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a checkpoint in the world.
 * Contains the location, owner, creation time, and saved player state.
 */
public class Checkpoint {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UUID checkpointId;
    private final UUID ownerId;
    private final String ownerName;
    private final Location location;
    private final long creationTime;
    private final PlayerState playerState;
    private boolean enabled;

    /**
     * Creates a new checkpoint with the specified parameters.
     *
     * @param ownerId    the UUID of the player who owns this checkpoint
     * @param ownerName  the name of the player who owns this checkpoint
     * @param location   the location of the checkpoint
     * @param playerState the saved player state
     */
    public Checkpoint(UUID ownerId, String ownerName, Location location, PlayerState playerState) {
        this.checkpointId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location.clone();
        this.creationTime = System.currentTimeMillis();
        this.playerState = playerState;
        this.enabled = true;
    }

    /**
     * Creates a checkpoint from serialized data.
     * Used when loading from storage.
     */
    public Checkpoint(UUID checkpointId, UUID ownerId, String ownerName,
                      Location location, long creationTime,
                      PlayerState playerState, boolean enabled) {
        this.checkpointId = checkpointId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location;
        this.creationTime = creationTime;
        this.playerState = playerState;
        this.enabled = enabled;
    }

    /**
     * Gets the formatted creation time string for display.
     *
     * @return the formatted time string
     */
    public String getFormattedTime() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(creationTime), ZoneId.systemDefault());
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * Gets a unique location key for this checkpoint.
     * Uses block coordinates to ensure consistent lookup.
     *
     * @return a string key representing this checkpoint's location
     */
    public String getLocationKey() {
        return createLocationKey(location);
    }

    /**
     * Creates a location key from a Location object.
     *
     * @param loc the location
     * @return the location key string
     */
    public static String createLocationKey(Location loc) {
        return loc.getWorld().getName() + ":" +
                loc.getBlockX() + ":" +
                loc.getBlockY() + ":" +
                loc.getBlockZ();
    }

    /**
     * Converts this checkpoint to a Map for YAML serialization.
     *
     * @return a map representation of this checkpoint
     */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("checkpointId", checkpointId.toString());
        map.put("ownerId", ownerId.toString());
        map.put("ownerName", ownerName);
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        map.put("creationTime", creationTime);
        map.put("playerState", playerState.serialize());
        map.put("enabled", enabled);
        return map;
    }

    /**
     * Creates a Checkpoint from a serialized Map.
     *
     * @param map the serialized data
     * @return the deserialized Checkpoint, or null if the world doesn't exist
     */
    @SuppressWarnings("unchecked")
    public static Checkpoint deserialize(Map<String, Object> map) {
        UUID checkpointId = UUID.fromString((String) map.get("checkpointId"));
        UUID ownerId = UUID.fromString((String) map.get("ownerId"));
        String ownerName = (String) map.get("ownerName");

        String worldName = (String) map.get("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null; // World doesn't exist anymore
        }

        double x = ((Number) map.get("x")).doubleValue();
        double y = ((Number) map.get("y")).doubleValue();
        double z = ((Number) map.get("z")).doubleValue();
        float yaw = ((Number) map.get("yaw")).floatValue();
        float pitch = ((Number) map.get("pitch")).floatValue();
        Location location = new Location(world, x, y, z, yaw, pitch);

        long creationTime = ((Number) map.get("creationTime")).longValue();
        PlayerState state = PlayerState.deserialize((Map<String, Object>) map.get("playerState"));
        boolean enabled = (boolean) map.getOrDefault("enabled", true);

        return new Checkpoint(checkpointId, ownerId, ownerName, location,
                creationTime, state, enabled);
    }

    // Getters and setters
    public UUID getCheckpointId() {
        return checkpointId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Location getLocation() {
        return location.clone();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}