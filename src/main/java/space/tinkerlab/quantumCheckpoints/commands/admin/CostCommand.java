package space.tinkerlab.quantumCheckpoints.commands.admin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.commands.SubCommand;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles '/checkpoints cost' — sets the creation cost for checkpoints.
 */
public class CostCommand implements SubCommand {

    private final QuantumCheckpoints plugin;

    public CostCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "cost";
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 0) {
            MessageUtil.info(player, "Current cost: §e" + plugin.getConfigManager().getCostDescription());
            MessageUtil.info(player, "Usage: /checkpoints cost <item> <amount>");
            MessageUtil.info(player, "Use §e/checkpoints cost none§7 to make free.");
            return;
        }

        if (args[0].equalsIgnoreCase("none") || args[0].equalsIgnoreCase("free")) {
            plugin.getConfigManager().setCheckpointCost(null);
            MessageUtil.success(player, "Checkpoint creation is now free.");
            return;
        }

        if (args.length < 2) {
            MessageUtil.error(player, "Usage: /checkpoints cost <item> <amount>");
            return;
        }

        Material material = Material.matchMaterial(args[0]);
        if (material == null) {
            MessageUtil.error(player, "Unknown item: " + args[0]);
            return;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                MessageUtil.error(player, "Amount must be greater than 0.");
                return;
            }

            plugin.getConfigManager().setCheckpointCost(new ItemStack(material, amount));
            MessageUtil.success(player, "Cost set to " + amount + "x " +
                    material.name().toLowerCase().replace("_", " ") + ".");
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "Invalid amount: " + args[1]);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .map(m -> m.name().toLowerCase())
                    .limit(50)
                    .collect(Collectors.toList());
            options.add(0, "none");
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("1", "5", "10", "32", "64").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}