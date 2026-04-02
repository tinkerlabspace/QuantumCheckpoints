// src/main/java/space/tinkerlab/quantumCheckpoints/util/MessageUtil.java
package space.tinkerlab.quantumCheckpoints.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * Utility class for sending formatted messages to players.
 */
public final class MessageUtil {

    private static final String PREFIX = "§8[§5QC§8] ";

    private MessageUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Sends a success message to the sender.
     *
     * @param sender  the message recipient
     * @param message the message to send
     */
    public static void success(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§a" + message);
    }

    /**
     * Sends an error message to the sender.
     *
     * @param sender  the message recipient
     * @param message the message to send
     */
    public static void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§c" + message);
    }

    /**
     * Sends an info message to the sender.
     *
     * @param sender  the message recipient
     * @param message the message to send
     */
    public static void info(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§7" + message);
    }

    /**
     * Sends a warning message to the sender.
     *
     * @param sender  the message recipient
     * @param message the message to send
     */
    public static void warn(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§e" + message);
    }
}