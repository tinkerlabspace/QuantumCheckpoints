package space.tinkerlab.quantumCheckpoints.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/checkpoints status' — displays current configuration.
 */
public class StatusCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public StatusCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "status";
    }

    @Override
    public @NotNull java.util.List<String> getAliases() {
        return java.util.List.of("info");
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
        var config = plugin.getConfigManager();

        MessageUtil.info(sender, "§6═══ QuantumCheckpoints Status ═══");

        // Core
        MessageUtil.info(sender, "§e Core Settings:");
        MessageUtil.info(sender, "  §7Enabled: " + formatBoolean(config.isCheckpointsEnabled()));
        MessageUtil.info(sender, "  §7Checkpoint Limit: §f" + config.getCheckpointLimit() + " per player");
        MessageUtil.info(sender, "  §7Creation Cost: §f" + config.getCostDescription());

        // Restoration
        MessageUtil.info(sender, "§e Restoration:");
        MessageUtil.info(sender, "  §7Penalty Enabled: " + formatBoolean(config.isPenaltyEnabled()));

        // Proximity
        MessageUtil.info(sender, "§e Proximity:");
        MessageUtil.info(sender, "  §7Radius: §f" + config.getProximityRadius() + " blocks");

        // Visual
        MessageUtil.info(sender, "§e Visual:");
        MessageUtil.info(sender, "  §7Beam Height: §f" + config.getBeamHeight() + " blocks");
        MessageUtil.info(sender, "  §7View Distance: §f" + config.getParticleViewDistance() + " blocks");

        // Confirmation
        MessageUtil.info(sender, "§e Confirmation:");
        MessageUtil.info(sender, "  §7Timeout: §f" + config.getConfirmationTimeout() + " seconds");

        // Statistics
        MessageUtil.info(sender, "§e Statistics:");
        MessageUtil.info(sender, "  §7Total Checkpoints: §f" + plugin.getCheckpointManager().getAllCheckpoints().size());
    }

    private String formatBoolean(boolean value) {
        return value ? "§aYes" : "§cNo";
    }
}