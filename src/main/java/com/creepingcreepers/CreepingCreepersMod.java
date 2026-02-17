/*
 * ============================================================================
 * CreepingCreepersMod.java
 * ============================================================================
 * 
 * WHAT THIS FILE DOES:
 * This is the main entry point for the Creeping Creepers mod. It initializes
 * all mod components, registers entities, items, and event handlers.
 * 
 * WHY IT EXISTS:
 * Forge requires a main mod class annotated with @Mod to identify and load
 * the mod. This class coordinates all registrations and setup.
 * 
 * HOW TO EXTEND FOR NEW CREEPER VARIANTS:
 * 1. Create a new entity class extending AbstractVariantCreeper
 * 2. Register it in ModEntities.java
 * 3. Create a spawn egg in ModItems.java
 * 4. Add config values in CreepingCreepersConfig.java
 * 5. Create renderer and model classes in the client package
 * 
 * FORGE 1.21.x NOTES:
 * - Uses DeferredRegister for all registrations
 * - Event bus registration happens in constructor
 * - Client setup uses FMLClientSetupEvent
 * 
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers;

import com.creepingcreepers.client.ClientModEvents;
import com.creepingcreepers.config.CreepingCreepersConfig;
import com.creepingcreepers.registry.ModEntities;
import com.creepingcreepers.registry.ModItems;
import com.creepingcreepers.event.ModEventHandlers;
import com.creepingcreepers.event.ModBusEventHandlers;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

/**
 * Main mod class for Creeping Creepers.
 * 
 * This class is automatically instantiated by Forge when the mod loads.
 * The @Mod annotation tells Forge this is a mod with the ID "creepingcreepers".
 */
@Mod(CreepingCreepersMod.MOD_ID)
public class CreepingCreepersMod {
    
    /**
     * The unique identifier for this mod.
     * Used in resource locations, config files, and registry names.
     * NEVER change this after release as it will break existing worlds.
     */
    public static final String MOD_ID = "creepingcreepers";
    
    /**
     * Logger instance for debugging and error reporting.
     * Use this instead of System.out.println for proper log formatting.
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Constructor called by Forge during mod loading.
     *
     * In Forge 61.x for MC 1.21.x, the constructor receives FMLJavaModLoadingContext
     * which provides access to the mod bus group and config registration.
     *
     * This is where we:
     * 1. Get the mod bus group from context for registration
     * 2. Register all deferred registers (entities, items, etc.)
     * 3. Register configuration files via context
     * 4. Set up event handlers
     *
     * IMPORTANT: Do NOT do heavy initialization here. Use events like
     * FMLCommonSetupEvent for setup that needs other mods loaded.
     *
     * @param context The mod loading context providing bus group and config registration
     */
    public CreepingCreepersMod(FMLJavaModLoadingContext context) {
        // Log that we're starting initialization
        LOGGER.info("Creeping Creepers mod initializing...");

        // Get the mod bus group from context
        BusGroup modBusGroup = context.getModBusGroup();

        // Register all our deferred registers to the mod bus group
        // This queues our entities and items to be registered at the right time
        ModEntities.register(modBusGroup);
        ModItems.register(modBusGroup);

        // Register entity attributes - this is required for all living entities
        // Without this, the game will crash when spawning our entities
        EntityAttributeCreationEvent.BUS.addListener(ModEntities::registerAttributes);

        // Register spawn placement rules (MOD bus event)
        SpawnPlacementRegisterEvent.BUS.addListener(ModEventHandlers::onRegisterSpawnPlacements);

        // Register creative tab contents (MOD bus event)
        BuildCreativeModeTabContentsEvent.BUS.addListener(ModBusEventHandlers::onBuildCreativeTabContents);

        // Register our configuration files via context
        // COMMON config is shared between client and server
        // This makes balance values consistent in multiplayer
        context.registerConfig(ModConfig.Type.COMMON, CreepingCreepersConfig.SPEC, "creepingcreepers-common.toml");

        // NOTE: Projectile immunity is handled directly in EnderCreeperEntity.hurtServer()
        // This is more reliable than event-based handling

        // Register client-side event handlers (renderers, models, etc.)
        // Only runs on the client side to avoid server crashes
        // Pass modBusGroup for FMLClientSetupEvent registration (Lazy Holder Pattern)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModEvents.init(modBusGroup);
        }

        LOGGER.info("Creeping Creepers mod initialized successfully!");
    }
    
}
