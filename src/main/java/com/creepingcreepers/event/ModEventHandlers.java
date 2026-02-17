/*
 * ============================================================================
 * ModEventHandlers.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Contains event handlers for MOD bus events. These handle mod lifecycle
 * events like spawn placement registration.
 *
 * NOTE: Projectile immunity for Ender Creeper is handled directly in
 * EnderCreeperEntity.hurtServer() method, not via events.
 *
 * FORGE 61.x NOTES:
 * Uses functional registration in the main mod constructor for better
 * performance instead of @EventBusSubscriber annotation.
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.event;

import com.creepingcreepers.entity.endercreeper.EnderCreeperEntity;
import com.creepingcreepers.entity.nethercreeper.NetherCreeperEntity;
import com.creepingcreepers.entity.withercreeper.WitherCreeperEntity;
import com.creepingcreepers.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;

/**
 * Event handler class for MOD bus events.
 * Handlers are registered functionally in CreepingCreepersMod constructor.
 */
public class ModEventHandlers {

    /**
     * Custom spawn placement type for Nether Creeper.
     * Mimics Strider lava spawning behavior while also allowing ground spawning.
     *
     * Strider-like lava spawning checks:
     * - Position is in lava fluid
     * - Lava has surface exposure (air or more lava above)
     * - Lava is deep enough (lava below too, or solid block below)
     *
     * Also allows ground spawning near lava for variety.
     */
    private static final SpawnPlacementType IN_LAVA_LIKE_STRIDER = new SpawnPlacementType() {
        @Override
        public boolean isSpawnPositionOk(LevelReader level, BlockPos pos, EntityType<?> entityType) {
            // Check for lava spawning (Strider-like behavior)
            if (level.getFluidState(pos).is(FluidTags.LAVA)) {
                // Check lava has surface access (can reach air or is at lava surface)
                BlockPos above = pos.above();
                boolean hasSurfaceAccess = level.getBlockState(above).isAir()
                        || level.getFluidState(above).is(FluidTags.LAVA);

                if (hasSurfaceAccess) {
                    return true;
                }
            }

            // Check for spawning ON lava surface (standing on lava, like Strider)
            if (level.getBlockState(pos).isAir() && level.getFluidState(pos.below()).is(FluidTags.LAVA)) {
                return true;
            }

            // Allow ground spawning near lava (secondary option)
            if (level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), entityType)
                    && level.getBlockState(pos).isAir()) {
                return true;
            }

            return false;
        }
    };

    /**
     * Registers spawn placement rules for mod entities.
     *
     * This event is fired during mod loading and determines:
     * - What heightmap type to use for spawn position
     * - What placement type (on ground, in water, etc.)
     * - Custom spawn conditions (light level, biome, etc.)
     *
     * @param event The spawn placement register event
     */
    public static void onRegisterSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // Register Ender Creeper spawn placement
        // Uses Enderman-like spawn rules (spawns on surface, in dark)
        event.register(
                ModEntities.ENDER_CREEPER.get(),
                // Spawn on ground
                SpawnPlacementTypes.ON_GROUND,
                // Use motion blocking heightmap (solid blocks)
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                // Custom spawn predicate - checks light level from config
                EnderCreeperEntity::checkEnderCreeperSpawnRules,
                // Replace any existing registration
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );

        // Register Wither Creeper spawn placement
        // Spawns in the Nether dimension with standard monster spawn rules
        event.register(
                ModEntities.WITHER_CREEPER.get(),
                // Spawn on ground
                SpawnPlacementTypes.ON_GROUND,
                // Use motion blocking heightmap
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                // Custom spawn predicate - checks for Nether dimension and spawn chance
                WitherCreeperEntity::checkWitherCreeperSpawnRules,
                // Replace any existing registration
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );

        // Register Nether Creeper spawn placement
        // Spawns in Lava Seas (like Strider) or on solid ground in the Nether
        event.register(
                ModEntities.NETHER_CREEPER.get(),
                // Custom type: Strider-like lava spawning with ground fallback
                IN_LAVA_LIKE_STRIDER,
                // Use MOTION_BLOCKING heightmap which includes fluids (lava surfaces)
                Heightmap.Types.MOTION_BLOCKING,
                // Custom spawn predicate - checks for Nether dimension and Y level
                NetherCreeperEntity::checkNetherCreeperSpawnRules,
                // Replace any existing registration
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }

}
