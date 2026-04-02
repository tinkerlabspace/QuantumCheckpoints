package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

/**
 * Handles '/cp confirm' and '/checkpoints confirm'.
 */
public class ConfirmCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public ConfirmCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "confirm";
    }

    @Override
    public @NotNull java.util.List<String> getAliases() {
        return java.util.List.of("yes");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (!plugin.getConfirmationManager().confirm(player)) {
            MessageUtil.error(player, "Nothing to confirm.");
        }
    }
}