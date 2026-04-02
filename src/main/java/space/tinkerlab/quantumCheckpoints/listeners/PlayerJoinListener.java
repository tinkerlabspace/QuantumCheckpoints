package space.tinkerlab.quantumCheckpoints.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;

/**
 * Handles player join events to update checkpoint beam visibility.
 * Ensures players only see their own checkpoint beams.
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
     * Updates checkpoint visibility when a player joins.
     * Runs one tick later to ensure entities are fully loaded on the client.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Delay by 1 tick so the player's entity tracking is fully initialised
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getBeamManager().updateVisibilityForPlayer(player);
            }
        }, 1L);
    }
}