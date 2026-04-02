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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages visual beam effects and labels for checkpoints.
 * Uses multiple armor stands along the beam height to provide
 * a fully interactive column that players can right-click at any point.
 */
public class BeamManager {

    public static final String CHECKPOINT_METADATA_KEY = "quantum_checkpoint";
    private static final String CHECKPOINT_BEAM_KEY = "quantum_checkpoint_beam";
    private static final double BEAM_HEIGHT = 5.0;
    private static final int PARTICLE_COUNT = 3;

    /**
     * Spacing between invisible interaction armor stands along the beam.
     * Smaller values create a more seamless clickable column but use more entities.
     */
    private static final double INTERACTION_STAND_SPACING = 0.5;

    private final QuantumCheckpoints plugin;

    /** Maps checkpoint IDs to all associated armor stand entities */
    private final Map<UUID, List<ArmorStand>> checkpointEntities;

    /** The task running the particle effects */
    private BukkitTask particleTask;

    /**
     * Creates a new BeamManager.
     *
     * @param plugin the main plugin instance
     */
    public BeamManager(QuantumCheckpoints plugin) {
        this.plugin = plugin;
        this.checkpointEntities = new ConcurrentHashMap<>();
        startParticleTask();
    }

    /**
     * Creates a visual beam and label for a checkpoint.
     *
     * @param checkpoint the checkpoint to create visuals for
     */
    public void createBeam(Checkpoint checkpoint) {
        // Remove existing entities if present
        removeBeam(checkpoint);

        Location location = checkpoint.getLocation();

        // Ensure we're on the main thread for entity spawning
        if (!plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> spawnBeamEntities(checkpoint, location));
        } else {
            spawnBeamEntities(checkpoint, location);
        }
    }

    /**
     * Spawns all entities that make up the checkpoint beam.
     * Includes the visible name/time labels and invisible interaction stands.
     * Must be called on the main server thread.
     *
     * @param checkpoint the checkpoint data
     * @param location   the base location of the checkpoint
     */
    private void spawnBeamEntities(Checkpoint checkpoint, Location location) {
        List<ArmorStand> entities = new ArrayList<>();
        Location baseCenter = location.clone().add(0.5, 0, 0.5);

        // Spawn the visible name label near the top of the beam
        Location nameLabelLoc = baseCenter.clone().add(0, BEAM_HEIGHT - 1.0, 0);
        ArmorStand nameStand = spawnLabelStand(nameLabelLoc, checkpoint,
                Component.text("⬥ ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(checkpoint.getOwnerName(), NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" ⬥", NamedTextColor.LIGHT_PURPLE)),
                false // Not a marker — allows right-click interaction
        );
        entities.add(nameStand);

        // Spawn the visible time label just below the name
        Location timeLabelLoc = nameLabelLoc.clone().subtract(0, 0.3, 0);
        ArmorStand timeStand = spawnLabelStand(timeLabelLoc, checkpoint,
                Component.text(checkpoint.getFormattedTime(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, true),
                true // Marker is fine; the interaction stands below handle clicks
        );
        entities.add(timeStand);

        // Spawn invisible interaction armor stands along the full beam height.
        // These ensure the player can right-click at any point on the beam column.
        for (double y = 0; y < BEAM_HEIGHT; y += INTERACTION_STAND_SPACING) {
            Location standLoc = baseCenter.clone().add(0, y, 0);
            ArmorStand interactionStand = spawnInteractionStand(standLoc, checkpoint);
            entities.add(interactionStand);
        }

        checkpointEntities.put(checkpoint.getCheckpointId(), entities);
    }

    /**
     * Spawns a visible label armor stand with a custom name.
     *
     * @param location   the spawn location
     * @param checkpoint the associated checkpoint
     * @param name       the display name component
     * @param marker     whether this should be a marker (no hitbox)
     * @return the spawned armor stand
     */
    private ArmorStand spawnLabelStand(Location location, Checkpoint checkpoint,
                                       Component name, boolean marker) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setMarker(marker);
        stand.setSmall(true);
        stand.setCustomNameVisible(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.customName(name);
        stand.setMetadata(CHECKPOINT_METADATA_KEY,
                new FixedMetadataValue(plugin, checkpoint.getCheckpointId().toString()));
        stand.setMetadata(CHECKPOINT_BEAM_KEY,
                new FixedMetadataValue(plugin, checkpoint.getCheckpointId().toString()));
        return stand;
    }

    /**
     * Spawns an invisible interaction armor stand.
     * These have no visible name but retain a hitbox so players can right-click them.
     *
     * @param location   the spawn location
     * @param checkpoint the associated checkpoint
     * @return the spawned armor stand
     */
    private ArmorStand spawnInteractionStand(Location location, Checkpoint checkpoint) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setMarker(false); // Must NOT be a marker so it retains a hitbox
        stand.setSmall(true);
        stand.setCustomNameVisible(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setMetadata(CHECKPOINT_METADATA_KEY,
                new FixedMetadataValue(plugin, checkpoint.getCheckpointId().toString()));
        stand.setMetadata(CHECKPOINT_BEAM_KEY,
                new FixedMetadataValue(plugin, checkpoint.getCheckpointId().toString()));
        return stand;
    }

    /**
     * Removes the visual beam and all associated entities for a checkpoint.
     *
     * @param checkpoint the checkpoint to remove visuals for
     */
    public void removeBeam(Checkpoint checkpoint) {
        List<ArmorStand> entities = checkpointEntities.remove(checkpoint.getCheckpointId());

        if (entities != null) {
            for (ArmorStand stand : entities) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }
    }

    /**
     * Removes all visual beams from the server.
     */
    public void removeAllBeams() {
        if (particleTask != null) {
            particleTask.cancel();
        }

        for (List<ArmorStand> entities : checkpointEntities.values()) {
            for (ArmorStand stand : entities) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }

        checkpointEntities.clear();
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
     * Starts the particle effect task that renders beam particles for all active checkpoints.
     */
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Checkpoint checkpoint : plugin.getCheckpointManager().getAllCheckpoints()) {
                    if (!checkpoint.isEnabled()) continue;

                    Location base = checkpoint.getLocation();

                    // Spawn vertical beam particles
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

                    // Spawn circular base particles
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
        }.runTaskTimer(plugin, 0L, 10L);
    }
}