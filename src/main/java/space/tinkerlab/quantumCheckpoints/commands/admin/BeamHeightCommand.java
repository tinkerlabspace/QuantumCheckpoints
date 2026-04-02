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
 * Handles '/checkpoints beamheight' — sets the visual beam height.
 */
public class BeamHeightCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public BeamHeightCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "beamheight";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("height");
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
            double current = plugin.getConfigManager().getBeamHeight();
            MessageUtil.info(sender, "Beam height: §e" + current + " blocks");
            MessageUtil.info(sender, "Usage: /checkpoints beamheight <height>");
            return;
        }

        try {
            double height = Double.parseDouble(args[0]);
            if (height < 1.0) {
                MessageUtil.error(sender, "Height must be at least 1.0 blocks.");
                return;
            }

            plugin.getConfigManager().setBeamHeight(height);

            // Recreate all beams with new height
            plugin.getBeamManager().recreateAllBeams();

            MessageUtil.success(sender, "Beam height set to " + height + " blocks. Beams updated.");
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid number: " + args[0]);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("3", "5", "7", "10").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}