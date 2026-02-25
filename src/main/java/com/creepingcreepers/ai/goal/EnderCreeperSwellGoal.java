/*
 * ============================================================================
 * EnderCreeperSwellGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Controls the "swelling" animation and countdown before the Ender Creeper
 * explodes. When the creeper is close enough to its target, it starts
 * swelling (charging up) and eventually explodes.
 *
 * WHY IT EXISTS:
 * This is the core Creeper behavior - the explosion mechanic. Without this
 * goal, the Ender Creeper would just be a teleporting mob that never explodes.
 *
 * HOW IT WORKS (matches vanilla creeper exactly):
 * 1. Checks if the creeper has a target and is close enough (3 blocks) to START
 * 2. Keeps swelling while target is within 7 blocks
 * 3. Deflates when target moves beyond 7 blocks
 * 4. Goal continues as long as swell > 0 (ruthless - commits to explosion)
 * 5. Creeper KEEPS MOVING toward target while swelling (no navigation stop)
 * 6. When swell reaches max (30 ticks), explosion triggers
 *
 * CRITICAL REQUIREMENTS (vanilla creeper behavior):
 * - Start swelling at 3 blocks, deflate at 7 blocks
 * - canContinueToUse() returns true while swell > 0 (like vanilla)
 * - NO navigation stop - keeps pursuing while swelling
 * - NO line of sight requirement
 * - Uses squared distance for performance
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
 * AI goal that makes the Ender Creeper swell and explode when close to target.
 * Matches vanilla creeper behavior exactly - ruthless and committed.
 *
 * Key behaviors (same as vanilla creeper):
 * - Starts swelling when within 3 blocks
 * - Keeps swelling while target within 7 blocks
 * - Deflates when target moves beyond 7 blocks
 * - Continues goal while swell > 0 (commits to explosion)
 * - KEEPS MOVING toward target while swelling (no navigation stop)
 * - NO line of sight requirement
 */
public class EnderCreeperSwellGoal extends Goal {

    /**
     * Reference to the Ender Creeper entity this goal controls.
     */
    private final EnderCreeperEntity creeper;

    /**
     * The current target entity.
     * Cached for performance (avoid repeated lookups).
     */
    @Nullable
    private LivingEntity target;

    /**
     * Squared distance at which the creeper starts swelling.
     * 3 blocks squared = 9. Using squared distance is faster
     * because we avoid the expensive sqrt() call.
     */
    private static final double EXPLOSION_RANGE_SQ = 9.0; // 3 blocks

    /**
     * Squared distance at which the creeper starts deflating (like vanilla).
     * 7 blocks squared = 49. If target moves further than this, creeper deflates.
     */
    private static final double DEFLATE_RANGE_SQ = 49.0; // 7 blocks

    /**
     * Creates a new swell goal for an Ender Creeper.
     *
     * @param creeper The Ender Creeper entity this goal controls
     */
    public EnderCreeperSwellGoal(EnderCreeperEntity creeper) {
        this.creeper = creeper;
        // MOVE flag stops other movement goals while swelling, like vanilla creeper
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Checks if this goal should start executing.
     *
     * Returns true if:
     * - Creeper has a valid target
     * - Target is within explosion range (3 blocks)
     * - NO line of sight requirement (like vanilla creeper)
     *
     * @return true if goal should start
     */
    @Override
    public boolean canUse() {
        // Get the current target
        LivingEntity currentTarget = this.creeper.getTarget();

        // No target = can't swell
        if (currentTarget == null) {
            return false;
        }

        // Check if target is close enough (3 blocks)
        double distanceSq = this.creeper.distanceToSqr(currentTarget);
        if (distanceSq > EXPLOSION_RANGE_SQ) {
            return false;
        }

        // Cache target for use in tick()
        this.target = currentTarget;
        return true;
    }

    /**
     * Called when the goal starts executing.
     * Starts the swelling countdown.
     */
    @Override
    public void start() {
        // Stop navigation so the creeper stands still while swelling
        this.creeper.getNavigation().stop();
        // Start swelling
        this.creeper.setSwellDir(1);
    }
    
    /**
     * Called when the goal stops executing.
     * Resets the swell state if we stopped before exploding.
     */
    @Override
    public void stop() {
        this.target = null;
        
        // Start deflating
        this.creeper.setSwellDir(-1);
    }
    
    /**
     * Checks if this goal should continue executing.
     *
     * Like vanilla creeper: continues as long as there's swell charge OR target is in range.
     * This makes the creeper commit to explosions - once it starts swelling, it keeps going.
     *
     * @return true if goal should continue
     */
    @Override
    public boolean canContinueToUse() {
        // Like vanilla: continue if still has swell charge OR can start new swell
        // This makes the creeper ruthless - once swelling, it commits
        return this.creeper.getSwell() > 0 || this.canUse();
    }
    
    /**
     * Called each tick while the goal is active.
     *
     * Like vanilla creeper:
     * - When within 7 blocks: keep swelling and keep moving toward target
     * - When further than 7 blocks: start deflating but keep pursuing
     * - Creeper keeps moving toward target while swelling (no navigation stop)
     */
    @Override
    public void tick() {
        // Update target reference
        LivingEntity currentTarget = this.creeper.getTarget();
        if (currentTarget != null) {
            this.target = currentTarget;
        }

        // If no target, just deflate
        if (this.target == null || !this.target.isAlive()) {
            this.creeper.setSwellDir(-1);
            return;
        }

        // Calculate distance to target
        double distanceSq = this.creeper.distanceToSqr(this.target);

        // Look at target
        this.creeper.getLookControl().setLookAt(
                this.target,
                30.0F, // Max rotation per tick horizontally
                30.0F  // Max rotation per tick vertically
        );

        // Adjust swell direction based on distance (like vanilla creeper)
        // Vanilla uses 7 blocks as the deflate distance
        if (distanceSq > DEFLATE_RANGE_SQ) {
            // Target too far (> 7 blocks) - deflate
            this.creeper.setSwellDir(-1);
        } else {
            // Target in range (within 7 blocks) - keep swelling
            // Creeper stays still while swelling (Goal.Flag.MOVE blocks movement goals)
            this.creeper.setSwellDir(1);
        }
    }
    
    /**
     * Indicates whether this goal requires the creeper to be updated frequently.
     * 
     * @return true because timing is critical for explosion
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
