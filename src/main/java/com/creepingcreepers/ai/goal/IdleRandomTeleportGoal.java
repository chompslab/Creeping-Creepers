/*
 * ============================================================================
 * IdleRandomTeleportGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Controls the Ender Creeper's idle random teleportation behavior.
 * When the creeper has no target, it will occasionally teleport to a
 * random nearby location, exactly like an Enderman does when passive.
 *
 * WHY IT EXISTS:
 * Extracted from EnderCreeperEntity.tick() to follow Mojang's convention
 * of separating AI behaviors into composable goals. This makes the behavior
 * easier to understand, extend, and disable independently.
 *
 * HOW IT WORKS:
 * 1. Only activates when the creeper has no target (idle)
 * 2. Uses a randomized timer (5-10 seconds between checks)
 * 3. On each timer expiry, has a 1-in-100 chance to teleport
 * 4. Teleports using Enderman-style logic (64x64x64 cube)
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.ai.goal;

import com.creepingcreepers.entity.endercreeper.EnderCreeperEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI goal that handles idle random teleportation (Enderman-like behavior).
 *
 * When the Ender Creeper has no target, it will periodically teleport
 * to a random nearby location, mimicking Enderman passive behavior.
 */
public class IdleRandomTeleportGoal extends Goal {

    /** 1-in-N chance per timer expiry to teleport. */
    private static final int TELEPORT_CHANCE = 100;

    /** Minimum delay before next teleport check, in ticks. */
    private static final int MIN_DELAY = 100;

    /** Random additional delay for teleport check, in ticks. */
    private static final int RANDOM_DELAY = 100;

    /** Reference to the Ender Creeper entity. */
    private final EnderCreeperEntity enderCreeper;

    /** Timer for idle random teleportation. */
    private int idleTeleportTimer = 0;

    /**
     * Creates a new idle random teleport goal.
     *
     * @param creeper The Ender Creeper this goal controls
     */
    public IdleRandomTeleportGoal(EnderCreeperEntity creeper) {
        this.enderCreeper = creeper;
        // No flags — this goal doesn't control movement or look direction,
        // so it can run concurrently with wander and look-around goals
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    /**
     * Checks if this goal should start executing.
     *
     * Returns true if the creeper has no target (is idle).
     *
     * @return true if goal should start
     */
    @Override
    public boolean canUse() {
        return this.enderCreeper.isAlive() && this.enderCreeper.getTarget() == null;
    }

    /**
     * Checks if this goal should continue executing.
     *
     * Continues as long as the creeper remains idle (no target).
     *
     * @return true if the creeper still has no target
     */
    @Override
    public boolean canContinueToUse() {
        return this.enderCreeper.isAlive() && this.enderCreeper.getTarget() == null;
    }

    /**
     * Called when the goal starts. Resets the timer.
     */
    @Override
    public void start() {
        this.idleTeleportTimer = MIN_DELAY + this.enderCreeper.getRandom().nextInt(RANDOM_DELAY);
    }

    /**
     * Called when the goal stops (creeper acquired a target).
     * Resets the timer so it starts fresh next time.
     */
    @Override
    public void stop() {
        this.idleTeleportTimer = 0;
    }

    /**
     * Called every tick while the goal is active.
     * Decrements the timer and attempts a random teleport when it expires.
     */
    @Override
    public void tick() {
        if (this.idleTeleportTimer > 0) {
            this.idleTeleportTimer--;
        } else {
            // Random chance to teleport when idle (like Enderman)
            if (this.enderCreeper.getRandom().nextInt(TELEPORT_CHANCE) == 0
                    && this.enderCreeper.canTeleport()) {
                this.enderCreeper.teleportRandomly();
            }
            // Reset timer (randomized between 5-10 seconds)
            this.idleTeleportTimer = MIN_DELAY + this.enderCreeper.getRandom().nextInt(RANDOM_DELAY);
        }
    }
}
