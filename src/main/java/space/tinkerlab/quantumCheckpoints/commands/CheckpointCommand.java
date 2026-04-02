package space.tinkerlab.quantumCheckpoints.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;
import space.tinkerlab.quantumCheckpoints.checkpoint.CheckpointManager;
import space.tinkerlab.quantumCheckpoints.util.MessageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles player checkpoint commands (/checkpoint or /cp).
 */
public class CheckpointCommand implements CommandExecutor, TabCompleter {

    private final QuantumCheckpoints plugin;

    /**
     * Creates a new CheckpointCommand handler.
     *
     * @param plugin the main plugin instance
     */
    public CheckpointCommand(QuantumCheckpoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("here")) {
            return handleCreateHere(player);
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            case "restore" -> handleRestore(player);
            case "confirm" -> handleConfirm(player);
            case "cancel" -> handleCancel(player);
            default -> handleCreateAtCoords(player, args);
        };
    }

    /**
     * Handles checkpoint creation at the player's current location.
     */
    private boolean handleCreateHere(Player player) {
        Location location = player.getLocation();
        CheckpointManager.CheckpointResult result =
                plugin.getCheckpointManager().createCheckpoint(player, location);

        if (result.isSuccess()) {
            MessageUtil.success(player, "Checkpoint created at your location!");
            int count = plugin.getCheckpointManager().getCheckpointCount(player.getUniqueId());
            int limit = plugin.getConfigManager().getCheckpointLimit();
            MessageUtil.info(player, "Checkpoints: " + count + "/" + limit);
        } else {
            MessageUtil.error(player, result.getMessage());
        }

        return true;
    }

    /**
     * Handles checkpoint creation at specified coordinates.
     */
    private boolean handleCreateAtCoords(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.error(player, "Usage: /checkpoint <x> <z>");
            return true;
        }

        try {
            double x = parseCoordinate(args[0], player.getLocation().getX());
            double z = parseCoordinate(args[1], player.getLocation().getZ());

            Location location = new Location(
                    player.getWorld(),
                    x,
                    player.getWorld().getHighestBlockYAt((int) x, (int) z) + 1,
                    z
            );

            CheckpointManager.CheckpointResult result =
                    plugin.getCheckpointManager().createCheckpoint(player, location);

            if (result.isSuccess()) {
                MessageUtil.success(player, "Checkpoint created at X: " + (int) x + ", Z: " + (int) z);
                int count = plugin.getCheckpointManager().getCheckpointCount(player.getUniqueId());
                int limit = plugin.getConfigManager().getCheckpointLimit();
                MessageUtil.info(player, "Checkpoints: " + count + "/" + limit);
            } else {
                MessageUtil.error(player, result.getMessage());
            }
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "Invalid coordinates. Usage: /checkpoint <x> <z>");
        }

        return true;
    }

    /**
     * Parses a coordinate value, supporting relative coordinates with ~.
     */
    private double parseCoordinate(String input, double playerCoord) {
        if (input.startsWith("~")) {
            if (input.length() == 1) {
                return playerCoord;
            }
            return playerCoord + Double.parseDouble(input.substring(1));
        }
        return Double.parseDouble(input);
    }

    /**
     * Handles checkpoint deletion commands.
     */
    private boolean handleDelete(Player player, String[] args) {
        if (args.length == 1) {
            return handleDeleteHere(player);
        }

        if (args[1].equalsIgnoreCase("all")) {
            return handleDeleteAll(player);
        }

        if (args.length >= 3) {
            return handleDeleteAtCoords(player, args[1], args[2]);
        }

        MessageUtil.error(player, "Usage: /checkpoint delete [all|<x> <z>]");
        return true;
    }

    /**
     * Handles deletion of a checkpoint at the player's current location.
     * Requires confirmation since this is a destructive action.
     */
    private boolean handleDeleteHere(Player player) {
        Checkpoint checkpoint = findCheckpointNearPlayer(player);

        if (checkpoint == null) {
            MessageUtil.error(player, "There is no checkpoint at your location.");
            return true;
        }

        if (!checkpoint.getOwnerId().equals(player.getUniqueId())) {
            MessageUtil.error(player, "This checkpoint belongs to another player.");
            return true;
        }

        Location loc = checkpoint.getLocation();
        plugin.getConfirmationManager().requestConfirmation(
                player,
                () -> {
                    plugin.getCheckpointManager().removeCheckpoint(checkpoint);
                    MessageUtil.success(player, "Checkpoint deleted.");
                },
                "Delete checkpoint at X: " + loc.getBlockX() +
                        ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ() + "?"
        );

        return true;
    }

    /**
     * Handles deletion of a checkpoint at specified coordinates.
     * Requires confirmation since this is a destructive action.
     */
    private boolean handleDeleteAtCoords(Player player, String xStr, String zStr) {
        try {
            double x = parseCoordinate(xStr, player.getLocation().getX());
            double z = parseCoordinate(zStr, player.getLocation().getZ());

            Checkpoint checkpoint = findCheckpointAtXZ(player, (int) x, (int) z);

            if (checkpoint == null) {
                MessageUtil.error(player, "You don't have a checkpoint at X: " + (int) x + ", Z: " + (int) z);
                return true;
            }

            plugin.getConfirmationManager().requestConfirmation(
                    player,
                    () -> {
                        plugin.getCheckpointManager().removeCheckpoint(checkpoint);
                        MessageUtil.success(player, "Checkpoint at X: " + (int) x + ", Z: " + (int) z + " deleted.");
                    },
                    "Delete checkpoint at X: " + (int) x + ", Z: " + (int) z + "?"
            );
        } catch (NumberFormatException e) {
            MessageUtil.error(player, "Invalid coordinates.");
        }

        return true;
    }

    /**
     * Finds a checkpoint at the specified X/Z coordinates owned by the player.
     */
    private Checkpoint findCheckpointAtXZ(Player player, int x, int z) {
        return plugin.getCheckpointManager().getCheckpointsForPlayer(player.getUniqueId())
                .stream()
                .filter(cp -> cp.getLocation().getBlockX() == x && cp.getLocation().getBlockZ() == z)
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a checkpoint near the player's current position.
     * Uses a proximity search to account for slight positional differences
     * between where the player stands and the checkpoint's stored location.
     *
     * @param player the player to search around
     * @return the nearest owned checkpoint, or null if none found
     */
    private Checkpoint findCheckpointNearPlayer(Player player) {
        Location playerLoc = player.getLocation();

        // First try exact block match
        Checkpoint exact = plugin.getCheckpointManager().getCheckpointAt(playerLoc);
        if (exact != null) {
            return exact;
        }

        // Search the player's own checkpoints within a reasonable radius
        // This handles the drift between player position and stored checkpoint location
        double searchRadius = 3.0;
        return plugin.getCheckpointManager().getCheckpointsForPlayer(player.getUniqueId())
                .stream()
                .filter(cp -> cp.getLocation().getWorld().equals(playerLoc.getWorld()))
                .filter(cp -> {
                    Location cpLoc = cp.getLocation();
                    double dx = cpLoc.getBlockX() - playerLoc.getBlockX();
                    double dz = cpLoc.getBlockZ() - playerLoc.getBlockZ();
                    double dy = cpLoc.getBlockY() - playerLoc.getBlockY();
                    return Math.abs(dx) <= searchRadius
                            && Math.abs(dz) <= searchRadius
                            && Math.abs(dy) <= searchRadius;
                })
                .min((a, b) -> {
                    double distA = a.getLocation().distanceSquared(playerLoc);
                    double distB = b.getLocation().distanceSquared(playerLoc);
                    return Double.compare(distA, distB);
                })
                .orElse(null);
    }

    /**
     * Handles deletion of all checkpoints for the player.
     */
    private boolean handleDeleteAll(Player player) {
        List<Checkpoint> checkpoints = plugin.getCheckpointManager()
                .getCheckpointsForPlayer(player.getUniqueId());

        if (checkpoints.isEmpty()) {
            MessageUtil.error(player, "You don't have any checkpoints.");
            return true;
        }

        int count = checkpoints.size();
        plugin.getConfirmationManager().requestConfirmation(
                player,
                () -> {
                    // Re-fetch to avoid stale references
                    List<Checkpoint> current = plugin.getCheckpointManager()
                            .getCheckpointsForPlayer(player.getUniqueId());
                    for (Checkpoint cp : current) {
                        plugin.getCheckpointManager().removeCheckpoint(cp);
                    }
                    MessageUtil.success(player, "Deleted " + count + " checkpoint(s).");
                },
                "This will delete all " + count + " of your checkpoints."
        );

        return true;
    }

    /**
     * Handles listing all checkpoints for the player.
     */
    private boolean handleList(Player player) {
        List<Checkpoint> checkpoints = plugin.getCheckpointManager()
                .getCheckpointsForPlayer(player.getUniqueId());

        if (checkpoints.isEmpty()) {
            MessageUtil.info(player, "You don't have any checkpoints.");
            return true;
        }

        int limit = plugin.getConfigManager().getCheckpointLimit();
        MessageUtil.info(player, "§6Your Checkpoints (" + checkpoints.size() + "/" + limit + "):");

        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint cp = checkpoints.get(i);
            Location loc = cp.getLocation();
            String status = cp.isEnabled() ? "§a✓" : "§c✗";
            MessageUtil.info(player, String.format("§7%d. %s §e%s §7@ X: %d, Y: %d, Z: %d §8[%s]",
                    i + 1,
                    status,
                    cp.getFormattedTime(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    loc.getWorld().getName()
            ));
        }

        return true;
    }

    /**
     * Handles restoration of a checkpoint at the player's current location.
     * Uses proximity search to find the nearest checkpoint the player owns.
     */
    private boolean handleRestore(Player player) {
        Checkpoint checkpoint = findCheckpointNearPlayer(player);

        if (checkpoint == null) {
            MessageUtil.error(player, "There is no checkpoint near your location.");
            return true;
        }

        if (!checkpoint.getOwnerId().equals(player.getUniqueId())) {
            MessageUtil.error(player, "This checkpoint belongs to another player.");
            return true;
        }

        if (!checkpoint.isEnabled()) {
            MessageUtil.error(player, "This checkpoint is currently disabled.");
            return true;
        }

        // Show confirmation GUI
        plugin.getConfirmationGUI().openRestoreConfirmation(player, checkpoint);
        return true;
    }

    /**
     * Handles confirmation of a pending action.
     */
    private boolean handleConfirm(Player player) {
        if (plugin.getConfirmationManager().confirm(player)) {
            return true;
        }
        MessageUtil.error(player, "You don't have any pending actions to confirm.");
        return true;
    }

    /**
     * Handles cancellation of a pending action.
     */
    private boolean handleCancel(Player player) {
        if (plugin.getConfirmationManager().cancel(player)) {
            MessageUtil.info(player, "Action cancelled.");
            return true;
        }
        MessageUtil.error(player, "You don't have any pending actions to cancel.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            return filterCompletions(
                    Arrays.asList("here", "delete", "list", "restore", "confirm", "cancel"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> completions = new ArrayList<>();
            completions.add("all");
            completions.add("~");
            return filterCompletions(completions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("delete")) {
            return List.of("~");
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