/*
 * ============================================================================
 * NetherCreeperTargetGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Implements continuous player targeting for the Nether Creeper with special
 * handling for players riding Striders.
 *
 * BEHAVIOR:
 * - Targets players within 16-block radius
 * - Targets players riding Striders within 14-block radius (reduced range)
 * - Sets target immediately with no delay — aggressive behavior
 * - Keeps running continuously while player remains in range
 * - Clears target when player escapes range (via TargetGoal.stop())
 * - Does NOT target other mobs (only retaliates via HurtByTargetGoal)
 * - Only targets survival/adventure mode players
 *
 * WHY STRIDER RIDERS HAVE REDUCED RANGE:
 * Striders are native Nether creatures, so the Nether Creeper is slightly
 * less aggressive toward players who are "traveling like a local."
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.ai.goal;

import com.creepingcreepers.config.CreepingCreepersConfig;
import com.creepingcreepers.entity.nethercreeper.NetherCreeperEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * Continuous AI goal that targets nearby players, with special handling for Strider riders.
 *
 * Sets target immediately on detection and stays running while the player is in range.
 * TargetGoal.stop() clears the target when the player truly escapes.
 */
public class NetherCreeperTargetGoal extends TargetGoal {

    /**
     * Reference to the Nether Creeper entity.
     */
    private final NetherCreeperEntity netherCreeper;

    /**
     * Multiplier applied to detection range for canContinueToUse().
     * Slight hysteresis prevents rapid start/stop oscillation at the edge of range.
     */
    private static final double CONTINUE_RANGE_MULTIPLIER = 1.5;

    /**
     * Creates a new target goal for the Nether Creeper.
     *
     * @param creeper The Nether Creeper this goal controls
     */
    public NetherCreeperTargetGoal(NetherCreeperEntity creeper) {
        super(creeper, false);
        this.netherCreeper = creeper;
    }

    /**
     * Checks if this goal should start executing.
     * Finds the nearest valid player within detection range and sets them as target immediately.
     *
     * @return true if a valid target was found and set
     */
    @Override
    public boolean canUse() {
        Player target = this.findNearestTarget();
        if (target != null) {
            this.mob.setTarget(target);
            return true;
        }
        return false;
    }

    /**
     * Checks if this goal should continue executing.
     * Keeps running while the current target is alive and within range.
     * When this returns false, TargetGoal.stop() is called which clears the target.
     *
     * @return true if the target is still valid and in range
     */
    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();

        if (!(target instanceof Player player)) {
            return false;
        }

        if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
            return false;
        }

        // Use a slightly extended range to avoid rapid start/stop at the boundary
        double detectionRange = this.netherCreeper.getDetectionRangeForPlayer(player);
        return this.netherCreeper.distanceTo(player) <= detectionRange * CONTINUE_RANGE_MULTIPLIER;
    }

    /**
     * Finds the nearest valid player within detection range.
     *
     * @return The nearest valid player, or null if none found
     */
    @Nullable
    private Player findNearestTarget() {
        double maxRange = CreepingCreepersConfig.NETHER_CREEPER_PLAYER_DETECTION_RANGE.get();

        AABB searchBox = this.netherCreeper.getBoundingBox().inflate(maxRange, maxRange / 2.0, maxRange);

        List<Player> nearbyPlayers = this.netherCreeper.level().getEntitiesOfClass(
                Player.class,
                searchBox,
                player -> !player.isSpectator() && !player.isCreative() && player.isAlive()
        );

        if (nearbyPlayers.isEmpty()) {
            return null;
        }

        // Sort by distance — always target the closest player
        nearbyPlayers.sort(Comparator.comparingDouble(this.netherCreeper::distanceTo));

        for (Player player : nearbyPlayers) {
            double detectionRange = this.netherCreeper.getDetectionRangeForPlayer(player);
            if (this.netherCreeper.distanceTo(player) <= detectionRange) {
                return player;
            }
        }

        return null;
    }
}
