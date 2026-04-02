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
 * Handles '/checkpoints penalty' — toggles the restoration penalty.
 */
public class PenaltyCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public PenaltyCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "penalty";
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
            boolean current = plugin.getConfigManager().isPenaltyEnabled();
            MessageUtil.info(sender, "Penalty is " + (current ? "§aenabled" : "§cdisabled") + "§7.");
            return;
        }

        boolean enabled = Boolean.parseBoolean(args[0]);
        plugin.getConfigManager().setPenaltyEnabled(enabled);
        MessageUtil.success(sender, "Penalty " + (enabled ? "enabled" : "disabled") + ".");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("true", "false").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}