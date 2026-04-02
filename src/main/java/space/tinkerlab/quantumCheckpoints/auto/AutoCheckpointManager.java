package space.tinkerlab.quantumCheckpoints.auto;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.CheckpointManager;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages automatic checkpoint creation for players.
 * Each player can have their own interval, or use the server default.
 * Tracks per-player preferences and timing independently.
 */
public class AutoCheckpointManager {

    private final QuantumCheckpoints plugin;

    /** Per-player auto-checkpoint preferences */
    private final Map<UUID, AutoCheckpointPreference> preferences;

    /** Tracks when each player last had an auto-checkpoint created */
    private final Map<UUID, Long> lastAutoCheckpoint;

    /** The main tick task that checks all online players */
    private BukkitTask tickTask;

    /**
     * Creates a new AutoCheckpointManager.
     *
     * @param plugin the main plugin instance
     */
    public AutoCheckpointManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.preferences = new ConcurrentHashMap<>();
        this.lastAutoCheckpoint = new ConcurrentHashMap<>();
    }

    /**
     * Starts the auto-checkpoint tick loop.
     * Runs every 20 ticks (1 second) and checks each online player
     * against their individual interval.
     */
    public void start() {
        stop();

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfigManager().isAutoCheckpointEnabled()) {
                    return;
                }

                if (!plugin.getConfigManager().isCheckpointsEnabled()) {
                    return;
                }

                long now = System.currentTimeMillis();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    processPlayer(player, now);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }

    /**
     * Stops the auto-checkpoint tick loop.
     */
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    /**
     * Restarts the tick loop. Used after config reload.
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * Checks whether a specific player is due for an auto-checkpoint
     * and creates one if appropriate.
     *
     * @param player the player to process
     * @param now    the current timestamp
     */
    private void processPlayer(Player player, long now) {
        UUID playerId = player.getUniqueId();
        AutoCheckpointPreference pref = getPreference(playerId);

        // Skip if this player has auto-checkpoints disabled
        if (!pref.isEnabled()) {
            return;
        }

        // Calculate the effective interval for this player
        long intervalMs = getEffectiveIntervalMs(playerId);

        // Check if enough time has elapsed since last auto-checkpoint
        long lastTime = lastAutoCheckpoint.getOrDefault(playerId, 0L);
        if (now - lastTime < intervalMs) {
            return;
        }

        // Attempt to create the auto-checkpoint
        createAutoCheckpoint(player);
        lastAutoCheckpoint.put(playerId, now);
    }

    /**
     * Creates an automatic checkpoint for a player.
     * Handles cost failures by disabling auto-checkpoints for that player.
     *
     * @param player the player to create a checkpoint for
     */
    private void createAutoCheckpoint(Player player) {
        Location location = player.getLocation();

        CheckpointManager.CheckpointResult result =
                plugin.getCheckpointManager().createCheckpoint(player, location);

        if (result.isSuccess()) {
            int count = plugin.getCheckpointManager().getCheckpointCount(player.getUniqueId());
            int limit = plugin.getConfigManager().getCheckpointLimit();
            MessageUtil.info(player, "§8[Auto] §7Checkpoint saved. §e(" + count + "/" + limit + ")");
        } else if (result.isProximityConflict()) {
            // Silently override own nearby checkpoint for auto-checkpoints
            // since the player implicitly wants the latest state saved
            CheckpointManager.CheckpointResult forced =
                    plugin.getCheckpointManager().forceCreateCheckpoint(
                            player, location, result.getCheckpoint());

            if (forced.isSuccess()) {
                int count = plugin.getCheckpointManager().getCheckpointCount(player.getUniqueId());
                int limit = plugin.getConfigManager().getCheckpointLimit();
                MessageUtil.info(player, "§8[Auto] §7Checkpoint updated. §e(" + count + "/" + limit + ")");
            } else {
                handleAutoCheckpointFailure(player, forced.getMessage());
            }
        } else {
            handleAutoCheckpointFailure(player, result.getMessage());
        }
    }

    /**
     * Handles a failed auto-checkpoint by disabling auto-checkpoints for
     * the player and informing them. The player must manually re-enable.
     *
     * @param player  the affected player
     * @param reason  the failure reason
     */
    private void handleAutoCheckpointFailure(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        AutoCheckpointPreference pref = getPreference(playerId);
        pref.setEnabled(false);
        preferences.put(playerId, pref);

        MessageUtil.warn(player, "§8[Auto] §eAuto-checkpoint failed: " + reason);
        MessageUtil.warn(player, "§8[Auto] §eAuto-checkpoints disabled. Use §f/cp auto on §eto re-enable.");
    }

    /**
     * Gets the effective interval in milliseconds for a player.
     * Uses the player's custom interval if set, otherwise the server default.
     *
     * @param playerId the player's UUID
     * @return the interval in milliseconds
     */
    public long getEffectiveIntervalMs(UUID playerId) {
        AutoCheckpointPreference pref = preferences.get(playerId);
        int minutes;

        if (pref != null && pref.hasCustomInterval()) {
            minutes = pref.getIntervalMinutes();
        } else {
            minutes = plugin.getConfigManager().getAutoCheckpointInterval();
        }

        return minutes * 60_000L;
    }

    /**
     * Gets the effective interval in minutes for a player.
     *
     * @param playerId the player's UUID
     * @return the interval in minutes
     */
    public int getEffectiveIntervalMinutes(UUID playerId) {
        AutoCheckpointPreference pref = preferences.get(playerId);
        if (pref != null && pref.hasCustomInterval()) {
            return pref.getIntervalMinutes();
        }
        return plugin.getConfigManager().getAutoCheckpointInterval();
    }

    /**
     * Gets the preference for a player, creating a default if none exists.
     *
     * @param playerId the player's UUID
     * @return the player's preference
     */
    public AutoCheckpointPreference getPreference(UUID playerId) {
        return preferences.computeIfAbsent(playerId, k -> new AutoCheckpointPreference());
    }

    /**
     * Sets auto-checkpoint enabled/disabled for a player.
     *
     * @param playerId the player's UUID
     * @param enabled  whether auto-checkpoints should be enabled
     */
    public void setEnabled(UUID playerId, boolean enabled) {
        AutoCheckpointPreference pref = getPreference(playerId);
        pref.setEnabled(enabled);

        // Reset timer when re-enabled so it doesn't fire immediately
        if (enabled) {
            lastAutoCheckpoint.put(playerId, System.currentTimeMillis());
        }
    }

    /**
     * Sets a custom interval for a player.
     *
     * @param playerId the player's UUID
     * @param minutes  the interval in minutes
     * @return true if the interval was accepted
     */
    public boolean setPlayerInterval(UUID playerId, int minutes) {
        int minInterval = plugin.getConfigManager().getAutoCheckpointMinInterval();
        if (minutes < minInterval) {
            return false;
        }

        AutoCheckpointPreference pref = getPreference(playerId);
        pref.setIntervalMinutes(minutes);

        // Reset timer to align with new interval
        lastAutoCheckpoint.put(playerId, System.currentTimeMillis());
        return true;
    }

    /**
     * Clears a player's custom interval so they use the server default.
     *
     * @param playerId the player's UUID
     */
    public void clearPlayerInterval(UUID playerId) {
        AutoCheckpointPreference pref = getPreference(playerId);
        pref.clearCustomInterval();
    }

    /**
     * Checks whether a player has a custom interval set.
     *
     * @param playerId the player's UUID
     * @return true if the player has a custom interval
     */
    public boolean hasCustomInterval(UUID playerId) {
        AutoCheckpointPreference pref = preferences.get(playerId);
        return pref != null && pref.hasCustomInterval();
    }

    /**
     * Called when a player joins. Initializes their timer.
     *
     * @param playerId the player's UUID
     */
    public void onPlayerJoin(UUID playerId) {
        lastAutoCheckpoint.put(playerId, System.currentTimeMillis());
    }

    /**
     * Called when a player leaves. Cleans up timer data but preserves preferences.
     *
     * @param playerId the player's UUID
     */
    public void onPlayerQuit(UUID playerId) {
        lastAutoCheckpoint.remove(playerId);
    }

    /**
     * Gets all stored preferences for persistence.
     *
     * @return unmodifiable map of player preferences
     */
    public Map<UUID, AutoCheckpointPreference> getAllPreferences() {
        return Map.copyOf(preferences);
    }

    /**
     * Loads a preference for a player. Used during data loading.
     *
     * @param playerId   the player's UUID
     * @param preference the preference to load
     */
    public void loadPreference(UUID playerId, AutoCheckpointPreference preference) {
        preferences.put(playerId, preference);
    }
}