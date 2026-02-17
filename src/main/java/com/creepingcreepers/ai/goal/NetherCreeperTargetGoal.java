/*
 * ============================================================================
 * NetherCreeperTargetGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Implements player targeting for the Nether Creeper with special handling
 * for players riding Striders.
 *
 * BEHAVIOR:
 * - Targets players within 16-block radius
 * - Targets players riding Striders within 14-block radius (reduced range)
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
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * AI goal that targets nearby players, with special handling for Strider riders.
 *
 * Players on foot are detected at 16 blocks, while players riding Striders
 * are only detected at 14 blocks.
 */
public class NetherCreeperTargetGoal extends TargetGoal {

    /**
     * Reference to the Nether Creeper entity.
     */
    private final NetherCreeperEntity netherCreeper;

    /**
     * The player we're considering targeting.
     */
    @Nullable
    private Player pendingTarget;

    /**
     * Brief delay before acquiring target (gives player a moment to react).
     */
    private int targetingDelay;

    /**
     * Ticks before target is acquired.
     */
    private static final int TARGETING_DELAY_TICKS = 5;

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
     * Looks for players within detection range.
     *
     * @return true if a valid target is found
     */
    @Override
    public boolean canUse() {
        // Use the larger detection range for initial search
        double maxRange = CreepingCreepersConfig.NETHER_CREEPER_PLAYER_DETECTION_RANGE.get();

        // Create search box
        AABB searchBox = this.netherCreeper.getBoundingBox().inflate(maxRange, maxRange / 2.0, maxRange);

        // Get all nearby players (not spectator, not creative)
        List<Player> nearbyPlayers = this.netherCreeper.level().getEntitiesOfClass(
                Player.class,
                searchBox,
                player -> !player.isSpectator() && !player.isCreative() && player.isAlive()
        );

        if (nearbyPlayers.isEmpty()) {
            return false;
        }

        // Sort by distance (closest first)
        nearbyPlayers.sort(Comparator.comparingDouble(this.netherCreeper::distanceTo));

        // Find the closest valid target
        for (Player player : nearbyPlayers) {
            // Get the appropriate detection range for this player
            double detectionRange = this.netherCreeper.getDetectionRangeForPlayer(player);
            double distance = this.netherCreeper.distanceTo(player);

            if (distance <= detectionRange) {
                this.pendingTarget = player;
                return true;
            }
        }

        this.pendingTarget = null;
        return false;
    }

    /**
     * Called when the goal starts executing.
     */
    @Override
    public void start() {
        this.targetingDelay = TARGETING_DELAY_TICKS;
    }

    /**
     * Called when the goal stops executing.
     */
    @Override
    public void stop() {
        this.pendingTarget = null;
        super.stop();
    }

    /**
     * Checks if this goal should continue executing.
     *
     * @return true if target is still valid and in range
     */
    @Override
    public boolean canContinueToUse() {
        if (this.pendingTarget == null) {
            return false;
        }

        // Stop if player is no longer valid
        if (!this.pendingTarget.isAlive() || this.pendingTarget.isSpectator() || this.pendingTarget.isCreative()) {
            return false;
        }

        // Stop if we've already set this player as our main target
        if (this.netherCreeper.getTarget() == this.pendingTarget) {
            return false;
        }

        // Check if player is still in range (use their specific range)
        double detectionRange = this.netherCreeper.getDetectionRangeForPlayer(this.pendingTarget);
        double distance = this.netherCreeper.distanceTo(this.pendingTarget);

        return distance <= detectionRange;
    }

    /**
     * Called each tick while the goal is active.
     * Counts down the targeting delay, then sets the target.
     */
    @Override
    public void tick() {
        if (this.pendingTarget == null) {
            return;
        }

        // Count down targeting delay
        if (this.targetingDelay > 0) {
            this.targetingDelay--;
            return;
        }

        // Delay expired - acquire target
        this.netherCreeper.setTarget(this.pendingTarget);
        this.pendingTarget = null;
    }
}
