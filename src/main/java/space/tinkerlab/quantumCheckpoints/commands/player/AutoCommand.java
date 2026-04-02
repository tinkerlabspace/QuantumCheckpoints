package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.auto.AutoCheckpointPreference;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles '/cp auto' — manages per-player auto-checkpoint settings.
 *
 * Sub-usage:
 *   /cp auto              — shows current auto-checkpoint status
 *   /cp auto on           — enables auto-checkpoints
 *   /cp auto off          — disables auto-checkpoints
 *   /cp auto interval <m> — sets custom interval in minutes
 *   /cp auto reset        — resets to server default interval
 */
public class AutoCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public AutoCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "auto";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 0) {
            showStatus(player);
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "on", "enable" -> handleEnable(player);
            case "off", "disable" -> handleDisable(player);
            case "interval", "time" -> handleInterval(player, args);
            case "reset", "default" -> handleReset(player);
            default -> {
                MessageUtil.error(player, "Usage: /cp auto [on|off|interval <minutes>|reset]");
            }
        }
    }

    /**
     * Shows the player's current auto-checkpoint status.
     */
    private void showStatus(Player player) {
        UUID playerId = player.getUniqueId();
        boolean serverEnabled = plugin.getConfigManager().isAutoCheckpointEnabled();
        AutoCheckpointPreference pref = plugin.getAutoCheckpointManager().getPreference(playerId);

        MessageUtil.info(player, "§6Auto-Checkpoint Status:");

        if (!serverEnabled) {
            MessageUtil.info(player, " §7Server-wide: §cDisabled");
            MessageUtil.info(player, " §8Auto-checkpoints are turned off by an admin.");
            return;
        }

        MessageUtil.info(player, " §7Your auto-checkpoint: " + (pref.isEnabled() ? "§aOn" : "§cOff"));

        int effectiveInterval = plugin.getAutoCheckpointManager().getEffectiveIntervalMinutes(playerId);
        int serverDefault = plugin.getConfigManager().getAutoCheckpointInterval();

        if (pref.hasCustomInterval()) {
            MessageUtil.info(player, " §7Interval: §f" + pref.getIntervalMinutes() +
                    " min §7(server default: " + serverDefault + " min)");
        } else {
            MessageUtil.info(player, " §7Interval: §f" + effectiveInterval + " min §7(server default)");
        }

        int minInterval = plugin.getConfigManager().getAutoCheckpointMinInterval();
        MessageUtil.info(player, " §7Min allowed interval: §f" + minInterval + " min");
    }

    /**
     * Enables auto-checkpoints for the player.
     */
    private void handleEnable(Player player) {
        if (!plugin.getConfigManager().isAutoCheckpointEnabled()) {
            MessageUtil.error(player, "Auto-checkpoints are disabled server-wide.");
            return;
        }

        plugin.getAutoCheckpointManager().setEnabled(player.getUniqueId(), true);
        int interval = plugin.getAutoCheckpointManager().getEffectiveIntervalMinutes(player.getUniqueId());
        MessageUtil.success(player, "Auto-checkpoints enabled. Interval: " + interval + " min.");
    }

    /**
     * Disables auto-checkpoints for the player.
     */
    private void handleDisable(Player player) {
        plugin.getAutoCheckpointManager().setEnabled(player.getUniqueId(), false);
        MessageUtil.success(player, "Auto-checkpoints disabled.");
    }

    /**
     * Sets a custom interval for the player.
     */
    private void handleInterval(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.error(player, "Usage: /cp auto interval <minutes>");
            return;
        }

        if (!plugin.getConfigManager().isAutoCheckpointEnabled()) {
            MessageUtil.error(player, "Auto-checkpoints are disabled server-wide.");
            return;
        }

        try {
            int minutes = Integer.parseInt(args[1]);
            int minInterval = plugin.getConfigManager().getAutoCheckpointMinInterval();

            if (minutes < minInterval) {
                MessageUtil.error(player, "Minimum interval is " + minInterval + " minute(s).");
                return;
            }

            boolean accepted = plugin.getAutoCheckpointManager().setPlayerInterval(
                    player.getUniqueId(), minutes);

            if (accepted) {
                MessageUtil.success(player, "Auto-checkpoint interval set to " + minutes + " minute(s).");
            } else {
                MessageUtil.error(player, "Minimum interval is " + minInterval + " minute(s).");
            }
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "Invalid number: " + args[1]);
        }
    }

    /**
     * Resets the player's interval to the server default.
     */
    private void handleReset(Player player) {
        plugin.getAutoCheckpointManager().clearPlayerInterval(player.getUniqueId());
        int serverDefault = plugin.getConfigManager().getAutoCheckpointInterval();
        MessageUtil.success(player, "Interval reset to server default (" + serverDefault + " min).");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "interval", "reset").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("interval") || args[0].equalsIgnoreCase("time"))) {
            int minInterval = plugin.getConfigManager().getAutoCheckpointMinInterval();
            return Arrays.asList(
                            String.valueOf(minInterval),
                            "5", "10", "15", "30"
                    ).stream()
                    .distinct()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}