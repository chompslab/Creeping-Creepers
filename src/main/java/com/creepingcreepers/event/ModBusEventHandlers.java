/*
 * ============================================================================
 * ModBusEventHandlers.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Contains event handlers for MOD bus events. These handle mod lifecycle
 * events like creative tab registration.
 *
 * FORGE 61.x NOTES:
 * Uses functional registration in the main mod constructor for better
 * performance instead of @EventBusSubscriber annotation.
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.event;

import com.creepingcreepers.registry.ModItems;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Event handler class for MOD bus events.
 * Handlers are registered functionally in CreepingCreepersMod constructor.
 */
public class ModBusEventHandlers {

    /**
     * Adds items to creative mode tabs.
     *
     * In 1.20+, creative tabs are event-based rather than item properties.
     * This adds our spawn eggs to the appropriate tabs.
     *
     * @param event The creative tab contents event
     */
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        // Add to spawn eggs tab
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.ENDER_CREEPER_SPAWN_EGG.get());
            event.accept(ModItems.WITHER_CREEPER_SPAWN_EGG.get());
            event.accept(ModItems.NETHER_CREEPER_SPAWN_EGG.get());
        }
    }
}
