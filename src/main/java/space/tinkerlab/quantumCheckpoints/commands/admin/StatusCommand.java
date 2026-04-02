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
        boolean enabled = plugin.getConfigManager().isCheckpointsEnabled();
        boolean penalty = plugin.getConfigManager().isPenaltyEnabled();
        int limit = plugin.getConfigManager().getCheckpointLimit();
        String cost = plugin.getConfigManager().getCostDescription();
        int total = plugin.getCheckpointManager().getAllCheckpoints().size();

        MessageUtil.info(sender, "§6QuantumCheckpoints Status:");
        MessageUtil.info(sender, " §7Enabled: " + (enabled ? "§aYes" : "§cNo"));
        MessageUtil.info(sender, " §7Penalty: " + (penalty ? "§aYes" : "§cNo"));
        MessageUtil.info(sender, " §7Limit: §e" + limit + " per player");
        MessageUtil.info(sender, " §7Cost: §e" + cost);
        MessageUtil.info(sender, " §7Total checkpoints: §e" + total);
    }
}