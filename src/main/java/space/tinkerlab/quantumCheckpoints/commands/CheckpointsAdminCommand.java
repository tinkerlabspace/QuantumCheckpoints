package space.tinkerlab.quantumCheckpoints.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.admin.*;
import space.tinkerlab.quantumCheckpoints.commands.player.CancelCommand;
import space.tinkerlab.quantumCheckpoints.commands.player.ConfirmCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin dispatcher for admin checkpoint commands (/checkpoints).
 * Supports both player and console execution.
 */
public class CheckpointsAdminCommand implements CommandExecutor, TabCompleter {

    private final CommandRegistry registry;

    /**
     * Creates a new CheckpointsAdminCommand dispatcher and registers all sub-commands.
     *
     * @param plugin the main plugin instance
     */
    public CheckpointsAdminCommand(QuantumCheckpoints plugin) {
        this.registry = new CommandRegistry();

        // Core settings
        registry.register(new CostCommand(plugin));
        registry.register(new LimitCommand(plugin));
        registry.register(new PenaltyCommand(plugin));

        // Enable/Disable
        registry.register(new DisableCommand(plugin));
        registry.register(new EnableCommand(plugin));
        registry.register(new ClearCommand(plugin));

        // Proximity
        registry.register(new ProximityCommand(plugin));

        // Visual
        registry.register(new BeamHeightCommand(plugin));
        registry.register(new ViewDistanceCommand(plugin));

        // Confirmation
        registry.register(new TimeoutCommand(plugin));

        // Utility
        registry.register(new StatusCommand(plugin));
        registry.register(new ReloadCommand(plugin));
        registry.register(new ConfirmCommand(plugin));
        registry.register(new CancelCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("quantumcheckpoints.admin")) {
            MessageUtil.error(sender, "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = registry.get(args[0]);
        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        if (sender instanceof Player player) {
            subCommand.execute(player, subArgs);
        } else if (subCommand.supportsConsole()) {
            subCommand.execute(sender, subArgs);
        } else {
            sender.sendMessage("This sub-command can only be used by players.");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.info(sender, "§6═══ QuantumCheckpoints Admin ═══");
        MessageUtil.info(sender, "§e Core:");
        MessageUtil.info(sender, "  §f/checkpoints cost <item> <amount> §7- Set cost");
        MessageUtil.info(sender, "  §f/checkpoints limit <number> §7- Set limit");
        MessageUtil.info(sender, "  §f/checkpoints penalty <true/false> §7- Toggle penalty");
        MessageUtil.info(sender, "§e Control:");
        MessageUtil.info(sender, "  §f/checkpoints disable [true/false] §7- Disable");
        MessageUtil.info(sender, "  §f/checkpoints enable [true/false] §7- Enable");
        MessageUtil.info(sender, "  §f/checkpoints clear §7- Clear all");
        MessageUtil.info(sender, "§e Proximity:");
        MessageUtil.info(sender, "  §f/checkpoints proximity <radius> §7- Set radius");
        MessageUtil.info(sender, "§e Visual:");
        MessageUtil.info(sender, "  §f/checkpoints beamheight <height> §7- Set height");
        MessageUtil.info(sender, "  §f/checkpoints viewdistance <dist> §7- Set view dist");
        MessageUtil.info(sender, "§e Other:");
        MessageUtil.info(sender, "  §f/checkpoints timeout <seconds> §7- Set timeout");
        MessageUtil.info(sender, "  §f/checkpoints status §7- View settings");
        MessageUtil.info(sender, "  §f/checkpoints reload §7- Reload config");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("quantumcheckpoints.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterCompletions(registry.getCommandNames(), args[0]);
        }

        if (sender instanceof Player player) {
            SubCommand subCommand = registry.get(args[0]);
            if (subCommand != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(player, subArgs);
            }
        }

        return List.of();
    }

    private List<String> filterCompletions(List<String> options, String current) {
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList());
    }
}