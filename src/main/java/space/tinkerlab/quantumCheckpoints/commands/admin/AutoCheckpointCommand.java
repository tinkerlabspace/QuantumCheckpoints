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
 * Handles '/checkpoints auto' — manages server-wide auto-checkpoint settings.
 *
 * Sub-usage:
 *   /checkpoints auto                     — shows current settings
 *   /checkpoints auto on|off              — enables/disables server-wide
 *   /checkpoints auto interval <minutes>  — sets default interval
 *   /checkpoints auto mininterval <mins>  — sets minimum player interval
 */
public class AutoCheckpointCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public AutoCheckpointCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "auto";
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
            showStatus(sender);
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "on", "enable" -> handleEnable(sender);
            case "off", "disable" -> handleDisable(sender);
            case "interval", "time" -> handleInterval(sender, args);
            case "mininterval", "min" -> handleMinInterval(sender, args);
            default -> {
                MessageUtil.error(sender, "Usage: /checkpoints auto [on|off|interval <min>|mininterval <min>]");
            }
        }
    }

    private void showStatus(CommandSender sender) {
        var config = plugin.getConfigManager();

        MessageUtil.info(sender, "§6Auto-Checkpoint Server Settings:");
        MessageUtil.info(sender, " §7Enabled: " + (config.isAutoCheckpointEnabled() ? "§aYes" : "§cNo"));
        MessageUtil.info(sender, " §7Default Interval: §f" + config.getAutoCheckpointInterval() + " min");
        MessageUtil.info(sender, " §7Minimum Player Interval: §f" + config.getAutoCheckpointMinInterval() + " min");
    }

    private void handleEnable(CommandSender sender) {
        plugin.getConfigManager().setAutoCheckpointEnabled(true);
        plugin.getAutoCheckpointManager().restart();
        MessageUtil.success(sender, "Auto-checkpoints enabled server-wide.");
    }

    private void handleDisable(CommandSender sender) {
        plugin.getConfigManager().setAutoCheckpointEnabled(false);
        MessageUtil.success(sender, "Auto-checkpoints disabled server-wide.");
    }

    private void handleInterval(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.info(sender, "Current interval: §e" +
                    plugin.getConfigManager().getAutoCheckpointInterval() + " min");
            MessageUtil.info(sender, "Usage: /checkpoints auto interval <minutes>");
            return;
        }

        try {
            int minutes = Integer.parseInt(args[1]);
            int minInterval = plugin.getConfigManager().getAutoCheckpointMinInterval();

            if (minutes < minInterval) {
                MessageUtil.error(sender, "Interval must be at least " + minInterval + " minute(s).");
                return;
            }

            plugin.getConfigManager().setAutoCheckpointInterval(minutes);
            MessageUtil.success(sender, "Default auto-checkpoint interval set to " + minutes + " minute(s).");
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid number: " + args[1]);
        }
    }

    private void handleMinInterval(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.info(sender, "Current minimum interval: §e" +
                    plugin.getConfigManager().getAutoCheckpointMinInterval() + " min");
            MessageUtil.info(sender, "Usage: /checkpoints auto mininterval <minutes>");
            return;
        }

        try {
            int minutes = Integer.parseInt(args[1]);
            if (minutes < 1) {
                MessageUtil.error(sender, "Minimum interval must be at least 1 minute.");
                return;
            }

            plugin.getConfigManager().setAutoCheckpointMinInterval(minutes);
            MessageUtil.success(sender, "Minimum player interval set to " + minutes + " minute(s).");

            // Warn if default is now below the new minimum
            if (plugin.getConfigManager().getAutoCheckpointInterval() < minutes) {
                MessageUtil.warn(sender, "Default interval was adjusted up to " + minutes + " min.");
            }
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid number: " + args[1]);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "interval", "mininterval").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("interval") ||
                args[0].equalsIgnoreCase("mininterval") ||
                args[0].equalsIgnoreCase("time") ||
                args[0].equalsIgnoreCase("min"))) {
            return Arrays.asList("1", "5", "10", "15", "30").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}