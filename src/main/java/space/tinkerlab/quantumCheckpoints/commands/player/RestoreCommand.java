package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.LocationUtil;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/cp restore' — restores a checkpoint at the player's location.
 */
public class RestoreCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public RestoreCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "restore";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        Checkpoint checkpoint = LocationUtil.findNearestOwnedCheckpoint(plugin, player);

        if (checkpoint == null) {
            MessageUtil.error(player, "No checkpoint found near you.");
            return;
        }

        if (!checkpoint.isEnabled()) {
            MessageUtil.error(player, "This checkpoint is currently disabled.");
            return;
        }

        plugin.getConfirmationGUI().openRestoreConfirmation(player, checkpoint);
    }
}