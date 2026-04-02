package space.tinkerlab.quantumCheckpoints.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;

/**
 * Handles player join and quit events for checkpoint visibility
 * and auto-checkpoint timer management.
 */
public class PlayerJoinListener implements Listener {

    private final QuantumCheckpoints plugin;

    /**
     * Creates a new PlayerJoinListener.
     *
     * @param plugin the main plugin instance
     */
    public PlayerJoinListener(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes checkpoint visibility and auto-checkpoint timer on join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay visibility update by 1 tick for entity tracking
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getBeamManager().updateVisibilityForPlayer(player);
            }
        }, 1L);

        // Initialize auto-checkpoint timer
        plugin.getAutoCheckpointManager().onPlayerJoin(player.getUniqueId());
    }

    /**
     * Cleans up timer data on quit. Preferences are preserved.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getAutoCheckpointManager().onPlayerQuit(event.getPlayer().getUniqueId());
    }
}