/*
 * ============================================================================
 * TeleportToTargetGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Controls the Ender Creeper's teleportation when repositioning toward
 * a target. Only teleports if the target is more than 12 blocks away.
 * Will NOT teleport if already within 11 blocks of the target.
 * Within engagement range, the creeper uses standard creeper attack behavior.
 *
 * WHY IT EXISTS:
 * This is what makes the Ender Creeper feel like an Enderman hybrid.
 * The unpredictable teleportation makes it more threatening and harder
 * to escape from than a normal Creeper.
 *
 * HOW IT WORKS:
 * 1. Checks if the creeper has a target farther than 12 blocks
 * 2. If so, teleports within 12 blocks of the target (but not closer than 11)
 * 3. Once within 11 blocks, standard creeper behavior takes over (no more teleporting)
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.ai.goal;

import com.creepingcreepers.entity.endercreeper.EnderCreeperEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * AI goal that handles teleportation to reach targets.
 *
 * Only teleports when the target is more than 12 blocks away.
 * Will NOT teleport if already within 11 blocks of target.
 * Within 11 blocks, standard creeper attack behavior is used (walking toward target).
 */
public class TeleportToTargetGoal extends Goal {

    /** Cooldown between teleport attempts in ticks (1 second). */
    private static final int TELEPORT_COOLDOWN_TICKS = 20;

    /**
     * Reference to the Ender Creeper entity.
     */
    private final EnderCreeperEntity enderCreeper;

    /**
     * Cached target for performance.
     */
    @Nullable
    private LivingEntity target;

    /**
     * Cooldown between teleportation attempts.
     * Prevents the goal from spamming teleport checks.
     */
    private int cooldown = 0;

    /**
     * Maximum engagement range - only teleport if farther than this.
     * Once engaged, teleports within 12 blocks of the target.
     */
    private static final double ENGAGE_MAX_RANGE = EnderCreeperEntity.ENGAGE_TELEPORT_MAX_RANGE; // 12.0
    private static final double ENGAGE_MAX_RANGE_SQ = ENGAGE_MAX_RANGE * ENGAGE_MAX_RANGE; // 144

    /**
     * Minimum engagement range - will NOT teleport if already within this distance.
     * Prevents teleporting when already close enough to attack normally.
     */
    private static final double ENGAGE_MIN_RANGE = EnderCreeperEntity.ENGAGE_TELEPORT_MIN_RANGE; // 11.0
    private static final double ENGAGE_MIN_RANGE_SQ = ENGAGE_MIN_RANGE * ENGAGE_MIN_RANGE; // 121

    /**
     * Creates a new teleport-to-target goal.
     *
     * @param creeper The Ender Creeper this goal controls
     */
    public TeleportToTargetGoal(EnderCreeperEntity creeper) {
        this.enderCreeper = creeper;

        // This goal controls movement
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Checks if this goal should start executing.
     *
     * Returns true if:
     * - Creeper has a valid target
     * - Target is MORE than 12 blocks away (will teleport to engage)
     * - Target is NOT within 11 blocks (too close, use normal attack)
     * - Creeper is allowed to teleport
     *
     * @return true if goal should start
     */
    @Override
    public boolean canUse() {
        // Check cooldown
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }

        // Get target
        LivingEntity currentTarget = this.enderCreeper.getTarget();
        if (currentTarget == null) {
            return false;
        }

        // Check if we can teleport
        if (!this.enderCreeper.canTeleport()) {
            return false;
        }

        // Calculate distance to target
        double distanceSq = this.enderCreeper.distanceToSqr(currentTarget);

        // Don't teleport if already within minimum range (11 blocks)
        // At this distance, use standard creeper attack behavior (walking)
        if (distanceSq <= ENGAGE_MIN_RANGE_SQ) {
            return false;
        }

        // Only teleport if target is MORE than maximum range (12 blocks)
        // Between 11-12 blocks, walk toward target instead
        if (distanceSq <= ENGAGE_MAX_RANGE_SQ) {
            return false;
        }

        // Target is far away (>12 blocks) - teleport to engage
        this.target = currentTarget;
        return true;
    }
    
    /**
     * Called when the goal starts executing.
     * Immediately performs the teleportation.
     */
    @Override
    public void start() {
        if (this.target == null) {
            return;
        }
        
        // Attempt to teleport toward the target
        boolean success = this.enderCreeper.teleportTowardEntity(this.target);
        
        // Set cooldown regardless of success
        // Prevents spam-attempting when blocked
        this.cooldown = TELEPORT_COOLDOWN_TICKS;
        
        // If successful, notify the creeper it should start walking
        if (success) {
            this.enderCreeper.onStartWalking();
        }
    }
    
    /**
     * This goal executes instantly (teleport) and doesn't continue.
     * 
     * @return false always
     */
    @Override
    public boolean canContinueToUse() {
        return false;
    }
    
    /**
     * Called when the goal stops.
     * Clears cached target.
     */
    @Override
    public void stop() {
        this.target = null;
    }
}
