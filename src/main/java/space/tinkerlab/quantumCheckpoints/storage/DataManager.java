package space.tinkerlab.quantumCheckpoints.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.auto.AutoCheckpointPreference;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles persistence of checkpoint data and player preferences to YAML files.
 */
public class DataManager {

    private final QuantumCheckpoints plugin;
    private final File checkpointFile;
    private final File preferencesFile;

    /**
     * Creates a new DataManager.
     *
     * @param plugin the main plugin instance
     */
    public DataManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.checkpointFile = new File(plugin.getDataFolder(), "checkpoints.yml");
        this.preferencesFile = new File(plugin.getDataFolder(), "preferences.yml");
    }

    /**
     * Loads all data from storage.
     */
    public void loadAll() {
        loadCheckpoints();
        loadPreferences();
    }

    /**
     * Saves all data to storage.
     */
    public void saveAll() {
        saveCheckpoints();
        savePreferences();
    }

    // ==========================================================================
    // CHECKPOINTS
    // ==========================================================================

    /**
     * Loads all checkpoint data from storage.
     */
    private void loadCheckpoints() {
        if (!checkpointFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(checkpointFile);
        ConfigurationSection checkpointsSection = yaml.getConfigurationSection("checkpoints");

        if (checkpointsSection == null) {
            return;
        }

        int loaded = 0;
        int failed = 0;

        for (String key : checkpointsSection.getKeys(false)) {
            try {
                ConfigurationSection cpSection = checkpointsSection.getConfigurationSection(key);
                if (cpSection != null) {
                    Checkpoint checkpoint = Checkpoint.deserialize(cpSection);
                    if (checkpoint != null) {
                        plugin.getCheckpointManager().addCheckpoint(checkpoint);
                        loaded++;
                    } else {
                        failed++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load checkpoint: " + key, e);
                failed++;
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " checkpoints" +
                (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    /**
     * Saves all checkpoint data to storage.
     */
    private void saveCheckpoints() {
        YamlConfiguration yaml = new YamlConfiguration();

        int index = 0;
        for (Checkpoint checkpoint : plugin.getCheckpointManager().getAllCheckpoints()) {
            checkpoint.serialize(yaml.createSection("checkpoints.cp" + index));
            index++;
        }

        try {
            ensureDataFolder();
            yaml.save(checkpointFile);
            plugin.getLogger().info("Saved " + index + " checkpoints");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save checkpoints", e);
        }
    }

    // ==========================================================================
    // PREFERENCES
    // ==========================================================================

    /**
     * Loads all player auto-checkpoint preferences from storage.
     */
    private void loadPreferences() {
        if (!preferencesFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(preferencesFile);
        ConfigurationSection prefsSection = yaml.getConfigurationSection("preferences");

        if (prefsSection == null) {
            return;
        }

        int loaded = 0;

        for (String key : prefsSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                ConfigurationSection prefSection = prefsSection.getConfigurationSection(key);

                if (prefSection != null) {
                    AutoCheckpointPreference pref = AutoCheckpointPreference.deserialize(prefSection);
                    plugin.getAutoCheckpointManager().loadPreference(playerId, pref);
                    loaded++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid player UUID in preferences: " + key, e);
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " player preferences");
    }

    /**
     * Saves all player auto-checkpoint preferences to storage.
     */
    private void savePreferences() {
        YamlConfiguration yaml = new YamlConfiguration();

        Map<UUID, AutoCheckpointPreference> prefs = plugin.getAutoCheckpointManager().getAllPreferences();

        for (Map.Entry<UUID, AutoCheckpointPreference> entry : prefs.entrySet()) {
            ConfigurationSection section = yaml.createSection("preferences." + entry.getKey().toString());
            entry.getValue().serialize(section);
        }

        try {
            ensureDataFolder();
            yaml.save(preferencesFile);
            plugin.getLogger().info("Saved " + prefs.size() + " player preferences");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save preferences", e);
        }
    }

    // ==========================================================================
    // CLEANUP
    // ==========================================================================

    /**
     * Deletes all stored checkpoint data.
     */
    public void deleteAll() {
        if (checkpointFile.exists()) {
            checkpointFile.delete();
        }
    }

    /**
     * Ensures the plugin data folder exists.
     */
    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }
}