/*
 * ============================================================================
 * NetherCreeperPathNavigation.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Provides custom pathfinding that allows the Nether Creeper to walk on lava
 * surfaces, similar to how Striders navigate in the Nether.
 *
 * WHY IT EXISTS:
 * - Standard GroundPathNavigation treats lava as an obstacle
 * - We need the creeper to path over lava like solid ground
 * - Also allows normal ground navigation when not in lava
 *
 * HOW IT WORKS:
 * - Extends GroundPathNavigation for standard ground movement
 * - Overrides key methods to treat lava surfaces as walkable
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.entity.nethercreeper;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Custom path navigation that allows walking on lava surfaces.
 *
 * This navigation system treats lava the same way normal navigation
 * treats solid ground, allowing the Nether Creeper to path across
 * lava seas in the Nether.
 */
public class NetherCreeperPathNavigation extends GroundPathNavigation {

    /**
     * Creates a new lava-walking path navigation.
     *
     * @param mob The mob using this navigation
     * @param level The level the mob is in
     */
    public NetherCreeperPathNavigation(Mob mob, Level level) {
        super(mob, level);
        // Allow floating - helps with lava surface movement
        this.setCanFloat(true);
    }

    /**
     * Checks if a path type is valid for this navigation.
     * Allows LAVA as a valid path type in addition to normal types.
     *
     * @param pathType The path type to check
     * @return true if the path type is valid
     */
    @Override
    protected boolean hasValidPathType(PathType pathType) {
        // Allow lava as valid path, plus all normal valid types
        if (pathType == PathType.LAVA) {
            return true;
        }
        // Also allow water-like path types for traversing lava
        if (pathType == PathType.WATER) {
            return true;
        }
        return super.hasValidPathType(pathType);
    }

    /**
     * Checks if a position is a stable destination for pathfinding.
     * Considers lava surface as stable, like solid ground.
     *
     * @param pos The position to check
     * @return true if the position is a stable destination
     */
    @Override
    public boolean isStableDestination(BlockPos pos) {
        // 1. Check if we are trying to stand ON TOP of a lava block.
        // We look at the block below our feet to see if it's lava.
        BlockPos below = pos.below();
        boolean isLavaBelow = this.level.getFluidState(below).is(FluidTags.LAVA);

        // 2. Make sure the spot we are standing in is AIR (so we aren't underwater/under-lava).
        boolean isAirAbove = this.level.getBlockState(pos).isAir();

        if (isLavaBelow && isAirAbove) {
            return true; // This is a perfect surface spot!
        }

        // 3. If it's not lava, let the normal Minecraft navigation rules apply
        // (This allows the Creeper to still walk on normal stone/grass).
        return super.isStableDestination(pos);
    }

    /**
     * Override to allow the mob to path through lava.
     */
    @Override
    protected boolean canUpdatePath() {
        // Allow path updates even when in lava
        return super.canUpdatePath() || this.mob.isInLava();
    }
}
