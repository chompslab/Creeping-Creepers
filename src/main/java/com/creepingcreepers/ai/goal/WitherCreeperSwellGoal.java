/*
 * ============================================================================
 * WitherCreeperSwellGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Controls the "swelling" animation and countdown before the Wither Creeper
 * explodes. When the creeper is close enough to its target, it starts
 * swelling (charging up) and eventually explodes.
 *
 * BEHAVIOR (vanilla creeper with line of sight requirement):
 * 1. Checks if the creeper has a target and is close enough (3 blocks)
 * 2. Starts swelling toward explosion
 * 3. Keeps swelling while target within 7 blocks and has line of sight
 * 4. Deflates when target moves beyond 7 blocks or line of sight is broken
 * 5. When swell reaches max, explosion is triggered
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.ai.goal;

import com.creepingcreepers.entity.base.AbstractVariantCreeper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * AI goal that makes the Wither Creeper swell and explode when close to target.
 * Based on vanilla creeper SwellGoal with added line of sight requirement.
 *
 * Key behaviors:
 * - Starts swelling when within 3 blocks
 * - Keeps swelling while target within 7 blocks and has line of sight
 * - Deflates when target moves beyond 7 blocks or loses line of sight
 * - Continues goal while swell > 0 (commits to explosion)
 */
public class WitherCreeperSwellGoal extends Goal {

    /** Squared distance to start swelling (3 blocks). */
    private static final double SWELL_START_RANGE_SQ = 9.0;

    /** Squared distance to cancel swelling (7 blocks). */
    private static final double SWELL_CANCEL_RANGE_SQ = 49.0;

    /**
     * Reference to the creeper entity this goal controls.
     */
    private final AbstractVariantCreeper creeper;

    /**
     * The current target entity.
     */
    @Nullable
    private LivingEntity target;

    /**
     * Creates a new swell goal for a variant creeper.
     *
     * @param creeper The creeper entity this goal controls
     */
    public WitherCreeperSwellGoal(AbstractVariantCreeper creeper) {
        this.creeper = creeper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Checks if this goal should start executing.
     * Matches vanilla: starts if swell > 0 OR target is within 3 blocks.
     *
     * @return true if goal should start
     */
    @Override
    public boolean canUse() {
        LivingEntity livingEntity = this.creeper.getTarget();
        return this.creeper.getSwell() > 0
                || (livingEntity != null && this.creeper.distanceToSqr(livingEntity) < SWELL_START_RANGE_SQ);
    }

    /**
     * Called when the goal starts executing.
     * Stops navigation and caches the target.
     */
    @Override
    public void start() {
        this.creeper.getNavigation().stop();
        this.target = this.creeper.getTarget();
    }

    /**
     * Called when the goal stops executing.
     */
    @Override
    public void stop() {
        this.target = null;
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

    /**
     * Called each tick while the goal is active.
     * Adjusts swell direction based on target distance and line of sight.
     *
     * Swell logic (vanilla + line of sight):
     * - No target: deflate
     * - Target beyond 7 blocks: deflate
     * - No line of sight to target: deflate
     * - Otherwise: swell
     */
    @Override
    public void tick() {
        if (this.target == null) {
            this.creeper.setSwellDir(-1);
        } else if (this.creeper.distanceToSqr(this.target) > SWELL_CANCEL_RANGE_SQ) {
            // Target beyond 7 blocks - deflate
            this.creeper.setSwellDir(-1);
        } else if (!this.creeper.getSensing().hasLineOfSight(this.target)) {
            // No line of sight - deflate
            this.creeper.setSwellDir(-1);
        } else {
            // Target in range with line of sight - swell
            this.creeper.setSwellDir(1);
        }
    }
}
