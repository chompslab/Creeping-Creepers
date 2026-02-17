/*
 * ============================================================================
 * NetherCreeperSwellGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Controls the "swelling" animation and countdown before the Nether Creeper
 * explodes. When the creeper is close enough to its target, it starts
 * swelling (charging up) and eventually explodes.
 *
 * BEHAVIOR (matches vanilla creeper):
 * - Starts swelling when within 3 blocks of target
 * - Keeps swelling while target is within 7 blocks
 * - Deflates when target moves beyond 7 blocks
 * - Commits to explosion once swelling starts (continues while swell > 0)
 * - Keeps moving toward target while swelling
 * - Explosion power is affected by temperature state (handled in entity)
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.ai.goal;

import com.creepingcreepers.entity.nethercreeper.NetherCreeperEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;

/**
 * AI goal that makes the Nether Creeper swell and explode when close to target.
 * Matches vanilla creeper behavior - ruthless and committed.
 *
 * Key behaviors:
 * - Starts swelling when within 3 blocks
 * - Keeps swelling while target within 7 blocks
 * - Deflates when target moves beyond 7 blocks
 * - Continues goal while swell > 0 (commits to explosion)
 * - Keeps moving toward target while swelling
 */
public class NetherCreeperSwellGoal extends Goal {

    /**
     * Reference to the Nether Creeper entity this goal controls.
     */
    private final NetherCreeperEntity creeper;

    /**
     * The current target entity.
     */
    @Nullable
    private LivingEntity target;

    /**
     * Squared distance at which the creeper starts swelling.
     * 3 blocks squared = 9.
     */
    private static final double EXPLOSION_RANGE_SQ = 9.0; // 3 blocks

    /**
     * Squared distance at which the creeper starts deflating.
     * 7 blocks squared = 49. If target moves further than this, creeper deflates.
     */
    private static final double DEFLATE_RANGE_SQ = 49.0; // 7 blocks

    /**
     * Creates a new swell goal for a Nether Creeper.
     *
     * @param creeper The Nether Creeper entity this goal controls
     */
    public NetherCreeperSwellGoal(NetherCreeperEntity creeper) {
        this.creeper = creeper;
        // No flags set - allows creeper to keep moving while swelling
    }

    /**
     * Checks if this goal should start executing.
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

        // Check if target is close enough
        double distanceSq = this.creeper.distanceToSqr(currentTarget);
        if (distanceSq > EXPLOSION_RANGE_SQ) {
            return false;
        }

        // Cache target
        this.target = currentTarget;
        return true;
    }

    /**
     * Called when the goal starts executing.
     */
    @Override
    public void start() {
        // Start swelling - no navigation stop, keeps moving toward target
        this.creeper.setSwellDir(1);
    }

    /**
     * Called when the goal stops executing.
     */
    @Override
    public void stop() {
        this.target = null;

        // Start deflating when goal stops
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

        // Look at target while swelling
        this.creeper.getLookControl().setLookAt(
                this.target,
                30.0F,
                30.0F
        );

        // Adjust swell direction based on distance (like vanilla creeper)
        // Vanilla uses 7 blocks as the deflate distance
        if (distanceSq > DEFLATE_RANGE_SQ) {
            // Target too far (> 7 blocks) - deflate
            this.creeper.setSwellDir(-1);
        } else {
            // Target in range (within 7 blocks) - keep swelling
            this.creeper.setSwellDir(1);
        }
    }

    /**
     * Indicates whether this goal requires frequent updates.
     *
     * @return true because timing is critical for explosion
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
