package space.tinkerlab.quantumCheckpoints.commands.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.List;

/**
 * Handles '/cp list' — displays all checkpoints for the player.
 */
public class ListCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public ListCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "list";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("ls");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        List<Checkpoint> checkpoints = plugin.getCheckpointManager()
                .getCheckpointsForPlayer(player.getUniqueId());

        if (checkpoints.isEmpty()) {
            MessageUtil.info(player, "You don't have any checkpoints.");
            return;
        }

        int limit = plugin.getConfigManager().getCheckpointLimit();
        MessageUtil.info(player, "§6Your Checkpoints (" + checkpoints.size() + "/" + limit + "):");

        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint cp = checkpoints.get(i);
            Location loc = cp.getLocation();
            String status = cp.isEnabled() ? "§a✓" : "§c✗";
            MessageUtil.info(player, String.format(
                    " §7%d. %s §e%s §7@ X:%d Y:%d Z:%d §8[%s]",
                    i + 1, status, cp.getFormattedTime(),
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    loc.getWorld().getName()
            ));
        }
    }
}