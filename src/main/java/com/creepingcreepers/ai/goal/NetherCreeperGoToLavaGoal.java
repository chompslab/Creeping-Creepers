/*
 * ============================================================================
 * NetherCreeperGoToLavaGoal.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Makes the Nether Creeper seek out lava whenever it's not in lava.
 * This simulates the creature's desire to warm up, similar to how Striders
 * shiver when out of lava.
 *
 * BEHAVIOR:
 * - Activates whenever the creeper is out of lava (not just when cold)
 * - Only activates when no target OR target is outside pursuit range (6 blocks)
 * - Searches for nearby lava blocks and paths toward them
 * - Stops once the creeper enters lava
 * - Stops immediately if a target gets within pursuit range (6 blocks)
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.ai.goal;

import com.creepingcreepers.entity.nethercreeper.NetherCreeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * AI goal that makes Nether Creepers seek out lava when not already in it.
 *
 * When out of lava and not within pursuit range of a target, the creeper
 * prioritizes getting back to lava. If a target gets close (within 6 blocks),
 * this goal stops and lets the melee/swell goals take over.
 */
public class NetherCreeperGoToLavaGoal extends Goal {

    /** Base wander ticks before changing direction. */
    private static final int WANDER_BASE_DURATION = 40;

    /** Random additional wander ticks (0 to this value). */
    private static final int WANDER_RANDOM_DURATION = 40;

    /** Distance in blocks for random walk target. */
    private static final int WANDER_DISTANCE = 5;

    /** Speed multiplier for random wandering. */
    private static final double WANDER_SPEED_MODIFIER = 0.8;

    /**
     * Reference to the Nether Creeper entity.
     */
    private final NetherCreeperEntity creeper;

    /**
     * Movement speed multiplier when seeking lava.
     * Combined with URGENT_SPEED (1.3x) for final movement speed.
     */
    private final double speedModifier;

    /**
     * Urgent speed multiplier - creeper moves faster when desperate for warmth.
     */
    private static final double URGENT_SPEED = 1.3;

    /**
     * The target lava position to move toward.
     */
    @Nullable
    private BlockPos targetLavaPos;

    /**
     * Maximum search radius for lava blocks.
     * Increased to 16 blocks to better find lava in the Nether.
     */
    private static final int SEARCH_RADIUS = 16;

    /**
     * Squared distance at which creeper should stop seeking lava and pursue target.
     * 6 blocks squared = 36. Gives players more opportunity to engage.
     */
    private static final double PURSUIT_RANGE_SQ = 36.0;

