package space.tinkerlab.quantumCheckpoints.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;

/**
 * Utility class for checkpoint location lookups.
 */
public final class LocationUtil {

    private LocationUtil() {}

    /**
     * Finds the nearest checkpoint owned by the player within the configured proximity radius.
     *
     * @param plugin the plugin instance
     * @param player the player to search around
     * @return the nearest owned checkpoint, or null if none found
     */
    @Nullable
    public static Checkpoint findNearestOwnedCheckpoint(@NotNull QuantumCheckpoints plugin,
                                                        @NotNull Player player) {
        Location playerLoc = player.getLocation();
        double radius = plugin.getConfigManager().getProximityRadius();

        return plugin.getCheckpointManager().getCheckpointsForPlayer(player.getUniqueId())
                .stream()
                .filter(cp -> cp.getLocation().getWorld().equals(playerLoc.getWorld()))
                .filter(cp -> isWithinRadius(cp.getLocation(), playerLoc, radius))
                .min((a, b) -> {
                    double distA = a.getLocation().distanceSquared(playerLoc);
                    double distB = b.getLocation().distanceSquared(playerLoc);
                    return Double.compare(distA, distB);
                })
                .orElse(null);
    }

    /**
     * Checks whether two locations are within a given radius on all axes.
     */
    private static boolean isWithinRadius(Location a, Location b, double radius) {
        return Math.abs(a.getBlockX() - b.getBlockX()) <= radius
                && Math.abs(a.getBlockY() - b.getBlockY()) <= radius
                && Math.abs(a.getBlockZ() - b.getBlockZ()) <= radius;
    }
}