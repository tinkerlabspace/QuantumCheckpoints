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
 * Handles '/checkpoints disable [true/false]'.
 * true (default): disables all existing checkpoints.
 * false: only prevents creation of new ones.
 */
public class DisableCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public DisableCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "disable";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        boolean disableExisting = args.length == 0 || Boolean.parseBoolean(args[0]);

        String desc = disableExisting
                ? "Disable all existing checkpoints and prevent new ones?"
                : "Prevent new checkpoints? (existing ones stay active)";

        plugin.getConfirmationManager().requestConfirmation(player, () -> {
            plugin.getConfigManager().setCheckpointsEnabled(false);
            if (disableExisting) {
                plugin.getCheckpointManager().setAllCheckpointsEnabled(false);
                MessageUtil.success(player, "All checkpoints disabled.");
            } else {
                MessageUtil.success(player, "New checkpoint creation disabled.");
            }
        }, desc);
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