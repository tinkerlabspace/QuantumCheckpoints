// src/main/java/space/tinkerlab/quantumCheckpoints/config/ConfigManager.java
package space.tinkerlab.quantumCheckpoints.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;

/**
 * Manages plugin configuration including costs, limits, and feature toggles.
 */
public class ConfigManager {

    private final QuantumCheckpoints plugin;

    private boolean checkpointsEnabled;
    private boolean penaltyEnabled;
    private int checkpointLimit;
    private ItemStack checkpointCost;

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
     * Loads configuration from file, creating defaults if necessary.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        this.checkpointsEnabled = config.getBoolean("checkpoints-enabled", true);
        this.penaltyEnabled = config.getBoolean("penalty-enabled", true);
        this.checkpointLimit = config.getInt("checkpoint-limit", 3);

        // Load cost configuration
        String costMaterial = config.getString("cost.material", "");
        int costAmount = config.getInt("cost.amount", 0);

        if (!costMaterial.isEmpty() && costAmount > 0) {
            Material material = Material.matchMaterial(costMaterial);
            if (material != null) {
                this.checkpointCost = new ItemStack(material, costAmount);
            }
        }
    }

    /**
     * Saves the current configuration to file.
     */
    public void saveConfig() {
        FileConfiguration config = plugin.getConfig();

        config.set("checkpoints-enabled", checkpointsEnabled);
        config.set("penalty-enabled", penaltyEnabled);
        config.set("checkpoint-limit", checkpointLimit);

        if (checkpointCost != null) {
            config.set("cost.material", checkpointCost.getType().name());
            config.set("cost.amount", checkpointCost.getAmount());
        } else {
            config.set("cost.material", "");
            config.set("cost.amount", 0);
        }

        plugin.saveConfig();
    }

    // Getters and setters with save

    public boolean isCheckpointsEnabled() {
        return checkpointsEnabled;
    }

    /**
     * Sets whether checkpoints are enabled and saves the config.
     *
     * @param enabled the enabled state
     */
    public void setCheckpointsEnabled(boolean enabled) {
        this.checkpointsEnabled = enabled;
        saveConfig();
    }

    public boolean isPenaltyEnabled() {
        return penaltyEnabled;
    }

    /**
     * Sets whether the restoration penalty is enabled and saves the config.
     *
     * @param enabled the enabled state
     */
    public void setPenaltyEnabled(boolean enabled) {
        this.penaltyEnabled = enabled;
        saveConfig();
    }

    public int getCheckpointLimit() {
        return checkpointLimit;
    }

    /**
     * Sets the checkpoint limit per player and saves the config.
     *
     * @param limit the new limit
     */
    public void setCheckpointLimit(int limit) {
        this.checkpointLimit = Math.max(1, limit);
        saveConfig();
    }

    public ItemStack getCheckpointCost() {
        return checkpointCost != null ? checkpointCost.clone() : null;
    }

    /**
     * Sets the checkpoint creation cost and saves the config.
     *
     * @param cost the cost item stack, or null to remove cost
     */
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

    /**
     * Formats a material name for display.
     *
     * @param material the material
     * @return the formatted name
     */
    private String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }
}