package space.tinkerlab.quantumCheckpoints.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Handles persistence of checkpoint data to YAML files.
 */
public class DataManager {

    private final QuantumCheckpoints plugin;
    private final File dataFile;

    /**
     * Creates a new DataManager.
     *
     * @param plugin the main plugin instance
     */
    public DataManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "checkpoints.yml");
    }

    /**
     * Loads all checkpoint data from storage.
     */
    public void loadAll() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
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
    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();

        int index = 0;
        for (Checkpoint checkpoint : plugin.getCheckpointManager().getAllCheckpoints()) {
            checkpoint.serialize(yaml.createSection("checkpoints.cp" + index));
            index++;
        }

        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            yaml.save(dataFile);
            plugin.getLogger().info("Saved " + index + " checkpoints");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save checkpoints", e);
        }
    }

    /**
     * Deletes all stored checkpoint data.
     */
    public void deleteAll() {
        if (dataFile.exists()) {
            dataFile.delete();
        }
    }
}