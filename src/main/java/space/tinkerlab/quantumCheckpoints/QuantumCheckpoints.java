package space.tinkerlab.quantumCheckpoints;

import org.bukkit.plugin.java.JavaPlugin;
import space.tinkerlab.quantumCheckpoints.auto.AutoCheckpointManager;
import space.tinkerlab.quantumCheckpoints.checkpoint.CheckpointManager;
import space.tinkerlab.quantumCheckpoints.commands.CheckpointCommand;
import space.tinkerlab.quantumCheckpoints.commands.CheckpointsAdminCommand;
import space.tinkerlab.quantumCheckpoints.commands.ConfirmationManager;
import space.tinkerlab.quantumCheckpoints.config.ConfigManager;
import space.tinkerlab.quantumCheckpoints.gui.ConfirmationGUI;
import space.tinkerlab.quantumCheckpoints.listeners.CheckpointInteractionListener;
import space.tinkerlab.quantumCheckpoints.listeners.GUIListener;
import space.tinkerlab.quantumCheckpoints.listeners.PlayerJoinListener;
import space.tinkerlab.quantumCheckpoints.storage.DataManager;
import space.tinkerlab.quantumCheckpoints.visual.BeamManager;

import java.util.Objects;

/**
 * Main plugin class for QuantumCheckpoints.
 * Manages the lifecycle of the plugin and provides access to all managers.
 */
public final class QuantumCheckpoints extends JavaPlugin {

    private static QuantumCheckpoints instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private CheckpointManager checkpointManager;
    private BeamManager beamManager;
    private ConfirmationManager confirmationManager;
    private ConfirmationGUI confirmationGUI;
    private AutoCheckpointManager autoCheckpointManager;

    @Override
    public void onEnable() {
        instance = this;

        initializeManagers();
        registerCommands();
        registerListeners();

        dataManager.loadAll();
        beamManager.restoreAllBeams();

        // Start auto-checkpoint system
        autoCheckpointManager.start();

        getLogger().info("QuantumCheckpoints has been enabled!");
    }

    @Override
    public void onDisable() {
        if (autoCheckpointManager != null) {
            autoCheckpointManager.stop();
        }

        if (dataManager != null) {
            dataManager.saveAll();
        }

        if (beamManager != null) {
            beamManager.removeAllBeams();
        }

        getLogger().info("QuantumCheckpoints has been disabled!");
    }

    /**
     * Initializes all manager instances.
     */
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        checkpointManager = new CheckpointManager(this);
        beamManager = new BeamManager(this);
        confirmationManager = new ConfirmationManager(this);
        confirmationGUI = new ConfirmationGUI(this);
        autoCheckpointManager = new AutoCheckpointManager(this);
    }

    /**
     * Registers all plugin commands.
     */
    private void registerCommands() {
        CheckpointCommand checkpointCommand = new CheckpointCommand(this);
        Objects.requireNonNull(getCommand("checkpoint")).setExecutor(checkpointCommand);
        Objects.requireNonNull(getCommand("checkpoint")).setTabCompleter(checkpointCommand);
        Objects.requireNonNull(getCommand("cp")).setExecutor(checkpointCommand);
        Objects.requireNonNull(getCommand("cp")).setTabCompleter(checkpointCommand);

        CheckpointsAdminCommand adminCommand = new CheckpointsAdminCommand(this);
        Objects.requireNonNull(getCommand("checkpoints")).setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("checkpoints")).setTabCompleter(adminCommand);
    }

    /**
     * Registers all event listeners.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CheckpointInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    public static QuantumCheckpoints getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CheckpointManager getCheckpointManager() {
        return checkpointManager;
    }

    public BeamManager getBeamManager() {
        return beamManager;
    }

    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }

    public ConfirmationGUI getConfirmationGUI() {
        return confirmationGUI;
    }

    public AutoCheckpointManager getAutoCheckpointManager() {
        return autoCheckpointManager;
    }
}