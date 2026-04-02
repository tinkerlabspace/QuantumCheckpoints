package space.tinkerlab.quantumCheckpoints.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/checkpoints clear' — removes all checkpoints server-wide.
 * Console executes immediately without confirmation.
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
    public boolean supportsConsole() {
        return true;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        int count = plugin.getCheckpointManager().getAllCheckpoints().size();
        if (count == 0) {
            MessageUtil.info(player, "No checkpoints to clear.");
            return;
        }

        plugin.getConfirmationManager().requestConfirmation(player, () -> {
            executeClear(player);
        }, "Permanently delete ALL " + count + " checkpoints?");
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            execute(player, args);
            return;
        }
        executeClear(sender);
    }

    private void executeClear(CommandSender sender) {
        int count = plugin.getCheckpointManager().getAllCheckpoints().size();
        plugin.getCheckpointManager().clearAllCheckpoints();
        plugin.getDataManager().deleteAll();
        MessageUtil.success(sender, "Cleared " + count + " checkpoint(s).");
    }
}