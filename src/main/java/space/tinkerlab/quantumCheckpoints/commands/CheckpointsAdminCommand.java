// src/main/java/space/tinkerlab/quantumCheckpoints/commands/CheckpointsAdminCommand.java
package space.tinkerlab.quantumCheckpoints.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles admin checkpoint commands (/checkpoints).
 */
public class CheckpointsAdminCommand implements CommandExecutor, TabCompleter {

    private final QuantumCheckpoints plugin;

    /**
     * Creates a new CheckpointsAdminCommand handler.
     *
     * @param plugin the main plugin instance
     */
    public CheckpointsAdminCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("quantumcheckpoints.admin")) {
            MessageUtil.error(sender, "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "cost" -> handleCost(sender, args);
            case "limit" -> handleLimit(sender, args);
            case "disable" -> handleDisable(sender, args);
            case "enable" -> handleEnable(sender, args);
            case "clear" -> handleClear(sender);
            case "penalty" -> handlePenalty(sender, args);
            case "status" -> handleStatus(sender);
            case "confirm" -> handleConfirm(sender);
            case "cancel" -> handleCancel(sender);
            default -> {
                sendHelpMessage(sender);
                yield true;
            }
        };
    }

    /**
     * Sends the help message listing all admin commands.
     */
    private void sendHelpMessage(CommandSender sender) {
        MessageUtil.info(sender, "§6QuantumCheckpoints Admin Commands:");
        MessageUtil.info(sender, "§e/checkpoints cost <item> <amount> §7- Set creation cost");
        MessageUtil.info(sender, "§e/checkpoints limit <number> §7- Set checkpoint limit per player");
        MessageUtil.info(sender, "§e/checkpoints disable <true/false> §7- Disable checkpoints");
        MessageUtil.info(sender, "§e/checkpoints enable <true/false> §7- Enable checkpoints");
        MessageUtil.info(sender, "§e/checkpoints clear §7- Clear all checkpoints");
        MessageUtil.info(sender, "§e/checkpoints penalty <true/false> §7- Toggle restoration penalty");
        MessageUtil.info(sender, "§e/checkpoints status §7- View current settings");
    }

    /**
     * Handles the cost subcommand.
     * Supports 'none'/'free' as single argument to remove cost,
     * or '<item> <amount>' to set a specific cost.
     */
    private boolean handleCost(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String currentCost = plugin.getConfigManager().getCostDescription();
            MessageUtil.info(sender, "Current cost: §e" + currentCost);
            MessageUtil.info(sender, "Usage: /checkpoints cost <item> <amount>");
            MessageUtil.info(sender, "Use '/checkpoints cost none' to remove cost");
            return true;
        }

        // Handle 'none' and 'free' as special single-argument cases first
        if (args[1].equalsIgnoreCase("none") || args[1].equalsIgnoreCase("free")) {
            plugin.getConfigManager().setCheckpointCost(null);
            MessageUtil.success(sender, "Checkpoint creation is now free.");
            return true;
        }

        // Setting a material cost requires both material and amount
        if (args.length < 3) {
            MessageUtil.error(sender, "Usage: /checkpoints cost <item> <amount>");
            return true;
        }

        Material material = Material.matchMaterial(args[1]);
        if (material == null) {
            MessageUtil.error(sender, "Unknown item: " + args[1]);
            return true;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                MessageUtil.error(sender, "Amount must be greater than 0.");
                return true;
            }

            plugin.getConfigManager().setCheckpointCost(new ItemStack(material, amount));
            MessageUtil.success(sender, "Checkpoint cost set to " + amount + "x " +
                    material.name().toLowerCase().replace("_", " "));
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid amount: " + args[2]);
        }

        return true;
    }

    /**
     * Handles the limit subcommand.
     */
    private boolean handleLimit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            int currentLimit = plugin.getConfigManager().getCheckpointLimit();
            MessageUtil.info(sender, "Current limit: §e" + currentLimit + " checkpoints per player");
            MessageUtil.info(sender, "Usage: /checkpoints limit <number>");
            return true;
        }

        try {
            int limit = Integer.parseInt(args[1]);
            if (limit <= 0) {
                MessageUtil.error(sender, "Limit must be greater than 0.");
                return true;
            }

            plugin.getConfigManager().setCheckpointLimit(limit);
            MessageUtil.success(sender, "Checkpoint limit set to " + limit + " per player.");
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "Invalid number: " + args[1]);
        }

        return true;
    }

    /**
     * Handles the disable subcommand.
     */
    private boolean handleDisable(CommandSender sender, String[] args) {
        boolean disableExisting = true; // Default behavior
        if (args.length >= 2) {
            disableExisting = Boolean.parseBoolean(args[1]);
        }

        if (!(sender instanceof Player player)) {
            executeDisable(sender, disableExisting);
            return true;
        }

        // Require confirmation for destructive action
        final boolean finalDisableExisting = disableExisting;
        plugin.getConfirmationManager().requestConfirmation(
                player,
                () -> executeDisable(player, finalDisableExisting),
                disableExisting ?
                        "This will disable all existing checkpoints and prevent new ones." :
                        "This will prevent creation of new checkpoints (existing ones remain active)."
        );

        return true;
    }

    /**
     * Executes the disable action.
     */
    private void executeDisable(CommandSender sender, boolean disableExisting) {
        plugin.getConfigManager().setCheckpointsEnabled(false);

        if (disableExisting) {
            plugin.getCheckpointManager().setAllCheckpointsEnabled(false);
            MessageUtil.success(sender, "Checkpoints disabled. All existing checkpoints are now inactive.");
        } else {
            MessageUtil.success(sender, "Checkpoint creation disabled. Existing checkpoints remain active.");
        }
    }

    /**
     * Handles the enable subcommand.
     */
    private boolean handleEnable(CommandSender sender, String[] args) {
        boolean restoreExisting = true; // Default behavior
        if (args.length >= 2) {
            restoreExisting = Boolean.parseBoolean(args[1]);
        }

        if (!restoreExisting && sender instanceof Player player) {
            // Require confirmation for reset
            plugin.getConfirmationManager().requestConfirmation(
                    player,
                    () -> {
                        plugin.getCheckpointManager().clearAllCheckpoints();
                        plugin.getConfigManager().setCheckpointsEnabled(true);
                        MessageUtil.success(player, "All checkpoints cleared. Checkpoints are now enabled.");
                    },
                    "This will DELETE all existing checkpoints and enable the system fresh."
            );
            return true;
        }

        plugin.getConfigManager().setCheckpointsEnabled(true);

        if (restoreExisting) {
            plugin.getCheckpointManager().setAllCheckpointsEnabled(true);
            MessageUtil.success(sender, "Checkpoints enabled. All existing checkpoints are now active.");
        } else {
            plugin.getCheckpointManager().clearAllCheckpoints();
            MessageUtil.success(sender, "Checkpoints enabled with a fresh start.");
        }

        return true;
    }

    /**
     * Handles the clear subcommand.
     */
    private boolean handleClear(CommandSender sender) {
        int count = plugin.getCheckpointManager().getAllCheckpoints().size();

        if (count == 0) {
            MessageUtil.info(sender, "There are no checkpoints to clear.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.getCheckpointManager().clearAllCheckpoints();
            plugin.getDataManager().deleteAll();
            MessageUtil.success(sender, "Cleared " + count + " checkpoint(s).");
            return true;
        }

        plugin.getConfirmationManager().requestConfirmation(
                player,
                () -> {
                    plugin.getCheckpointManager().clearAllCheckpoints();
                    plugin.getDataManager().deleteAll();
                    MessageUtil.success(player, "Cleared " + count + " checkpoint(s).");
                },
                "This will permanently delete ALL " + count + " checkpoints on the server."
        );

        return true;
    }

    /**
     * Handles the penalty subcommand.
     */
    private boolean handlePenalty(CommandSender sender, String[] args) {
        if (args.length < 2) {
            boolean current = plugin.getConfigManager().isPenaltyEnabled();
            MessageUtil.info(sender, "Restoration penalty is currently: §e" + (current ? "enabled" : "disabled"));
            MessageUtil.info(sender, "Usage: /checkpoints penalty <true/false>");
            return true;
        }

        boolean enabled = Boolean.parseBoolean(args[1]);
        plugin.getConfigManager().setPenaltyEnabled(enabled);
        MessageUtil.success(sender, "Restoration penalty " + (enabled ? "enabled" : "disabled") + ".");

        return true;
    }

    /**
     * Handles the status subcommand.
     */
    private boolean handleStatus(CommandSender sender) {
        MessageUtil.info(sender, "§6QuantumCheckpoints Status:");
        MessageUtil.info(sender, "§7Checkpoints Enabled: §e" +
                (plugin.getConfigManager().isCheckpointsEnabled() ? "Yes" : "No"));
        MessageUtil.info(sender, "§7Penalty Enabled: §e" +
                (plugin.getConfigManager().isPenaltyEnabled() ? "Yes" : "No"));
        MessageUtil.info(sender, "§7Checkpoint Limit: §e" +
                plugin.getConfigManager().getCheckpointLimit() + " per player");
        MessageUtil.info(sender, "§7Creation Cost: §e" +
                plugin.getConfigManager().getCostDescription());
        MessageUtil.info(sender, "§7Total Checkpoints: §e" +
                plugin.getCheckpointManager().getAllCheckpoints().size());
        return true;
    }

    /**
     * Handles the confirm subcommand.
     */
    private boolean handleConfirm(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (plugin.getConfirmationManager().confirm(player)) {
            return true;
        }
        MessageUtil.error(sender, "You don't have any pending actions to confirm.");
        return true;
    }

    /**
     * Handles the cancel subcommand.
     */
    private boolean handleCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (plugin.getConfirmationManager().cancel(player)) {
            MessageUtil.info(sender, "Action cancelled.");
            return true;
        }
        MessageUtil.error(sender, "You don't have any pending actions to cancel.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("quantumcheckpoints.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterCompletions(
                    Arrays.asList("cost", "limit", "disable", "enable", "clear", "penalty", "status", "confirm", "cancel"),
                    args[0]
            );
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "cost" -> {
                    List<String> materials = Arrays.stream(Material.values())
                            .filter(Material::isItem)
                            .map(m -> m.name().toLowerCase())
                            .limit(50)
                            .collect(Collectors.toList());
                    materials.add(0, "none");
                    yield filterCompletions(materials, args[1]);
                }
                case "limit" -> filterCompletions(Arrays.asList("1", "3", "5", "10"), args[1]);
                case "disable", "enable", "penalty" -> filterCompletions(Arrays.asList("true", "false"), args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("cost")) {
            return filterCompletions(Arrays.asList("1", "5", "10", "32", "64"), args[2]);
        }

        return List.of();
    }

    /**
     * Filters tab completions based on current input.
     */
    private List<String> filterCompletions(List<String> options, String current) {
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList());
    }
}