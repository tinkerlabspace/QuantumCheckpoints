// src/main/java/space/tinkerlab/quantumCheckpoints/visual/BeamManager.java
package space.tinkerlab.quantumCheckpoints.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages visual beam effects and labels for checkpoints.
 * Uses armor stands for labels and particles for the beam effect.
 */
public class BeamManager {

    public static final String CHECKPOINT_METADATA_KEY = "quantum_checkpoint";
    private static final double BEAM_HEIGHT = 5.0;
    private static final int PARTICLE_COUNT = 3;

    private final QuantumCheckpoints plugin;

    /** Maps checkpoint IDs to their armor stand entities */
    private final Map<UUID, ArmorStand> checkpointLabels;

    /** The task running the particle effects */
    private BukkitTask particleTask;

    /**
     * Creates a new BeamManager.
     *
     * @param plugin the main plugin instance
     */
    public BeamManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.checkpointLabels = new ConcurrentHashMap<>();
        startParticleTask();
    }

    /**
     * Creates a visual beam and label for a checkpoint.
     *
     * @param checkpoint the checkpoint to create visuals for
     */
    public void createBeam(Checkpoint checkpoint) {
        Location location = checkpoint.getLocation();

        // Remove existing label if present
        removeBeam(checkpoint);

        // Create armor stand for label
        Location labelLocation = location.clone().add(0.5, 1.5, 0.5);

        // Ensure we're on the main thread
        if (!plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> createArmorStandLabel(checkpoint, labelLocation));
        } else {
            createArmorStandLabel(checkpoint, labelLocation);
        }
    }

    /**
     * Creates the armor stand entity for the checkpoint label.
     * Must be called on the main server thread.
     */
    private void createArmorStandLabel(Checkpoint checkpoint, Location location) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

        // Configure armor stand to be invisible and non-interactable (except for right-click)
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(false); // Allow interaction
        armorStand.setSmall(true);
        armorStand.setCustomNameVisible(true);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);

        // Set the custom name with checkpoint info
        Component name = Component.text("⬥ ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(checkpoint.getOwnerName(), NamedTextColor.WHITE)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ⬥", NamedTextColor.LIGHT_PURPLE));
        armorStand.customName(name);

        // Add metadata to identify this as a checkpoint
        armorStand.setMetadata(CHECKPOINT_METADATA_KEY,
                new FixedMetadataValue(plugin, checkpoint.getCheckpointId().toString()));

        checkpointLabels.put(checkpoint.getCheckpointId(), armorStand);

        // Create a second armor stand for the time display (below the name)
        Location timeLocation = location.clone().subtract(0, 0.3, 0);
        ArmorStand timeStand = (ArmorStand) location.getWorld().spawnEntity(timeLocation, EntityType.ARMOR_STAND);

        timeStand.setVisible(false);
        timeStand.setGravity(false);
        timeStand.setInvulnerable(true);
        timeStand.setMarker(true);
        timeStand.setSmall(true);
        timeStand.setCustomNameVisible(true);
        timeStand.setBasePlate(false);

        Component timeName = Component.text(checkpoint.getFormattedTime(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true);
        timeStand.customName(timeName);

        // Link the time stand to the main stand via metadata
        timeStand.setMetadata("quantum_checkpoint_time",
                new FixedMetadataValue(plugin, checkpoint.getCheckpointId().toString()));
        armorStand.setMetadata("quantum_checkpoint_time_stand",
                new FixedMetadataValue(plugin, timeStand.getUniqueId().toString()));
    }

    /**
     * Removes the visual beam and label for a checkpoint.
     *
     * @param checkpoint the checkpoint to remove visuals for
     */
    public void removeBeam(Checkpoint checkpoint) {
        ArmorStand armorStand = checkpointLabels.remove(checkpoint.getCheckpointId());

        if (armorStand != null && !armorStand.isDead()) {
            // Remove associated time stand if exists
            if (armorStand.hasMetadata("quantum_checkpoint_time_stand")) {
                String timeStandId = armorStand.getMetadata("quantum_checkpoint_time_stand")
                        .get(0).asString();
                armorStand.getWorld().getEntities().stream()
                        .filter(e -> e.getUniqueId().toString().equals(timeStandId))
                        .findFirst()
                        .ifPresent(org.bukkit.entity.Entity::remove);
            }

            armorStand.remove();
        }
    }

    /**
     * Removes all visual beams from the server.
     */
    public void removeAllBeams() {
        // Stop particle task
        if (particleTask != null) {
            particleTask.cancel();
        }

        // Remove all armor stands
        for (ArmorStand armorStand : checkpointLabels.values()) {
            if (!armorStand.isDead()) {
                // Remove time stand
                if (armorStand.hasMetadata("quantum_checkpoint_time_stand")) {
                    String timeStandId = armorStand.getMetadata("quantum_checkpoint_time_stand")
                            .get(0).asString();
                    armorStand.getWorld().getEntities().stream()
                            .filter(e -> e.getUniqueId().toString().equals(timeStandId))
                            .findFirst()
                            .ifPresent(org.bukkit.entity.Entity::remove);
                }
                armorStand.remove();
            }
        }

        checkpointLabels.clear();
    }

    /**
     * Restores all beams for existing checkpoints.
     * Called on plugin enable.
     */
    public void restoreAllBeams() {
        for (Checkpoint checkpoint : plugin.getCheckpointManager().getAllCheckpoints()) {
            if (checkpoint.isEnabled()) {
                createBeam(checkpoint);
            }
        }
    }

    /**
     * Starts the particle effect task.
     */
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Checkpoint checkpoint : plugin.getCheckpointManager().getAllCheckpoints()) {
                    if (!checkpoint.isEnabled()) continue;

                    Location base = checkpoint.getLocation();

                    // Spawn beam particles
                    for (double y = 0; y < BEAM_HEIGHT; y += 0.5) {
                        Location particleLoc = base.clone().add(0.5, y, 0.5);
                        base.getWorld().spawnParticle(
                                Particle.END_ROD,
                                particleLoc,
                                PARTICLE_COUNT,
                                0.1, 0.1, 0.1,
                                0.01
                        );
                    }

                    // Spawn base particles (circular)
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                        double x = Math.cos(angle) * 0.5;
                        double z = Math.sin(angle) * 0.5;
                        Location particleLoc = base.clone().add(0.5 + x, 0.1, 0.5 + z);
                        base.getWorld().spawnParticle(
                                Particle.DRAGON_BREATH,
                                particleLoc,
                                1,
                                0, 0, 0,
                                0
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Run every 0.5 seconds
    }
}