package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.CheckpointManager;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.CoordinateUtil;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.List;

/**
 * Creates a checkpoint at specified X Z coordinates.
 * Triggered by '/cp <x> <z>'.
 *
 * Registered under the name "at" so the dispatcher can route
 * unrecognised first arguments (numeric/tilde) to this handler.
 */
public class CreateAtCoordsCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public CreateAtCoordsCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "at";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            MessageUtil.error(player, "Usage: /cp <x> <z>");
            return;
        }

        try {
            double x = CoordinateUtil.parse(args[0], player.getLocation().getX());
            double z = CoordinateUtil.parse(args[1], player.getLocation().getZ());

            Location location = new Location(
                    player.getWorld(),
                    x,
                    player.getWorld().getHighestBlockYAt((int) x, (int) z) + 1,
                    z
            );

            CheckpointManager.CheckpointResult result =
                    plugin.getCheckpointManager().createCheckpoint(player, location);

            if (result.isSuccess()) {
                reportSuccess(player, (int) x, (int) z);
            } else if (result.isProximityConflict()) {
                plugin.getConfirmationManager().requestOverrideConfirmation(
                        player,
                        () -> {
                            CheckpointManager.CheckpointResult forced =
                                    plugin.getCheckpointManager().forceCreateCheckpoint(
                                            player, location, result.getCheckpoint());
                            if (forced.isSuccess()) {
                                reportSuccess(player, (int) x, (int) z);
                            } else {
                                MessageUtil.error(player, forced.getMessage());
                            }
                        },
                        result.getCheckpoint().getOwnerName()
                );
            } else {
                MessageUtil.error(player, result.getMessage());
            }
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "Invalid coordinates. Usage: /cp <x> <z>");
        }
    }

    private void reportSuccess(Player player, int x, int z) {
        MessageUtil.success(player, "Checkpoint created at X: " + x + ", Z: " + z);
        int count = plugin.getCheckpointManager().getCheckpointCount(player.getUniqueId());
        int limit = plugin.getConfigManager().getCheckpointLimit();
        MessageUtil.info(player, "Checkpoints: §e" + count + "/" + limit);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length <= 2) {
            return List.of("~");
        }
        return List.of();
    }
}