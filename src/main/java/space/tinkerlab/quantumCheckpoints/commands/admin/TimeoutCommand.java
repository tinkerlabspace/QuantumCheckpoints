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
 * Handles '/checkpoints timeout' — sets the confirmation timeout duration.
 */
public class TimeoutCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public TimeoutCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "timeout";
    }

    @Override
    public boolean supportsConsole() {
        return true;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        execute((CommandSender) player, args);
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            long current = plugin.getConfigManager().getConfirmationTimeout();
            MessageUtil.info(sender, "Confirmation timeout: §e" + current + " seconds");
            MessageUtil.info(sender, "Usage: /checkpoints timeout <seconds>");
            return;
        }

        try {
            long seconds = Long.parseLong(args[0]);
            if (seconds < 5) {
                MessageUtil.error(sender, "Timeout must be at least 5 seconds.");
                return;
            }

            plugin.getConfigManager().setConfirmationTimeout(seconds);
            MessageUtil.success(sender, "Confirmation timeout set to " + seconds + " seconds.");
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid number: " + args[0]);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("10", "30", "60", "120").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}