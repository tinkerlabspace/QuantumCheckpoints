package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.List;

/**
 * Handles '/cp delete all'.
 * Not directly registered in the registry — invoked by {@link DeleteCommand}.
 */
public class DeleteAllCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public DeleteAllCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "delete_all";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        List<Checkpoint> checkpoints = plugin.getCheckpointManager()
                .getCheckpointsForPlayer(player.getUniqueId());

        if (checkpoints.isEmpty()) {
            MessageUtil.error(player, "You don't have any checkpoints.");
            return;
        }

        int count = checkpoints.size();
        plugin.getConfirmationManager().requestConfirmation(
                player,
                () -> {
                    List<Checkpoint> current = plugin.getCheckpointManager()
                            .getCheckpointsForPlayer(player.getUniqueId());
                    for (Checkpoint cp : current) {
                        plugin.getCheckpointManager().removeCheckpoint(cp);
                    }
                    MessageUtil.success(player, "Deleted " + count + " checkpoint(s).");
                },
                "Delete all " + count + " of your checkpoints?"
        );
    }
}