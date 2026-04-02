package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.checkpoint.CheckpointManager;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Creates a checkpoint at the player's current location.
 * Triggered by '/cp' with no args or '/cp here'.
 */
public class CreateHereCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public CreateHereCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "here";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        Location location = player.getLocation();
        CheckpointManager.CheckpointResult result =
                plugin.getCheckpointManager().createCheckpoint(player, location);

        if (result.isSuccess()) {
            reportSuccess(player);
        } else if (result.isProximityConflict()) {
            Checkpoint conflicting = result.getCheckpoint();
            plugin.getConfirmationManager().requestOverrideConfirmation(
                    player,
                    () -> {
                        // Re-capture location at confirmation time in case player moved
                        CheckpointManager.CheckpointResult forced =
                                plugin.getCheckpointManager().forceCreateCheckpoint(
                                        player, location, conflicting);
                        if (forced.isSuccess()) {
                            reportSuccess(player);
                        } else {
                            MessageUtil.error(player, forced.getMessage());
                        }
                    },
                    conflicting.getOwnerName()
            );
        } else {
            MessageUtil.error(player, result.getMessage());
        }
    }

    private void reportSuccess(Player player) {
        MessageUtil.success(player, "Checkpoint created at your location!");
        int count = plugin.getCheckpointManager().getCheckpointCount(player.getUniqueId());
        int limit = plugin.getConfigManager().getCheckpointLimit();
        MessageUtil.info(player, "Checkpoints: §e" + count + "/" + limit);
    }
}