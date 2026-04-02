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
 * Supports different confirmation types for different scenarios.
 */
public class ConfirmationManager {

    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;

    private final QuantumCheckpoints plugin;
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
     * Requests a standard destructive-action confirmation.
     *
     * @param player      the player to prompt
     * @param action      the action to run on confirmation
     * @param description brief description of what will happen
     */
    public void requestConfirmation(Player player, Runnable action, String description) {
        UUID playerId = player.getUniqueId();
        cancelPendingConfirmation(playerId);

        PendingConfirmation confirmation = new PendingConfirmation(action, description);
        pendingConfirmations.put(playerId, confirmation);

        MessageUtil.warn(player, description);
        MessageUtil.info(player, "§e/cp confirm §7to proceed, §e/cp cancel §7to abort. §8(" +
                CONFIRMATION_TIMEOUT_SECONDS + "s)");

        scheduleTimeout(playerId, confirmation);
    }

    /**
     * Requests confirmation for overriding the player's own nearby checkpoint.
     *
     * @param player the player to prompt
     * @param action the action to run on confirmation
     * @param existingCheckpointOwner included for context (will always be the player themselves)
     */
    public void requestOverrideConfirmation(Player player, Runnable action, String existingCheckpointOwner) {
        UUID playerId = player.getUniqueId();
        cancelPendingConfirmation(playerId);

        String description = "You already have a checkpoint nearby. It will be replaced.";
        PendingConfirmation confirmation = new PendingConfirmation(action, description);
        pendingConfirmations.put(playerId, confirmation);

        MessageUtil.warn(player, description);
        MessageUtil.info(player, "§e/cp confirm §7to replace it, §e/cp cancel §7to abort. §8(" +
                CONFIRMATION_TIMEOUT_SECONDS + "s)");

        scheduleTimeout(playerId, confirmation);
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
     * Checks if a player has a pending confirmation.
     *
     * @param player the player to check
     * @return true if the player has a pending confirmation
     */
    public boolean hasPendingConfirmation(Player player) {
        return pendingConfirmations.containsKey(player.getUniqueId());
    }

    private boolean cancelPendingConfirmation(UUID playerId) {
        return pendingConfirmations.remove(playerId) != null;
    }

    /**
     * Schedules automatic timeout for a pending confirmation.
     */
    private void scheduleTimeout(UUID playerId, PendingConfirmation confirmation) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingConfirmations.remove(playerId, confirmation)) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        MessageUtil.error(player, "Confirmation expired.");
                    }
                }
            }
        }.runTaskLater(plugin, CONFIRMATION_TIMEOUT_SECONDS * 20L);
    }

    private record PendingConfirmation(Runnable action, String description) {}
}