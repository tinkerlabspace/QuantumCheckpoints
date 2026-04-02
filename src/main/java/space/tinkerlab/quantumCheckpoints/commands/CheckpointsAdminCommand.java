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
 * Delegates all logic to registered {@link SubCommand} handlers.
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

        registry.register(new CostCommand(plugin));
        registry.register(new LimitCommand(plugin));
        registry.register(new DisableCommand(plugin));
        registry.register(new EnableCommand(plugin));
        registry.register(new ClearCommand(plugin));
        registry.register(new PenaltyCommand(plugin));
        registry.register(new StatusCommand(plugin));
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

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        SubCommand subCommand = registry.get(args[0]);
        if (subCommand != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            subCommand.execute(player, subArgs);
        } else {
            sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        MessageUtil.info(player, "§6QuantumCheckpoints Admin Commands:");
        MessageUtil.info(player, " §e/checkpoints cost <item> <amount> §7- Set cost");
        MessageUtil.info(player, " §e/checkpoints limit <number> §7- Set limit");
        MessageUtil.info(player, " §e/checkpoints disable [true/false] §7- Disable");
        MessageUtil.info(player, " §e/checkpoints enable [true/false] §7- Enable");
        MessageUtil.info(player, " §e/checkpoints clear §7- Clear all");
        MessageUtil.info(player, " §e/checkpoints penalty <true/false> §7- Toggle penalty");
        MessageUtil.info(player, " §e/checkpoints status §7- View settings");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("quantumcheckpoints.admin") || !(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            return filterCompletions(registry.getCommandNames(), args[0]);
        }

        SubCommand subCommand = registry.get(args[0]);
        if (subCommand != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(player, subArgs);
        }

        return List.of();
    }

    private List<String> filterCompletions(List<String> options, String current) {
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList());
    }
}