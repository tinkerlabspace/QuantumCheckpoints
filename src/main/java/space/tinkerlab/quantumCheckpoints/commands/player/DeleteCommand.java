package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.LocationUtil;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles '/cp delete' (at current location).
 * Delegates to {@link DeleteAtCoordsCommand} or {@link DeleteAllCommand}
 * via the dispatcher if arguments are present.
 */
public class DeleteCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public DeleteCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "delete";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("del", "remove");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        // Route to sub-handlers if arguments present
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            new DeleteAllCommand(plugin).execute(player, new String[0]);
            return;
        }

        if (args.length >= 2) {
            new DeleteAtCoordsCommand(plugin).execute(player, args);
            return;
        }

        // Delete at current location
        Checkpoint checkpoint = LocationUtil.findNearestOwnedCheckpoint(plugin, player);

        if (checkpoint == null) {
            MessageUtil.error(player, "No checkpoint found at your location.");
            return;
        }

        Location loc = checkpoint.getLocation();
        plugin.getConfirmationManager().requestConfirmation(
                player,
                () -> {
                    plugin.getCheckpointManager().removeCheckpoint(checkpoint);
                    MessageUtil.success(player, "Checkpoint deleted.");
                },
                "Delete checkpoint at X:" + loc.getBlockX() +
                        " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ() + "?"
        );
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("all", "~").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("~");
        }
        return List.of();
    }
}