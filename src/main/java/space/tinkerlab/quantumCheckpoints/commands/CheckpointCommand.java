package space.tinkerlab.quantumCheckpoints.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.player.*;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin dispatcher for player checkpoint commands (/checkpoint, /cp).
 * Delegates all logic to registered {@link SubCommand} handlers.
 */
public class CheckpointCommand implements CommandExecutor, TabCompleter {

    private final QuantumCheckpoints plugin;
    private final CommandRegistry registry;
    private final SubCommand createHereCommand;

    /**
     * Creates a new CheckpointCommand dispatcher and registers all sub-commands.
     *
     * @param plugin the main plugin instance
     */
    public CheckpointCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.registry = new CommandRegistry();
        this.createHereCommand = new CreateHereCommand(plugin);

        registry.register(createHereCommand);
        registry.register(new CreateAtCoordsCommand(plugin));
        registry.register(new DeleteCommand(plugin));
        registry.register(new DeleteAtCoordsCommand(plugin));
        registry.register(new DeleteAllCommand(plugin));
        registry.register(new ListCommand(plugin));
        registry.register(new RestoreCommand(plugin));
        registry.register(new ConfirmCommand(plugin));
        registry.register(new CancelCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        // No args defaults to creating at current location
        if (args.length == 0) {
            createHereCommand.execute(player, new String[0]);
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand subCommand = registry.get(subName);

        if (subCommand != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            subCommand.execute(player, subArgs);
        } else {
            // If the first arg looks like a coordinate, try create-at-coords
            SubCommand coordCommand = registry.get("at");
            if (coordCommand != null) {
                coordCommand.execute(player, args);
            } else {
                MessageUtil.error(player, "Unknown sub-command. Use §e/cp help§c for usage.");
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            return filterCompletions(registry.getCommandNames(), args[0]);
        }

        String subName = args[0].toLowerCase();
        SubCommand subCommand = registry.get(subName);
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