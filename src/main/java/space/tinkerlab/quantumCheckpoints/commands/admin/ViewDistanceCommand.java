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
 * Handles '/checkpoints viewdistance' — sets the particle view distance.
 */
public class ViewDistanceCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public ViewDistanceCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "viewdistance";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("particledistance", "vd");
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
            double current = plugin.getConfigManager().getParticleViewDistance();
            MessageUtil.info(sender, "Particle view distance: §e" + current + " blocks");
            MessageUtil.info(sender, "Usage: /checkpoints viewdistance <distance>");
            return;
        }

        try {
            double distance = Double.parseDouble(args[0]);
            if (distance < 10.0) {
                MessageUtil.error(sender, "Distance must be at least 10 blocks.");
                return;
            }

            plugin.getConfigManager().setParticleViewDistance(distance);
            MessageUtil.success(sender, "Particle view distance set to " + distance + " blocks.");
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid number: " + args[0]);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("50", "100", "150", "200").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}