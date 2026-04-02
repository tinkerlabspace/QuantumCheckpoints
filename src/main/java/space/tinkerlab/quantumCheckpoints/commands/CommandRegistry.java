package space.tinkerlab.quantumCheckpoints.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry that maps sub-command names and aliases to their handlers.
 * Supports case-insensitive lookup.
 */
public class CommandRegistry {

    /** Maps lowercase command names/aliases to their handlers */
    private final Map<String, SubCommand> commands;

    /** Ordered list of registered commands for help/tab-complete */
    private final List<SubCommand> registeredOrder;

    public CommandRegistry() {
        this.commands = new LinkedHashMap<>();
        this.registeredOrder = new ArrayList<>();
    }

    /**
     * Registers a sub-command, mapping its name and all aliases.
     *
     * @param command the sub-command to register
     * @throws IllegalArgumentException if a name/alias conflicts with an existing registration
     */
    public void register(@NotNull SubCommand command) {
        String name = command.getName().toLowerCase();
        if (commands.containsKey(name)) {
            throw new IllegalArgumentException("Sub-command already registered: " + name);
        }

        commands.put(name, command);

        for (String alias : command.getAliases()) {
            String lower = alias.toLowerCase();
            if (commands.containsKey(lower)) {
                throw new IllegalArgumentException("Alias conflicts with existing command: " + lower);
            }
            commands.put(lower, command);
        }

        registeredOrder.add(command);
    }

    /**
     * Looks up a sub-command by name or alias (case-insensitive).
     *
     * @param name the name to look up
     * @return the sub-command, or null if not found
     */
    @Nullable
    public SubCommand get(@NotNull String name) {
        return commands.get(name.toLowerCase());
    }

    /**
     * Gets the primary names of all registered sub-commands.
     * Used for first-level tab completion.
     *
     * @return list of primary names
     */
    @NotNull
    public List<String> getCommandNames() {
        return registeredOrder.stream()
                .map(SubCommand::getName)
                .collect(Collectors.toList());
    }

    /**
     * Gets all registered names including aliases.
     *
     * @return unmodifiable set of all registered keys
     */
    @NotNull
    public Set<String> getAllKeys() {
        return Collections.unmodifiableSet(commands.keySet());
    }
}