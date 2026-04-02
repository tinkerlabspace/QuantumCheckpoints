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
 * Handles '/checkpoints limit' — sets the max checkpoints per player.
 */
public class LimitCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public LimitCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "limit";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 0) {
            MessageUtil.info(player, "Current limit: §e" +
                    plugin.getConfigManager().getCheckpointLimit() + " per player");
            return;
        }

        try {
            int limit = Integer.parseInt(args[0]);
            if (limit <= 0) {
                MessageUtil.error(player, "Limit must be greater than 0.");
                return;
            }
            plugin.getConfigManager().setCheckpointLimit(limit);
            MessageUtil.success(player, "Checkpoint limit set to " + limit + ".");
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "Invalid number: " + args[0]);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "3", "5", "10").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}