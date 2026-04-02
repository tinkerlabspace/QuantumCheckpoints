package space.tinkerlab.quantumCheckpoints.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/checkpoints reload' — reloads configuration from disk.
 */
public class ReloadCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public ReloadCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "reload";
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
        // Clear pending confirmations as timeout values may have changed
        plugin.getConfirmationManager().clearAll();

        // Reload config
        plugin.getConfigManager().reload();

        // Recreate beams to reflect any visual changes
        plugin.getBeamManager().recreateAllBeams();

        MessageUtil.success(sender, "Configuration reloaded successfully.");
    }
}