package space.tinkerlab.quantumCheckpoints.commands.admin;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles '/checkpoints enable [true/false]'.
 * true (default): enables and restores previous checkpoints.
 * false: clears all checkpoints and enables fresh.
 */
public class EnableCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public EnableCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "enable";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        boolean restoreExisting = args.length == 0 || Boolean.parseBoolean(args[0]);

        if (restoreExisting) {
            plugin.getConfigManager().setCheckpointsEnabled(true);
            plugin.getCheckpointManager().setAllCheckpointsEnabled(true);
            MessageUtil.success(player, "Checkpoints enabled. Existing checkpoints restored.");
        } else {
            // Destructive — requires confirmation
            plugin.getConfirmationManager().requestConfirmation(player, () -> {
                plugin.getCheckpointManager().clearAllCheckpoints();
                plugin.getConfigManager().setCheckpointsEnabled(true);
                MessageUtil.success(player, "All checkpoints cleared. System enabled fresh.");
            }, "Clear ALL checkpoints and enable fresh?");
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("true", "false").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}