/*
 * ============================================================================
 * ModEntities.java
 * ============================================================================
 * 
 * WHAT THIS FILE DOES:
 * Registers all custom entities added by the mod using Forge's DeferredRegister.
 * This includes the Ender Creeper, Wither Creeper, and Nether Creeper.
 * 
 * WHY IT EXISTS:
 * Forge requires entities to be registered during the correct loading phase.
 * DeferredRegister handles the timing automatically, preventing crashes.
 * 
 * HOW TO ADD NEW CREEPER VARIANTS:
 * 1. Create your entity class extending AbstractVariantCreeper
 * 2. Add a new RegistryObject<EntityType<?>> following the ENDER_CREEPER pattern
 * 3. Add the entity attributes in registerAttributes()
 * 
 * FORGE REGISTRATION NOTES:
 * - EntityType.Builder configures entity properties (size, spawn category, etc.)
 * - Attributes MUST be registered separately in EntityAttributeCreationEvent
 * - The entity class constructor must match the factory signature
 * 
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.registry;

import com.creepingcreepers.CreepingCreepersMod;
import com.creepingcreepers.entity.endercreeper.EnderCreeperEntity;
import com.creepingcreepers.entity.nethercreeper.NetherCreeperEntity;
import com.creepingcreepers.entity.withercreeper.WitherCreeperEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for all mod entities.
 * 
 * Uses Forge's DeferredRegister pattern for safe, timing-independent registration.
 * All entities are registered under the "creepingcreepers" namespace.
 */
public class ModEntities {
    
    /**
     * The deferred register for entity types.
     * This queues registrations to happen at the correct time during mod loading.
     */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CreepingCreepersMod.MOD_ID);
    
    /**
     * The Ender Creeper entity type.
     * 
     * Configuration breakdown:
     * - EntityType.Builder.of(): Creates a builder with entity factory
     * - MobCategory.MONSTER: Spawns as hostile mob, affected by mob cap
     * - sized(0.6F, 1.7F): Hitbox dimensions (width, height) - same as Creeper
     * - clientTrackingRange(8): Chunk distance for client updates
     * - build(): Creates the final EntityType
     * 
     * The registry name "ender_creeper" becomes "creepingcreepers:ender_creeper"
     */
    public static final RegistryObject<EntityType<EnderCreeperEntity>> ENDER_CREEPER =
            ENTITY_TYPES.register("ender_creeper", () ->
                    EntityType.Builder.<EnderCreeperEntity>of(EnderCreeperEntity::new, MobCategory.MONSTER)
                            // Hitbox size - width 0.6 blocks, height 1.7 blocks (creeper size)
                            .sized(0.6F, 1.7F)
                            // How far away clients should track this entity (in chunks)
                            .clientTrackingRange(8)
                            // NOT fire immune - Ender Creeper takes fire/lava damage like Enderman
                            // Build with the registry key
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "ender_creeper")))
            );

    /**
     * The Wither Creeper entity type.
     *
     * Configuration breakdown:
     * - Spawns in the Nether dimension (23% spawn chance, standard monster rules)
     * - Fire immune (like all Nether mobs)
     * - Same size as vanilla Creeper
     */
    public static final RegistryObject<EntityType<WitherCreeperEntity>> WITHER_CREEPER =
            ENTITY_TYPES.register("wither_creeper", () ->
                    EntityType.Builder.<WitherCreeperEntity>of(WitherCreeperEntity::new, MobCategory.MONSTER)
                            // Hitbox size - same as vanilla Creeper
                            .sized(0.6F, 1.7F)
                            // Client tracking range
                            .clientTrackingRange(8)
                            // Fire immunity (Nether creature)
                            .fireImmune()
                            // Build with the registry key
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "wither_creeper")))
            );

    /**
     * The Nether Creeper entity type.
     *
     * Configuration breakdown:
     * - Spawns in Lava Seas in the Nether (same conditions as Strider)
     * - Fire immune and lava immune (walks on lava)
     * - Has warm/cold states affecting behavior
     * - Same size as vanilla Creeper
     */
    public static final RegistryObject<EntityType<NetherCreeperEntity>> NETHER_CREEPER =
            ENTITY_TYPES.register("nether_creeper", () ->
                    EntityType.Builder.<NetherCreeperEntity>of(NetherCreeperEntity::new, MobCategory.MONSTER)
                            // Hitbox size - same as vanilla Creeper
                            .sized(0.6F, 1.7F)
                            // Client tracking range (slightly higher for lava seas)
                            .clientTrackingRange(10)
                            // Fire immunity (Nether creature, walks on lava)
                            .fireImmune()
                            // Build with the registry key
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "nether_creeper")))
            );

    /**
     * Registers the DeferredRegister to the mod bus group.
     * Called from the main mod class constructor.
     *
     * @param modBusGroup The mod bus group from the mod constructor
     */
    public static void register(BusGroup modBusGroup) {
        ENTITY_TYPES.register(modBusGroup);
    }
    
    /**
     * Registers entity attributes for all mod entities.
     * 
     * CRITICAL: This MUST be called during EntityAttributeCreationEvent.
     * Living entities without registered attributes will crash the game!
     * 
     * This method is registered as an event listener in the main mod class.
     * 
     * @param event The attribute creation event from Forge
     */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Register Ender Creeper attributes
        // The createAttributes() method returns an AttributeSupplier.Builder
        // with all necessary attributes (health, speed, attack damage, etc.)
        event.put(ENDER_CREEPER.get(), EnderCreeperEntity.createAttributes().build());

        // Register Wither Creeper attributes
        event.put(WITHER_CREEPER.get(), WitherCreeperEntity.createAttributes().build());

        // Register Nether Creeper attributes
        event.put(NETHER_CREEPER.get(), NetherCreeperEntity.createAttributes().build());
    }
}
