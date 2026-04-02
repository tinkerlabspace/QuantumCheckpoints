package space.tinkerlab.quantumCheckpoints.util;

/**
 * Utility class for parsing coordinate values from command arguments.
 */
public final class CoordinateUtil {

    private CoordinateUtil() {}

    /**
     * Parses a coordinate string, supporting relative notation with ~.
     *
     * @param input       the input string (e.g. "100", "~", "~5")
     * @param playerCoord the player's current coordinate for relative calculations
     * @return the parsed absolute coordinate
     * @throws NumberFormatException if the input cannot be parsed
     */
    public static double parse(String input, double playerCoord) {
        if (input.startsWith("~")) {
            if (input.length() == 1) {
                return playerCoord;
            }
            return playerCoord + Double.parseDouble(input.substring(1));
        }
        return Double.parseDouble(input);
    }
}