package space.tinkerlab.quantumCheckpoints.checkpoint;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
     * @param ownerId     the UUID of the player who owns this checkpoint
     * @param ownerName   the name of the player who owns this checkpoint
     * @param location    the location of the checkpoint
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
     */
    private Checkpoint(UUID checkpointId, UUID ownerId, String ownerName,
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
     * Serializes this checkpoint into a ConfigurationSection.
     *
     * @param section the section to write into
     */
    public void serialize(ConfigurationSection section) {
        section.set("checkpointId", checkpointId.toString());
        section.set("ownerId", ownerId.toString());
        section.set("ownerName", ownerName);
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", (double) location.getYaw());
        section.set("pitch", (double) location.getPitch());
        section.set("creationTime", creationTime);
        section.set("enabled", enabled);

        playerState.serialize(section.createSection("playerState"));
    }

    /**
     * Deserializes a checkpoint from a ConfigurationSection.
     *
     * @param section the section to read from
     * @return the deserialized Checkpoint, or null if the world doesn't exist
     */
    public static Checkpoint deserialize(ConfigurationSection section) {
        UUID checkpointId = UUID.fromString(section.getString("checkpointId"));
        UUID ownerId = UUID.fromString(section.getString("ownerId"));
        String ownerName = section.getString("ownerName");

        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        Location location = new Location(world, x, y, z, yaw, pitch);

        long creationTime = section.getLong("creationTime");
        boolean enabled = section.getBoolean("enabled", true);

        ConfigurationSection stateSection = section.getConfigurationSection("playerState");
        if (stateSection == null) {
            return null;
        }
        PlayerState state = PlayerState.deserialize(stateSection);

        return new Checkpoint(checkpointId, ownerId, ownerName, location,
                creationTime, state, enabled);
    }

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