// src/main/java/space/tinkerlab/quantumCheckpoints/commands/ConfirmationManager.java
package space.tinkerlab.quantumCheckpoints.commands;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pending command confirmations for destructive operations.
 */
public class ConfirmationManager {

    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;

    private final QuantumCheckpoints plugin;

    /** Maps player UUIDs to their pending confirmations */
    private final Map<UUID, PendingConfirmation> pendingConfirmations;

    /**
     * Creates a new ConfirmationManager.
     *
     * @param plugin the main plugin instance
     */
    public ConfirmationManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.pendingConfirmations = new ConcurrentHashMap<>();
    }

    /**
     * Requests confirmation from a player for a destructive action.
     *
     * @param player      the player to request confirmation from
     * @param action      the action to confirm
     * @param description a description of what will happen
     */
    public void requestConfirmation(Player player, Runnable action, String description) {
        UUID playerId = player.getUniqueId();

        // Cancel any existing pending confirmation
        cancelPendingConfirmation(playerId);

        PendingConfirmation confirmation = new PendingConfirmation(action, description);
        pendingConfirmations.put(playerId, confirmation);

        MessageUtil.warn(player, "⚠ " + description);
        MessageUtil.info(player, "Type §e/checkpoint confirm§7 or §e/checkpoints confirm§7 within " +
                CONFIRMATION_TIMEOUT_SECONDS + " seconds to confirm.");
        MessageUtil.info(player, "Type §e/checkpoint cancel§7 to cancel.");

        // Schedule timeout
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingConfirmations.remove(playerId, confirmation)) {
                    if (player.isOnline()) {
                        MessageUtil.error(player, "Confirmation timed out.");
                    }
                }
            }
        }.runTaskLater(plugin, CONFIRMATION_TIMEOUT_SECONDS * 20L);
    }

    /**
     * Confirms and executes a pending action for a player.
     *
     * @param player the player confirming
     * @return true if there was a pending confirmation to execute
     */
    public boolean confirm(Player player) {
        PendingConfirmation confirmation = pendingConfirmations.remove(player.getUniqueId());
        if (confirmation != null) {
            confirmation.action().run();
            return true;
        }
        return false;
    }

    /**
     * Cancels a pending confirmation for a player.
     *
     * @param player the player canceling
     * @return true if there was a pending confirmation to cancel
     */
    public boolean cancel(Player player) {
        return cancelPendingConfirmation(player.getUniqueId());
    }

    /**
     * Cancels a pending confirmation by player UUID.
     *
     * @param playerId the player's UUID
     * @return true if there was a pending confirmation to cancel
     */
    private boolean cancelPendingConfirmation(UUID playerId) {
        return pendingConfirmations.remove(playerId) != null;
    }

    /**
     * Checks if a player has a pending confirmation.
     *
     * @param player the player to check
     * @return true if the player has a pending confirmation
     */
    public boolean hasPendingConfirmation(Player player) {
        return pendingConfirmations.containsKey(player.getUniqueId());
    }

    /**
     * Record to hold a pending confirmation action.
     */
    private record PendingConfirmation(Runnable action, String description) {}
}