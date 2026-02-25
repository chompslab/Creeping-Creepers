/*
 * ============================================================================
 * ModItems.java
 * ============================================================================
 * 
 * WHAT THIS FILE DOES:
 * Registers all items added by the mod, primarily spawn eggs for each
 * creeper variant. Spawn eggs allow creative mode players to spawn entities.
 * 
 * WHY IT EXISTS:
 * Each entity needs a spawn egg for testing and creative mode spawning.
 * Items must be registered through Forge's deferred register system.
 * 
 * HOW TO ADD SPAWN EGGS FOR NEW VARIANTS:
 * 1. Create a new RegistryObject<Item> following the ENDER_CREEPER_SPAWN_EGG pattern
 * 2. Use SpawnEggItem with Item.Properties.spawnEgg() for your entity type
 * 3. Choose appropriate primary and secondary colors (configured in item model)
 * 4. Add a lang entry in en_us.json
 * 5. Create an item model in models/item/
 * 
 * COLOR SELECTION TIPS:
 * - Primary color: The main body/shell of the egg
 * - Secondary color: The spots on the egg
 * - Use hex color codes (0xRRGGBB format)
 * 
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.registry;

import com.creepingcreepers.CreepingCreepersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for all mod items.
 *
 * Currently contains spawn eggs for creeper variants.
 * Can be extended to include other items like special drops.
 */
public class ModItems {

    /**
     * The deferred register for items.
     */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreepingCreepersMod.MOD_ID);

    /**
     * Spawn egg for the Ender Creeper.
     *
     * Uses setId() to properly configure the item ID before construction.
     *
     * Color scheme (for reference when setting up item model):
     * - Primary (0x0C0C0C): Very dark gray/black, like Enderman skin
     * - Secondary (0x0DA70B): Bright green, like Creeper's signature color
     */
    public static final RegistryObject<Item> ENDER_CREEPER_SPAWN_EGG = ITEMS.register(
            "ender_creeper_spawn_egg",
            () -> new SpawnEggItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "ender_creeper_spawn_egg")))
                            .spawnEgg(ModEntities.ENDER_CREEPER.get())
            )
    );

    /**
     * Spawn egg for the Wither Creeper.
     *
     * Color scheme (for reference when setting up item model):
     * - Primary (0x141414): Very dark gray/black, like Wither Skeleton
     * - Secondary (0x4A4A4A): Dark gray, like Wither effect color
     */
    public static final RegistryObject<Item> WITHER_CREEPER_SPAWN_EGG = ITEMS.register(
            "wither_creeper_spawn_egg",
            () -> new SpawnEggItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "wither_creeper_spawn_egg")))
                            .spawnEgg(ModEntities.WITHER_CREEPER.get())
            )
    );

    /**
     * Spawn egg for the Nether Creeper.
     *
     * Color scheme (for reference when setting up item model):
     * - Primary (0xCC3300): Bright orange-red, like lava/magma
     * - Secondary (0x0DA70B): Bright green, like Creeper's signature color
     */
    public static final RegistryObject<Item> NETHER_CREEPER_SPAWN_EGG = ITEMS.register(
            "nether_creeper_spawn_egg",
            () -> new SpawnEggItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "nether_creeper_spawn_egg")))
                            .spawnEgg(ModEntities.NETHER_CREEPER.get())
            )
    );

    // =========================================================================
    // CREATIVE TAB
    // =========================================================================

    /**
     * Deferred register for the mod's creative mode tab.
     */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreepingCreepersMod.MOD_ID);

    /**
     * The "Creeping Creepers" creative tab.
     * Ensures the mod name shows correctly in item tooltips instead of
     * the vanilla "Spawn Egg" tab label.
     */
    public static final RegistryObject<CreativeModeTab> CREEPING_CREEPERS_TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.creepingcreepers"))
                    .icon(() -> new ItemStack(ENDER_CREEPER_SPAWN_EGG.get()))
                    .displayItems((params, output) -> {
                        output.accept(ENDER_CREEPER_SPAWN_EGG.get());
                        output.accept(WITHER_CREEPER_SPAWN_EGG.get());
                        output.accept(NETHER_CREEPER_SPAWN_EGG.get());
                    })
                    .build()
    );

    /**
     * Registers the DeferredRegisters to the mod bus group.
     * Called from the main mod class constructor.
     *
     * @param modBusGroup The mod bus group from the mod constructor
     */
    public static void register(BusGroup modBusGroup) {
        ITEMS.register(modBusGroup);
        CREATIVE_TABS.register(modBusGroup);
    }
}
