package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.CoordinateUtil;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/cp delete <x> <z>'.
 * Not directly registered in the registry — invoked by {@link DeleteCommand}.
 */
public class DeleteAtCoordsCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public DeleteAtCoordsCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "delete_at";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            MessageUtil.error(player, "Usage: /cp delete <x> <z>");
            return;
        }

        try {
            double x = CoordinateUtil.parse(args[0], player.getLocation().getX());
            double z = CoordinateUtil.parse(args[1], player.getLocation().getZ());

            Checkpoint checkpoint = findCheckpointAtXZ(player, (int) x, (int) z);
            if (checkpoint == null) {
                MessageUtil.error(player, "No checkpoint found at X: " + (int) x + ", Z: " + (int) z);
                return;
            }

            int ix = (int) x;
            int iz = (int) z;
            plugin.getConfirmationManager().requestConfirmation(
                    player,
                    () -> {
                        plugin.getCheckpointManager().removeCheckpoint(checkpoint);
                        MessageUtil.success(player, "Checkpoint at X: " + ix + ", Z: " + iz + " deleted.");
                    },
                    "Delete checkpoint at X:" + ix + " Z:" + iz + "?"
            );
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "Invalid coordinates.");
        }
    }

    private Checkpoint findCheckpointAtXZ(Player player, int x, int z) {
        return plugin.getCheckpointManager().getCheckpointsForPlayer(player.getUniqueId())
                .stream()
                .filter(cp -> cp.getLocation().getBlockX() == x && cp.getLocation().getBlockZ() == z)
                .findFirst()
                .orElse(null);
    }
}