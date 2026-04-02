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
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        // Core settings
        this.checkpointsEnabled = config.getBoolean("checkpoints-enabled", true);
        this.checkpointLimit = config.getInt("checkpoint-limit", 3);

        // Cost configuration
        String costMaterial = config.getString("cost.material", "");
        int costAmount = config.getInt("cost.amount", 0);
        if (!costMaterial.isEmpty() && costAmount > 0) {
            Material material = Material.matchMaterial(costMaterial);
            if (material != null) {
                this.checkpointCost = new ItemStack(material, costAmount);
            } else {
                this.checkpointCost = null;
            }
        } else {
            this.checkpointCost = null;
        }

        // Restoration settings
        this.penaltyEnabled = config.getBoolean("penalty-enabled", true);

        // Proximity settings
        this.proximityRadius = config.getDouble("proximity-radius", 3.0);

        // Visual settings
        this.beamHeight = config.getDouble("beam-height", 5.0);
        this.particleViewDistance = config.getDouble("particle-view-distance", 100.0);

        // Confirmation settings
        this.confirmationTimeout = config.getLong("confirmation-timeout", 30);
    }

    /**
     * Saves the current configuration to file.
     */
    public void saveConfig() {
        FileConfiguration config = plugin.getConfig();

        // Core settings
        config.set("checkpoints-enabled", checkpointsEnabled);
        config.set("checkpoint-limit", checkpointLimit);

        // Cost
        if (checkpointCost != null) {
            config.set("cost.material", checkpointCost.getType().name());
            config.set("cost.amount", checkpointCost.getAmount());
        } else {
            config.set("cost.material", "");
            config.set("cost.amount", 0);
        }

        // Restoration settings
        config.set("penalty-enabled", penaltyEnabled);

        // Proximity settings
        config.set("proximity-radius", proximityRadius);

        // Visual settings
        config.set("beam-height", beamHeight);
        config.set("particle-view-distance", particleViewDistance);

        // Confirmation settings
        config.set("confirmation-timeout", confirmationTimeout);

        plugin.saveConfig();
    }

    /**
     * Reloads configuration from disk without saving current values.
     * Useful when admin has manually edited config.yml.
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

    /**
     * Gets a formatted string describing the current cost.
     *
     * @return the cost description
     */
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
    // PROXIMITY SETTINGS
    // ==========================================================================

    public double getProximityRadius() {
        return proximityRadius;
    }

    /**
     * Sets the proximity radius. Minimum value is 1.0.
     *
     * @param radius the new radius in blocks
     */
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

    /**
     * Sets the beam height. Minimum value is 1.0.
     * Note: Existing beams will need to be recreated to reflect changes.
     *
     * @param height the new height in blocks
     */
    public void setBeamHeight(double height) {
        this.beamHeight = Math.max(1.0, height);
        saveConfig();
    }

    public double getParticleViewDistance() {
        return particleViewDistance;
    }

    /**
     * Sets the particle view distance. Minimum value is 10.0.
     *
     * @param distance the new distance in blocks
     */
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

    /**
     * Sets the confirmation timeout. Minimum value is 5 seconds.
     *
     * @param seconds the timeout in seconds
     */
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