package space.tinkerlab.quantumCheckpoints.auto;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Stores a player's auto-checkpoint preferences.
 * Players can enable/disable auto-checkpoints and set their own interval.
 */
public class AutoCheckpointPreference {

    /**
     * Sentinel value indicating no custom interval is set.
     * The player uses the server default.
     */
    private static final int NO_CUSTOM_INTERVAL = -1;

    private boolean enabled;
    private int intervalMinutes;

    /**
     * Creates a default preference (enabled, using server default interval).
     */
    public AutoCheckpointPreference() {
        this.enabled = true;
        this.intervalMinutes = NO_CUSTOM_INTERVAL;
    }

    /**
     * Creates a preference with specific values.
     */
    public AutoCheckpointPreference(boolean enabled, int intervalMinutes) {
        this.enabled = enabled;
        this.intervalMinutes = intervalMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Whether the player has a custom interval set.
     *
     * @return true if using a custom interval
     */
    public boolean hasCustomInterval() {
        return intervalMinutes != NO_CUSTOM_INTERVAL;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int minutes) {
        this.intervalMinutes = minutes;
    }

    /**
     * Resets to using the server default interval.
     */
    public void clearCustomInterval() {
        this.intervalMinutes = NO_CUSTOM_INTERVAL;
    }

    /**
     * Serializes this preference into a ConfigurationSection.
     *
     * @param section the section to write into
     */
    public void serialize(ConfigurationSection section) {
        section.set("enabled", enabled);
        section.set("intervalMinutes", intervalMinutes);
    }

    /**
     * Deserializes a preference from a ConfigurationSection.
     *
     * @param section the section to read from
     * @return the deserialized preference
     */
    public static AutoCheckpointPreference deserialize(ConfigurationSection section) {
        boolean enabled = section.getBoolean("enabled", true);
        int interval = section.getInt("intervalMinutes", NO_CUSTOM_INTERVAL);
        return new AutoCheckpointPreference(enabled, interval);
    }
}