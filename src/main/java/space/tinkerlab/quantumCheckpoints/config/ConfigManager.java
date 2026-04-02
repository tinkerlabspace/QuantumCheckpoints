package space.tinkerlab.quantumCheckpoints.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;

/**
 * Manages plugin configuration including costs, limits, and feature toggles.
 * Supports runtime reloading and provides access to all configurable values.
 */
public class ConfigManager {

    private final QuantumCheckpoints plugin;

    // Core settings
    private boolean checkpointsEnabled;
    private int checkpointLimit;
    private ItemStack checkpointCost;

    // Restoration settings
    private boolean penaltyEnabled;

    // Auto checkpoint settings
    private boolean autoCheckpointEnabled;
    private int autoCheckpointInterval;
    private int autoCheckpointMinInterval;

    // Proximity settings
    private double proximityRadius;

    // Visual settings
    private double beamHeight;
    private double particleViewDistance;

    // Confirmation settings
    private long confirmationTimeout;

    /**
     * Creates a new ConfigManager and loads the configuration.
     *
     * @param plugin the main plugin instance
     */
    public ConfigManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Loads or reloads configuration from file.
     * Creates defaults if the file doesn't exist.
     * Logs warnings and corrects invalid values.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        // Core settings
        this.checkpointsEnabled = config.getBoolean("checkpoints-enabled", true);
        this.checkpointLimit = config.getInt("checkpoint-limit", 3);

        // Cost configuration with validation
        String costMaterial = config.getString("cost.material", "");
        int costAmount = config.getInt("cost.amount", 0);

        if (!costMaterial.isEmpty() && costAmount > 0) {
            Material material = Material.matchMaterial(costMaterial);
            if (material != null) {
                this.checkpointCost = new ItemStack(material, costAmount);
            } else {
                plugin.getLogger().warning("Invalid material '" + costMaterial + "' in config. Setting cost to free.");
                this.checkpointCost = null;
                config.set("cost.material", "");
                config.set("cost.amount", 0);
                plugin.saveConfig();
            }
        } else {
            this.checkpointCost = null;
        }

        // Restoration settings
        this.penaltyEnabled = config.getBoolean("penalty-enabled", true);

        // Auto checkpoint settings with validation
        this.autoCheckpointEnabled = config.getBoolean("auto-checkpoint-enabled", true);

        this.autoCheckpointMinInterval = config.getInt("auto-checkpoint-min-interval", 1);
        if (this.autoCheckpointMinInterval < 1) {
            plugin.getLogger().warning("auto-checkpoint-min-interval must be at least 1. Correcting to 1.");
            this.autoCheckpointMinInterval = 1;
            config.set("auto-checkpoint-min-interval", 1);
            plugin.saveConfig();
        }

        this.autoCheckpointInterval = config.getInt("auto-checkpoint-interval", 10);
        if (this.autoCheckpointInterval < this.autoCheckpointMinInterval) {
            plugin.getLogger().warning("auto-checkpoint-interval must be at least " +
                    this.autoCheckpointMinInterval + ". Correcting.");
            this.autoCheckpointInterval = this.autoCheckpointMinInterval;
            config.set("auto-checkpoint-interval", this.autoCheckpointInterval);
            plugin.saveConfig();
        }

        // Proximity settings with validation
        this.proximityRadius = config.getDouble("proximity-radius", 3.0);
        if (this.proximityRadius < 1.0) {
            plugin.getLogger().warning("proximity-radius must be at least 1.0. Correcting to 1.0.");
            this.proximityRadius = 1.0;
            config.set("proximity-radius", 1.0);
            plugin.saveConfig();
        }

        // Visual settings with validation
        this.beamHeight = config.getDouble("beam-height", 5.0);
        if (this.beamHeight < 1.0) {
            plugin.getLogger().warning("beam-height must be at least 1.0. Correcting to 1.0.");
            this.beamHeight = 1.0;
            config.set("beam-height", 1.0);
            plugin.saveConfig();
        }

        this.particleViewDistance = config.getDouble("particle-view-distance", 100.0);
        if (this.particleViewDistance < 10.0) {
            plugin.getLogger().warning("particle-view-distance must be at least 10.0. Correcting to 10.0.");
            this.particleViewDistance = 10.0;
            config.set("particle-view-distance", 10.0);
            plugin.saveConfig();
        }

