package space.tinkerlab.quantumCheckpoints.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for all sub-commands in the plugin.
 * Implementations handle a single sub-command's logic.
 * By default, commands are player-only. Override {@link #supportsConsole()}
 * and {@link #execute(CommandSender, String[])} for console support.
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
     * Whether this command can be run from the console.
     *
     * @return true if console execution is supported
     */
    default boolean supportsConsole() {
        return false;
    }

    /**
     * Executes the sub-command as a player.
     *
     * @param player the player who issued the command
     * @param args   the remaining arguments after the sub-command name
     */
    void execute(@NotNull Player player, @NotNull String[] args);

    /**
     * Executes the sub-command from any sender (including console).
     * Only called if {@link #supportsConsole()} returns true.
     * Default implementation delegates to the player overload if applicable.
     *
     * @param sender the command sender
     * @param args   the remaining arguments
     */
    default void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            execute(player, args);
        }
    }

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