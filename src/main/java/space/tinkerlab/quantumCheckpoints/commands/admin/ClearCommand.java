package space.tinkerlab.quantumCheckpoints.commands.admin;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/checkpoints clear' — removes all checkpoints server-wide.
 */
public class ClearCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public ClearCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "clear";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        int count = plugin.getCheckpointManager().getAllCheckpoints().size();

        if (count == 0) {
            MessageUtil.info(player, "No checkpoints to clear.");
            return;
        }

        plugin.getConfirmationManager().requestConfirmation(player, () -> {
            plugin.getCheckpointManager().clearAllCheckpoints();
            plugin.getDataManager().deleteAll();
            MessageUtil.success(player, "Cleared " + count + " checkpoint(s).");
        }, "Permanently delete ALL " + count + " checkpoints?");
    }
}