package space.tinkerlab.quantumCheckpoints.commands;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for all sub-commands in the plugin.
 * Implementations handle a single sub-command's logic, keeping
 * command classes small and focused.
 */
public interface SubCommand {

    /**
     * Gets the primary name of this sub-command.
     *
     * @return the sub-command name (lowercase)
     */
    @NotNull String getName();

    /**
     * Gets any alternative names for this sub-command.
     *
     * @return list of aliases, or empty list if none
     */
    @NotNull default List<String> getAliases() {
        return List.of();
    }

    /**
     * Executes the sub-command.
     *
     * @param player the player who issued the command
     * @param args   the remaining arguments after the sub-command name
     */
    void execute(@NotNull Player player, @NotNull String[] args);

    /**
     * Provides tab completions for this sub-command.
     *
     * @param player the player requesting completions
     * @param args   the remaining arguments after the sub-command name
     * @return list of completions
     */
    @NotNull default List<String> tabComplete(@NotNull Player player, @NotNull String[] args) {
        return List.of();
    }
}