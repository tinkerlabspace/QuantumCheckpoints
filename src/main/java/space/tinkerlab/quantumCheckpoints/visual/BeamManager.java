package space.tinkerlab.quantumCheckpoints.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import space.tinkerlab.quantumCheckpoints.QuantumCheckpoints;
import space.tinkerlab.quantumCheckpoints.checkpoint.Checkpoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages visual beam effects and labels for checkpoints.
 * Beams are only visible to the checkpoint owner via per-player particle
 * sending and hidden armor stands for other players.
 */
public class BeamManager {

    public static final String CHECKPOINT_METADATA_KEY = "quantum_checkpoint";
    public static final String CHECKPOINT_OWNER_KEY = "quantum_checkpoint_owner";
    private static final double BEAM_HEIGHT = 5.0;
    private static final int PARTICLE_COUNT = 3;
    private static final double INTERACTION_STAND_SPACING = 0.5;

    private final QuantumCheckpoints plugin;
    private final Map<UUID, List<ArmorStand>> checkpointEntities;
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
     * The beam is only visible to the checkpoint's owner.
     *
     * @param checkpoint the checkpoint to create visuals for
     */
    public void createBeam(Checkpoint checkpoint) {
        removeBeam(checkpoint);

        Location location = checkpoint.getLocation();

        if (!plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> spawnBeamEntities(checkpoint, location));
        } else {
            spawnBeamEntities(checkpoint, location);
        }
    }

    /**
     * Spawns all entities that make up the checkpoint beam.
     * After spawning, hides all armor stands from every online player
     * except the owner.
     */
    private void spawnBeamEntities(Checkpoint checkpoint, Location location) {
        List<ArmorStand> entities = new ArrayList<>();
        Location baseCenter = location.clone().add(0.5, 0, 0.5);

        // Visible name label
        Location nameLabelLoc = baseCenter.clone().add(0, BEAM_HEIGHT - 1.0, 0);
        ArmorStand nameStand = spawnLabelStand(nameLabelLoc, checkpoint,
                Component.text("⬥ ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(checkpoint.getOwnerName(), NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" ⬥", NamedTextColor.LIGHT_PURPLE)),
                false
        );
        entities.add(nameStand);

        // Time label
        Location timeLabelLoc = nameLabelLoc.clone().subtract(0, 0.3, 0);
        ArmorStand timeStand = spawnLabelStand(timeLabelLoc, checkpoint,
                Component.text(checkpoint.getFormattedTime(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, true),
                true
        );
        entities.add(timeStand);

        // Interaction stands along beam height
        for (double y = 0; y < BEAM_HEIGHT; y += INTERACTION_STAND_SPACING) {
            Location standLoc = baseCenter.clone().add(0, y, 0);
            ArmorStand interactionStand = spawnInteractionStand(standLoc, checkpoint);
            entities.add(interactionStand);
        }

        checkpointEntities.put(checkpoint.getCheckpointId(), entities);

        // Hide these entities from all non-owner players
        hideFromNonOwners(checkpoint.getOwnerId(), entities);
    }

    /**
     * Hides a list of armor stands from all online players except the owner.
     * Uses the Paper API's per-player entity visibility.
     *
     * @param ownerId  the UUID of the checkpoint owner
     * @param entities the armor stands to hide
     */
    private void hideFromNonOwners(UUID ownerId, List<ArmorStand> entities) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(ownerId)) {
                for (ArmorStand stand : entities) {
                    online.hideEntity(plugin, stand);
                }
            }
        }
    }

    /**
     * Updates visibility for a player who just joined.
     * Hides all checkpoint entities that don't belong to them,
     * and ensures their own are visible.
     *
     * @param player the player who joined
     */
    public void updateVisibilityForPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        for (Map.Entry<UUID, List<ArmorStand>> entry : checkpointEntities.entrySet()) {
            UUID checkpointId = entry.getKey();
            List<ArmorStand> entities = entry.getValue();

            // Determine if this checkpoint belongs to the player
            var checkpoint = plugin.getCheckpointManager().getCheckpointById(checkpointId);
            if (checkpoint == null) continue;

            boolean isOwner = checkpoint.getOwnerId().equals(playerId);

            for (ArmorStand stand : entities) {
                if (stand == null || stand.isDead()) continue;
                if (isOwner) {
                    player.showEntity(plugin, stand);
                } else {
                    player.hideEntity(plugin, stand);
                }
            }
        }
    }

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
        stand.setMetadata(CHECKPOINT_OWNER_KEY,
                new FixedMetadataValue(plugin, checkpoint.getOwnerId().toString()));
        return stand;
    }

    private ArmorStand spawnInteractionStand(Location location, Checkpoint checkpoint) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setMarker(false);
        stand.setSmall(true);
        stand.setCustomNameVisible(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setMetadata(CHECKPOINT_METADATA_KEY,
                new FixedMetadataValue(plugin, checkpoint.getCheckpointId().toString()));
        stand.setMetadata(CHECKPOINT_OWNER_KEY,
                new FixedMetadataValue(plugin, checkpoint.getOwnerId().toString()));
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
     * Particles are sent only to the checkpoint owner using the per-player
     * spawnParticle method, so other players cannot see them.
     */
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Checkpoint checkpoint : plugin.getCheckpointManager().getAllCheckpoints()) {
                    if (!checkpoint.isEnabled()) continue;

                    Player owner = Bukkit.getPlayer(checkpoint.getOwnerId());
                    if (owner == null || !owner.isOnline()) continue;

                    Location base = checkpoint.getLocation();

                    // Only render if owner is in the same world and within reasonable distance
                    if (!owner.getWorld().equals(base.getWorld())) continue;
                    if (owner.getLocation().distanceSquared(base) > 10000) continue; // 100 blocks

                    // Vertical beam — sent only to the owner
                    for (double y = 0; y < BEAM_HEIGHT; y += 0.5) {
                        Location particleLoc = base.clone().add(0.5, y, 0.5);
                        owner.spawnParticle(
                                Particle.END_ROD,
                                particleLoc,
                                PARTICLE_COUNT,
                                0.1, 0.1, 0.1,
                                0.01
                        );
                    }

                    // Circular base — sent only to the owner
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                        double x = Math.cos(angle) * 0.5;
                        double z = Math.sin(angle) * 0.5;
                        Location particleLoc = base.clone().add(0.5 + x, 0.1, 0.5 + z);
                        owner.spawnParticle(
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