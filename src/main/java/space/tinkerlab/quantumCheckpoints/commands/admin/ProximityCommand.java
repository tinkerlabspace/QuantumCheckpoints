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
 * Handles '/checkpoints proximity' — sets the proximity radius.
 */
public class ProximityCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public ProximityCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "proximity";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("radius");
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
            double current = plugin.getConfigManager().getProximityRadius();
            MessageUtil.info(sender, "Proximity radius: §e" + current + " blocks");
            MessageUtil.info(sender, "Usage: /checkpoints proximity <radius>");
            return;
        }

        try {
            double radius = Double.parseDouble(args[0]);
            if (radius < 1.0) {
                MessageUtil.error(sender, "Radius must be at least 1.0 blocks.");
                return;
            }

            plugin.getConfigManager().setProximityRadius(radius);
            MessageUtil.success(sender, "Proximity radius set to " + radius + " blocks.");
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid number: " + args[0]);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "2", "3", "5", "10").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}