    /**
     * Creates a new go-to-lava goal.
     *
     * @param creeper The Nether Creeper entity
     * @param speedModifier Movement speed multiplier
     */
    public NetherCreeperGoToLavaGoal(NetherCreeperEntity creeper, double speedModifier) {
        this.creeper = creeper;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Checks if this goal should start executing.
     * Seeks lava when not in lava and not within pursuit range of a target.
     * Only runs on server side to prevent client/server desync.
     *
     * @return true if goal should start
     */
    @Override
    public boolean canUse() {
        // Only run on server side - block state queries must be server-authoritative
        if (this.creeper.level().isClientSide()) {
            return false;
        }

        // Don't seek lava if already in lava
        if (this.creeper.isInLava()) {
            return false;
        }

        // Check if we have a target within pursuit range
        // If target is close enough, don't seek lava - chase them instead!
        if (this.creeper.getTarget() != null) {
            double distSq = this.creeper.distanceToSqr(this.creeper.getTarget());
            if (distSq <= PURSUIT_RANGE_SQ) {
                // Target is close (within 6 blocks) - let melee/swell goals handle it
                return false;
            }
            // Target exists but is far away - prioritize getting warm
        }

        // Try to find nearby lava
        this.targetLavaPos = findNearestLava();

        // Even if no lava found, still return true - tick() will use direct movement
        return true;
    }

    /**
     * Called when the goal starts executing.
     */
    @Override
    public void start() {
        // Reset wander state
        this.wanderTicks = 0;

        if (this.targetLavaPos != null) {
            // Start moving toward the lava at urgent speed
            this.creeper.getNavigation().moveTo(
                    this.targetLavaPos.getX() + 0.5,
                    this.targetLavaPos.getY(),
                    this.targetLavaPos.getZ() + 0.5,
                    this.speedModifier * URGENT_SPEED
            );
        }
    }

    /**
     * Called when the goal stops executing.
     */
    @Override
    public void stop() {
        this.targetLavaPos = null;
        this.wanderTicks = 0;
        this.creeper.getNavigation().stop();
    }

    /**
     * Checks if this goal should continue executing.
     *
     * @return true if goal should continue
     */
    @Override
    public boolean canContinueToUse() {
        // Stop if we reached lava
        if (this.creeper.isInLava()) {
            return false;
        }

        // Stop if a target gets within pursuit range - time to chase!
        if (this.creeper.getTarget() != null) {
            double distSq = this.creeper.distanceToSqr(this.creeper.getTarget());
            if (distSq <= PURSUIT_RANGE_SQ) {
                // Target is close (within 6 blocks) - stop seeking lava and pursue
                return false;
            }
            // Target exists but is far - keep seeking lava
        }

        // Never give up - keep going as long as we're not in lava
        return true;
    }

    /**
     * Tick counter for periodic lava searches.
     */
    private int searchTicks = 0;

    /**
     * Called each tick while the goal is active.
     * Always tries to move towards lava using MoveControl.
     * Never gives up - will always try to reach lava when not in it.
     */
    @Override
    public void tick() {
        // Periodically search for lava (every 20 ticks)
        this.searchTicks++;
        if (this.targetLavaPos == null || this.searchTicks >= 20) {
            this.searchTicks = 0;
            this.targetLavaPos = findNearestLava();
        }

        // If we have a lava target, move towards it
        if (this.targetLavaPos != null) {
            // Always use MoveControl - more reliable than pathfinding for lava
            walkTowardsTarget();
        } else {
            // No lava found - walk in a random direction hoping to find some
            walkRandomDirection();
        }
    }

    /**
     * Makes the creeper walk directly towards the target lava position.
     * Uses MoveControl for proper movement instead of raw delta movement.
     */
    private void walkTowardsTarget() {
        if (this.targetLavaPos == null) {
            return;
        }

        // Use MoveControl to walk towards the target - this is more reliable
        this.creeper.getMoveControl().setWantedPosition(
                this.targetLavaPos.getX() + 0.5,
                this.targetLavaPos.getY(),
                this.targetLavaPos.getZ() + 0.5,
                this.speedModifier * URGENT_SPEED
        );

        // Face the target
        this.creeper.getLookControl().setLookAt(
                this.targetLavaPos.getX() + 0.5,
                this.targetLavaPos.getY(),
                this.targetLavaPos.getZ() + 0.5
        );
    }

    /**
     * Random direction for wandering when no lava found.
     */
    private double wanderAngle = 0;
    private int wanderTicks = 0;

    /**
     * Makes the creeper walk in a random direction when no lava is found.
     * Uses MoveControl for proper movement.
     */
    private void walkRandomDirection() {
        // Change direction every 40-80 ticks
        if (this.wanderTicks <= 0) {
            this.wanderAngle = this.creeper.getRandom().nextDouble() * Math.PI * 2;
            this.wanderTicks = WANDER_BASE_DURATION + this.creeper.getRandom().nextInt(WANDER_RANDOM_DURATION);
        }
        this.wanderTicks--;

        // Calculate target position 5 blocks in the random direction
        double targetX = this.creeper.getX() + Math.cos(this.wanderAngle) * WANDER_DISTANCE;
        double targetZ = this.creeper.getZ() + Math.sin(this.wanderAngle) * WANDER_DISTANCE;

        // Use MoveControl to walk towards the random position
        this.creeper.getMoveControl().setWantedPosition(
                targetX,
                this.creeper.getY(),
                targetZ,
                this.speedModifier * WANDER_SPEED_MODIFIER
        );
    }

    /**
     * Finds the nearest reachable lava surface within search radius.
     * If creeper is on land, prioritizes lava at lake edges (adjacent to solid ground).
     * Falls back to any surface lava if no edge lava found.
     *
     * @return The position above the nearest reachable lava, or null if none found
     */
    @Nullable
    private BlockPos findNearestLava() {
        Level level = this.creeper.level();
        BlockPos creeperPos = this.creeper.blockPosition();
        boolean creeperOnLava = this.creeper.isInLava();

        BlockPos nearestEdgeLava = null;
        double nearestEdgeDistSq = Double.MAX_VALUE;

        BlockPos nearestAnyLava = null;
        double nearestAnyDistSq = Double.MAX_VALUE;

        // Search outward in expanding rings so nearby lava is found first.
        // Once edge lava is found, we only need to finish the current ring
        // to guarantee it's the nearest, then exit early.
        for (int ring = 0; ring <= SEARCH_RADIUS; ring++) {
            // If we already found edge lava in a previous ring, no need to search further
            if (nearestEdgeLava != null) {
                break;
            }

            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // Only check blocks on the perimeter of this ring (skip interior, already checked)
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }

                    for (int dy = -3; dy <= 3; dy++) {
                        BlockPos checkPos = creeperPos.offset(dx, dy, dz);
                        FluidState fluid = level.getFluidState(checkPos);

                        // Check if this is a lava block
                        if (fluid.is(FluidTags.LAVA)) {
                            // Check if it's SURFACE lava (air above)
                            BlockPos abovePos = checkPos.above();
                            if (!level.getBlockState(abovePos).isAir()) {
                                continue; // Not surface lava
                            }

                            double distSq = creeperPos.distSqr(abovePos);

                            // Track nearest of ANY surface lava (fallback)
                            if (distSq < nearestAnyDistSq) {
                                nearestAnyDistSq = distSq;
                                nearestAnyLava = abovePos;
                            }

                            // Track nearest EDGE lava (preferred when on land)
                            if (isLavaEdge(level, checkPos) && distSq < nearestEdgeDistSq) {
                                nearestEdgeDistSq = distSq;
                                nearestEdgeLava = abovePos;
                            }
                        }
                    }
                }
            }
        }

        // If creeper is on lava, any surface lava works
        if (creeperOnLava) {
            return nearestAnyLava;
        }

        // If on land, prefer edge lava, but fall back to any lava
        return nearestEdgeLava != null ? nearestEdgeLava : nearestAnyLava;
    }

    /**
     * Checks if a lava block is at the edge of a lake (adjacent to solid walkable ground).
     * This helps find lava that can be reached from land.
     *
     * @param level The level
     * @param lavaPos The position of the lava block
     * @return true if lava is adjacent to solid ground
     */
    private boolean isLavaEdge(Level level, BlockPos lavaPos) {
        // Check all 4 horizontal neighbors
        for (BlockPos neighbor : new BlockPos[]{
                lavaPos.north(), lavaPos.south(), lavaPos.east(), lavaPos.west()}) {

            // Check if neighbor has solid ground below and air at walking level
            BlockPos groundPos = neighbor.below();
            if (level.getBlockState(groundPos).isSolid() &&
                level.getBlockState(neighbor).isAir()) {
                return true; // This lava is adjacent to walkable ground
            }

            // Also check same level - solid block next to lava
            if (level.getBlockState(neighbor).isSolid()) {
                // Check if there's air above the solid block for walking
                if (level.getBlockState(neighbor.above()).isAir()) {
                    return true;
                }
            }
        }
        return false;
    }
}
