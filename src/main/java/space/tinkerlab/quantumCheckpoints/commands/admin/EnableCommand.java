package space.tinkerlab.quantumCheckpoints.commands.admin;

import org.bukkit.command.CommandSender;
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
 * Console executes immediately without confirmation.
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
    public boolean supportsConsole() {
        return true;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        boolean restoreExisting = args.length == 0 || Boolean.parseBoolean(args[0]);

        if (restoreExisting) {
            executeEnable(player, true);
        } else {
            plugin.getConfirmationManager().requestConfirmation(player, () -> {
                executeEnable(player, false);
            }, "Clear ALL checkpoints and enable fresh?");
        }
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            execute(player, args);
            return;
        }
        boolean restoreExisting = args.length == 0 || Boolean.parseBoolean(args[0]);
        executeEnable(sender, restoreExisting);
    }

    private void executeEnable(CommandSender sender, boolean restoreExisting) {
        if (restoreExisting) {
            plugin.getConfigManager().setCheckpointsEnabled(true);
            plugin.getCheckpointManager().setAllCheckpointsEnabled(true);
            MessageUtil.success(sender, "Checkpoints enabled. Existing checkpoints restored.");
        } else {
            plugin.getCheckpointManager().clearAllCheckpoints();
            plugin.getConfigManager().setCheckpointsEnabled(true);
            MessageUtil.success(sender, "All checkpoints cleared. System enabled fresh.");
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