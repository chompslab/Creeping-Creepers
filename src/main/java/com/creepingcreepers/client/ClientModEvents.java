/*
 * ============================================================================
 * ClientModEvents.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Handles client-side only event registration. This includes:
 * - Entity renderer registration
 * - Model layer registration
 * - Any other client-specific setup
 *
 * WHY IT EXISTS:
 * Client-side code (rendering, models, textures) must be kept separate
 * from server code. This class only runs on the client.
 *
 * IMPORTANT - FORGE 61 LAZY HOLDER PATTERN:
 * In Forge 61+, EntityRenderersEvent fires BEFORE deferred registries complete.
 * Calling RegistryObject.get() during these events causes a NullPointerException.
 *
 * Solution: Use FMLClientSetupEvent which fires AFTER registration completes.
 * Use event.enqueueWork() for thread-safe operations on the render thread.
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client;

import com.creepingcreepers.client.model.EnderCreeperModel;
import com.creepingcreepers.client.model.NetherCreeperModel;
import com.creepingcreepers.client.model.WitherCreeperModel;
import com.creepingcreepers.client.renderer.EnderCreeperRenderer;
import com.creepingcreepers.client.renderer.NetherCreeperRenderer;
import com.creepingcreepers.client.renderer.WitherCreeperRenderer;
import com.creepingcreepers.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side event handlers for rendering and model setup.
 *
 * Uses the Lazy Holder Pattern with FMLClientSetupEvent to ensure
 * deferred registries are populated before accessing RegistryObjects.
 */
public class ClientModEvents {

    /**
     * Registers all client-side event listeners.
     * Called from the main mod class constructor on the client side.
     *
     * @param modBusGroup The mod's bus group for event registration
     */
    public static void init(BusGroup modBusGroup) {
        // Register layer definitions - these don't depend on entity registration
        // and can use the static BUS directly
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(ClientModEvents::onRegisterLayerDefinitions);

        // Register for FMLClientSetupEvent which fires AFTER deferred registries complete
        // This is the Lazy Holder Pattern - we defer .get() until registries are populated
        FMLClientSetupEvent.getBus(modBusGroup).addListener(ClientModEvents::onClientSetup);
    }

    /**
     * Called during FMLClientSetupEvent - AFTER deferred registries complete.
     *
     * This is where we safely register entity renderers because the
     * RegistryObjects are now populated and .get() will succeed.
     *
     * @param event The client setup event
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        // Use enqueueWork for thread-safe registration on the render thread
        event.enqueueWork(() -> {
            // NOW it's safe to call .get() - the entities are registered
            EntityRenderers.register(ModEntities.ENDER_CREEPER.get(), EnderCreeperRenderer::new);
            EntityRenderers.register(ModEntities.WITHER_CREEPER.get(), WitherCreeperRenderer::new);
            EntityRenderers.register(ModEntities.NETHER_CREEPER.get(), NetherCreeperRenderer::new);
        });
    }

    /**
     * Registers model layers.
     *
     * Model layers define the structure of entity models.
     * This doesn't require the entity to be registered yet.
     *
     * @param event The layer definition registration event
     */
    private static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Register the Ender Creeper model layer
        event.registerLayerDefinition(EnderCreeperModel.LAYER_LOCATION, EnderCreeperModel::createBodyLayer);

        // Register the Wither Creeper model layer
        event.registerLayerDefinition(WitherCreeperModel.LAYER_LOCATION, WitherCreeperModel::createBodyLayer);

        // Register the Nether Creeper model layer
        event.registerLayerDefinition(NetherCreeperModel.LAYER_LOCATION, NetherCreeperModel::createBodyLayer);
    }
}
