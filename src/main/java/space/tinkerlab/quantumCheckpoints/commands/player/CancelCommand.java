package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/cp cancel' and '/checkpoints cancel'.
 */
public class CancelCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public CancelCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "cancel";
    }

    @Override
    public @NotNull java.util.List<String> getAliases() {
        return java.util.List.of("no");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (plugin.getConfirmationManager().cancel(player)) {
            MessageUtil.info(player, "Action cancelled.");
        } else {
            MessageUtil.error(player, "Nothing to cancel.");
        }
    }
}