        // Confirmation settings with validation
        this.confirmationTimeout = config.getLong("confirmation-timeout", 30);
        if (this.confirmationTimeout < 5) {
            plugin.getLogger().warning("confirmation-timeout must be at least 5. Correcting to 5.");
            this.confirmationTimeout = 5;
            config.set("confirmation-timeout", 5);
            plugin.saveConfig();
        }
    }

    /**
     * Saves the current configuration to file.
     */
    public void saveConfig() {
        FileConfiguration config = plugin.getConfig();

        config.set("checkpoints-enabled", checkpointsEnabled);
        config.set("checkpoint-limit", checkpointLimit);

        if (checkpointCost != null) {
            config.set("cost.material", checkpointCost.getType().name());
            config.set("cost.amount", checkpointCost.getAmount());
        } else {
            config.set("cost.material", "");
            config.set("cost.amount", 0);
        }

        config.set("penalty-enabled", penaltyEnabled);

        config.set("auto-checkpoint-enabled", autoCheckpointEnabled);
        config.set("auto-checkpoint-interval", autoCheckpointInterval);
        config.set("auto-checkpoint-min-interval", autoCheckpointMinInterval);

        config.set("proximity-radius", proximityRadius);

        config.set("beam-height", beamHeight);
        config.set("particle-view-distance", particleViewDistance);

        config.set("confirmation-timeout", confirmationTimeout);

        plugin.saveConfig();
    }

    /**
     * Reloads configuration from disk without saving current values.
     */
    public void reload() {
        loadConfig();
    }

    // ==========================================================================
    // CORE SETTINGS
    // ==========================================================================

    public boolean isCheckpointsEnabled() {
        return checkpointsEnabled;
    }

    public void setCheckpointsEnabled(boolean enabled) {
        this.checkpointsEnabled = enabled;
        saveConfig();
    }

    public int getCheckpointLimit() {
        return checkpointLimit;
    }

    public void setCheckpointLimit(int limit) {
        this.checkpointLimit = Math.max(1, limit);
        saveConfig();
    }

    public ItemStack getCheckpointCost() {
        return checkpointCost != null ? checkpointCost.clone() : null;
    }

    public void setCheckpointCost(ItemStack cost) {
        this.checkpointCost = cost != null ? cost.clone() : null;
        saveConfig();
    }

    public String getCostDescription() {
        if (checkpointCost == null) {
            return "Free";
        }
        return checkpointCost.getAmount() + "x " + formatMaterialName(checkpointCost.getType());
    }

    // ==========================================================================
    // RESTORATION SETTINGS
    // ==========================================================================

    public boolean isPenaltyEnabled() {
        return penaltyEnabled;
    }

    public void setPenaltyEnabled(boolean enabled) {
        this.penaltyEnabled = enabled;
        saveConfig();
    }

    // ==========================================================================
    // AUTO CHECKPOINT SETTINGS
    // ==========================================================================

    public boolean isAutoCheckpointEnabled() {
        return autoCheckpointEnabled;
    }

    public void setAutoCheckpointEnabled(boolean enabled) {
        this.autoCheckpointEnabled = enabled;
        saveConfig();
    }

    public int getAutoCheckpointInterval() {
        return autoCheckpointInterval;
    }

    /**
     * Sets the default auto-checkpoint interval.
     * Enforces the minimum interval.
     *
     * @param minutes the interval in minutes
     */
    public void setAutoCheckpointInterval(int minutes) {
        this.autoCheckpointInterval = Math.max(autoCheckpointMinInterval, minutes);
        saveConfig();
    }

    public int getAutoCheckpointMinInterval() {
        return autoCheckpointMinInterval;
    }

    /**
     * Sets the minimum auto-checkpoint interval players can use.
     * Also adjusts the default interval up if it's now below the new minimum.
     *
     * @param minutes the minimum interval in minutes
     */
    public void setAutoCheckpointMinInterval(int minutes) {
        this.autoCheckpointMinInterval = Math.max(1, minutes);
        if (this.autoCheckpointInterval < this.autoCheckpointMinInterval) {
            this.autoCheckpointInterval = this.autoCheckpointMinInterval;
        }
        saveConfig();
    }

    // ==========================================================================
    // PROXIMITY SETTINGS
    // ==========================================================================

    public double getProximityRadius() {
        return proximityRadius;
    }

    public void setProximityRadius(double radius) {
        this.proximityRadius = Math.max(1.0, radius);
        saveConfig();
    }

    // ==========================================================================
    // VISUAL SETTINGS
    // ==========================================================================

    public double getBeamHeight() {
        return beamHeight;
    }

    public void setBeamHeight(double height) {
        this.beamHeight = Math.max(1.0, height);
        saveConfig();
    }

    public double getParticleViewDistance() {
        return particleViewDistance;
    }

    public void setParticleViewDistance(double distance) {
        this.particleViewDistance = Math.max(10.0, distance);
        saveConfig();
    }

    // ==========================================================================
    // CONFIRMATION SETTINGS
    // ==========================================================================

    public long getConfirmationTimeout() {
        return confirmationTimeout;
    }

    public void setConfirmationTimeout(long seconds) {
        this.confirmationTimeout = Math.max(5, seconds);
        saveConfig();
    }

    // ==========================================================================
    // UTILITY
    // ==========================================================================

    private String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }
}