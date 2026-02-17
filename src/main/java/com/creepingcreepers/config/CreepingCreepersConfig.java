/*
 * ============================================================================
 * CreepingCreepersConfig.java
 * ============================================================================
 * 
 * WHAT THIS FILE DOES:
 * Defines all configurable values for the mod using Forge's config system.
 * These values can be changed by users in the config file without recompiling.
 * 
 * WHY IT EXISTS:
 * - Allows server admins to balance the mod for their playstyle
 * - Makes testing different values easy during development
 * - Follows best practices for mod development
 * 
 * HOW TO ADD NEW CONFIG VALUES:
 * 1. Add a new ConfigValue field (use appropriate type: Int, Double, Boolean)
 * 2. Define it in the static block with BUILDER.define...()
 * 3. Access it via CreepingCreepersConfig.YOUR_VALUE.get()
 * 
 * CONFIG FILE LOCATION:
 * The config file is generated at: config/creepingcreepers-common.toml
 * 
 * FORGE CONFIG NOTES:
 * - ForgeConfigSpec.Builder creates type-safe config entries
 * - Values are automatically validated against defined ranges
 * - Changes are hot-reloaded when the file is saved
 * 
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration class containing all mod settings.
 * 
 * All values are static for easy access throughout the mod.
 * Use .get() to retrieve the current value (respects hot-reloading).
 */
public class CreepingCreepersConfig {
    
    /**
     * The config specification builder.
     * Used to define all config values with validation.
     */
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    /**
     * The completed config specification.
     * Registered with Forge in the main mod class.
     */
    public static final ForgeConfigSpec SPEC;
    
    // =========================================================================
    // ENDER CREEPER - STATS
    // =========================================================================
    // These values control the basic attributes of the Ender Creeper entity.
    // Adjusting these affects overall difficulty.
    
    /**
     * Maximum health points for Ender Creeper.
     * Vanilla Creeper has 20 HP, Enderman has 40 HP.
     * Default: 30 (between Creeper and Enderman)
     */
    public static final ForgeConfigSpec.DoubleValue ENDER_CREEPER_HEALTH;
    
    /**
     * Movement speed when walking (not teleporting).
     * Higher values make the entity faster.
     * Vanilla Creeper: 0.25, Enderman: 0.3
     * Default: 0.28
     */
    public static final ForgeConfigSpec.DoubleValue ENDER_CREEPER_SPEED;
    
    /**
     * Attack damage attribute (currently unused - melee attack is disabled).
     * The MeleeAttackGoal is used for pathfinding only; the creeper explodes instead.
     * Default: 7.0 (same as Enderman)
     */
    public static final ForgeConfigSpec.DoubleValue ENDER_CREEPER_ATTACK_DAMAGE;
    
    // =========================================================================
    // ENDER CREEPER - EXPLOSION
    // =========================================================================
    // Controls the explosion behavior and power.
    
    /**
     * Explosion radius in blocks.
     * Vanilla Creeper: 3, Charged Creeper: 6
     * Default: 4 (slightly stronger than normal)
     */
    public static final ForgeConfigSpec.IntValue ENDER_CREEPER_EXPLOSION_RADIUS;
    
    /**
     * Time in ticks before explosion after starting to swell.
     * 20 ticks = 1 second. Vanilla Creeper: 30 ticks
     * Default: 30
     */
    public static final ForgeConfigSpec.IntValue ENDER_CREEPER_FUSE_TIME;
    
    // =========================================================================
    // ENDER CREEPER - TELEPORTATION
    // =========================================================================
    // Controls teleportation mechanics inherited from Enderman.
    
    /**
     * Cooldown between teleports in ticks.
     * Prevents spam-teleporting and gives players a chance to react.
     * Default: 40 (2 seconds)
     */
    public static final ForgeConfigSpec.IntValue ENDER_CREEPER_TELEPORT_COOLDOWN;
    
    // =========================================================================
    // ENDER CREEPER - ENVIRONMENTAL
    // =========================================================================
    // Controls interactions with weather and environment.
    
    /**
     * Damage taken per tick when in rain or water.
     * Set to 0 to disable water/rain damage.
     * Default: 1.0
     */
    public static final ForgeConfigSpec.DoubleValue ENDER_CREEPER_WATER_DAMAGE;
    
    /**
     * Whether the Ender Creeper should flee from cats.
     * Matches vanilla Creeper behavior.
     * Default: true
     */
    public static final ForgeConfigSpec.BooleanValue ENDER_CREEPER_AFRAID_OF_CATS;
    
    // =========================================================================
    // ENDER CREEPER - SPAWNING
    // =========================================================================
    // Controls natural spawning conditions.

    // =========================================================================
    // WITHER CREEPER - STATS
    // =========================================================================

    /**
     * Maximum health points for Wither Creeper.
     * Default: 20 (same as vanilla Creeper and Wither Skeleton)
     */
    public static final ForgeConfigSpec.DoubleValue WITHER_CREEPER_HEALTH;

    /**
     * Movement speed when pursuing player.
     * Default: 0.25 (same as vanilla Creeper)
     */
    public static final ForgeConfigSpec.DoubleValue WITHER_CREEPER_SPEED;

    /**
     * Movement speed when fleeing from cats/ocelots.
     * Should be faster than pursuit speed.
     * Default: 1.3
     */
    public static final ForgeConfigSpec.DoubleValue WITHER_CREEPER_FLEE_SPEED;

    // =========================================================================
    // WITHER CREEPER - EXPLOSION
    // =========================================================================

    /**
     * Explosion radius in blocks.
     * Default: 3 (same as vanilla Creeper)
     */
    public static final ForgeConfigSpec.IntValue WITHER_CREEPER_EXPLOSION_RADIUS;

    /**
     * Fuse time in ticks before explosion.
     * Default: 30 (same as vanilla Creeper)
     */
    public static final ForgeConfigSpec.IntValue WITHER_CREEPER_FUSE_TIME;

    /**
     * Duration of Wither effect in ticks.
     * Default: 200 (10 seconds)
     */
    public static final ForgeConfigSpec.IntValue WITHER_CREEPER_WITHER_DURATION;

    /**
     * Wither effect amplifier (0 = Wither I).
     * Default: 0
     */
    public static final ForgeConfigSpec.IntValue WITHER_CREEPER_WITHER_AMPLIFIER;

    // =========================================================================
    // WITHER CREEPER - TARGETING
    // =========================================================================

    /**
     * Range at which the creeper flees from cats/ocelots.
     * Default: 6 blocks
     */
    public static final ForgeConfigSpec.DoubleValue WITHER_CREEPER_CAT_FEAR_RANGE;

    // =========================================================================
    // NETHER CREEPER - STATS
    // =========================================================================
    // These values control the basic attributes of the Nether Creeper entity.
    // The Nether Creeper has two states: warm (in lava) and cold (out of lava).

    /**
     * Maximum health points for Nether Creeper.
     * Default: 20 (same as vanilla Creeper)
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_HEALTH;

    /**
     * Movement speed when warm (in lava).
     * This should be similar to Strider speed in lava.
     * Default: 0.25
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_WARM_SPEED;

    /**
     * Movement speed when cold (out of lava).
     * Significantly slower than warm speed.
     * Default: 0.15
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_COLD_SPEED;

    /**
     * Movement speed when fleeing from cats/ocelots.
     * Should be faster than pursuit speed.
     * Default: 1.5
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_FLEE_SPEED;

    // =========================================================================
    // NETHER CREEPER - EXPLOSION
    // =========================================================================
    // Controls the explosion behavior. Explosion is reduced when cold.

    /**
     * Explosion radius in blocks when warm.
     * Default: 3 (same as vanilla Creeper)
     */
    public static final ForgeConfigSpec.IntValue NETHER_CREEPER_EXPLOSION_RADIUS;

    /**
     * Fuse time in ticks before explosion.
     * Default: 30 (same as vanilla Creeper)
     */
    public static final ForgeConfigSpec.IntValue NETHER_CREEPER_FUSE_TIME;

    /**
     * Multiplier for explosion radius when cold.
     * 0.2 = 80% reduction (20% of normal)
     * Default: 0.2
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_COLD_EXPLOSION_MULTIPLIER;

    // =========================================================================
    // NETHER CREEPER - TARGETING
    // =========================================================================
    // Controls how the Nether Creeper detects and chases players.

    /**
     * Detection range for players on foot.
     * Default: 16 blocks
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_PLAYER_DETECTION_RANGE;

    /**
     * Detection range for players riding a Strider.
     * Slightly shorter range than normal.
     * Default: 14 blocks
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_STRIDER_RIDER_DETECTION_RANGE;

    /**
     * Range at which the creeper flees from cats/ocelots.
     * Default: 6 blocks
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_CAT_FEAR_RANGE;

    // =========================================================================
    // NETHER CREEPER - TEMPERATURE
    // =========================================================================
    // Controls how quickly the creeper transitions between warm and cold states.

    /**
     * Damage taken per tick while cold (after delay).
     * Default: 1.0
     */
    public static final ForgeConfigSpec.DoubleValue NETHER_CREEPER_COLD_DAMAGE;

    /**
     * Delay in ticks before cold damage starts after becoming cold.
     * Default: 60 (3 seconds)
     */
    public static final ForgeConfigSpec.IntValue NETHER_CREEPER_COLD_DAMAGE_DELAY;

    /**
     * Interval in ticks between cold damage ticks.
     * Default: 20 (1 second between damage)
     */
    public static final ForgeConfigSpec.IntValue NETHER_CREEPER_COLD_DAMAGE_INTERVAL;

    // =========================================================================
    // STATIC INITIALIZATION BLOCK
    // =========================================================================
    // This is where all config values are defined with their defaults and ranges.
    
    static {
        // Push a category for organization in the config file
        BUILDER.comment("Ender Creeper Configuration")
               .comment("Adjust these values to balance the Ender Creeper to your liking.")
               .push("ender_creeper");
        
        // --- Stats Section ---
        BUILDER.comment("Base Statistics").push("stats");
        
        ENDER_CREEPER_HEALTH = BUILDER
                .comment("Maximum health points (vanilla Creeper: 20, Enderman: 40)")
                .defineInRange("health", 20.0, 1.0, 1000.0);
        
        ENDER_CREEPER_SPEED = BUILDER
                .comment("Movement speed (vanilla Creeper: 0.25, Enderman: 0.3)")
                .defineInRange("movement_speed", 0.28, 0.0, 2.0);
        
        ENDER_CREEPER_ATTACK_DAMAGE = BUILDER
                .comment("Melee attack damage (Enderman-style attack)")
                .defineInRange("attack_damage", 7.0, 0.0, 100.0);
        
        BUILDER.pop(); // End stats
        
        // --- Explosion Section ---
        BUILDER.comment("Explosion Settings").push("explosion");
        
        ENDER_CREEPER_EXPLOSION_RADIUS = BUILDER
                .comment("Explosion radius in blocks (vanilla Creeper: 3)")
                .defineInRange("explosion_radius", 3, 1, 10);
        
        ENDER_CREEPER_FUSE_TIME = BUILDER
                .comment("Fuse time in ticks before explosion (20 ticks = 1 second)")
                .defineInRange("fuse_time", 30, 1, 200);
        
        BUILDER.pop(); // End explosion
        
        // --- Teleportation Section ---
        BUILDER.comment("Teleportation Settings").push("teleportation");
        
        ENDER_CREEPER_TELEPORT_COOLDOWN = BUILDER
                .comment("Cooldown between teleports in ticks")
                .defineInRange("teleport_cooldown", 40, 0, 600);
        
        BUILDER.pop(); // End teleportation

        // --- Environmental Section ---
        BUILDER.comment("Environmental Interactions").push("environmental");
        
        ENDER_CREEPER_WATER_DAMAGE = BUILDER
                .comment("Damage per tick when in water or rain (0 to disable)")
                .defineInRange("water_damage", 1.0, 0.0, 20.0);
        
        ENDER_CREEPER_AFRAID_OF_CATS = BUILDER
                .comment("Whether the Ender Creeper flees from cats")
                .define("afraid_of_cats", true);
        
        BUILDER.pop(); // End environmental

        BUILDER.pop(); // End ender_creeper

        // =========================================================================
        // WITHER CREEPER CONFIGURATION
        // =========================================================================
        BUILDER.comment("Wither Creeper Configuration")
               .comment("A creeper that spawns in Nether Fortresses and inflicts Wither on explosion.")
               .push("wither_creeper");

        // --- Stats Section ---
        BUILDER.comment("Base Statistics").push("stats");

        WITHER_CREEPER_HEALTH = BUILDER
                .comment("Maximum health points (vanilla Creeper: 20, Wither Skeleton: 20)")
                .defineInRange("health", 20.0, 1.0, 1000.0);

        WITHER_CREEPER_SPEED = BUILDER
                .comment("Movement speed when pursuing player")
                .defineInRange("movement_speed", 0.25, 0.0, 2.0);

        WITHER_CREEPER_FLEE_SPEED = BUILDER
                .comment("Movement speed when fleeing from cats (should be faster than pursuit)")
                .defineInRange("flee_speed", 1.3, 0.0, 3.0);

        BUILDER.pop(); // End stats

        // --- Explosion Section ---
        BUILDER.comment("Explosion Settings").push("explosion");

        WITHER_CREEPER_EXPLOSION_RADIUS = BUILDER
                .comment("Explosion radius in blocks (vanilla Creeper: 3)")
                .defineInRange("explosion_radius", 3, 1, 10);

        WITHER_CREEPER_FUSE_TIME = BUILDER
                .comment("Fuse time in ticks before explosion (20 ticks = 1 second)")
                .defineInRange("fuse_time", 30, 1, 200);

        WITHER_CREEPER_WITHER_DURATION = BUILDER
                .comment("Duration of Wither effect in ticks (200 = 10 seconds)")
                .defineInRange("wither_duration", 200, 20, 1200);

        WITHER_CREEPER_WITHER_AMPLIFIER = BUILDER
                .comment("Wither effect amplifier (0 = Wither I, 1 = Wither II)")
                .defineInRange("wither_amplifier", 0, 0, 2);

        BUILDER.pop(); // End explosion

        // --- Targeting Section ---
        BUILDER.comment("Targeting Behavior").push("targeting");

        WITHER_CREEPER_CAT_FEAR_RANGE = BUILDER
                .comment("Range in blocks at which the creeper flees from cats/ocelots")
                .defineInRange("cat_fear_range", 6.0, 1.0, 32.0);

        BUILDER.pop(); // End targeting

        BUILDER.pop(); // End wither_creeper

        // =========================================================================
        // NETHER CREEPER CONFIGURATION
        // =========================================================================
        BUILDER.comment("Nether Creeper Configuration")
               .comment("A creeper that spawns in Lava Seas and walks on lava like a Strider.")
               .comment("Has warm/cold states affecting explosion power and movement speed.")
               .push("nether_creeper");

        // --- Stats Section ---
        BUILDER.comment("Base Statistics").push("stats");

        NETHER_CREEPER_HEALTH = BUILDER
                .comment("Maximum health points (vanilla Creeper: 20)")
                .defineInRange("health", 20.0, 1.0, 1000.0);

        NETHER_CREEPER_WARM_SPEED = BUILDER
                .comment("Movement speed when warm (in lava, like Strider)")
                .defineInRange("warm_speed", 0.25, 0.0, 2.0);

        NETHER_CREEPER_COLD_SPEED = BUILDER
                .comment("Movement speed when cold (out of lava, significantly slower)")
                .defineInRange("cold_speed", 0.15, 0.0, 2.0);

        NETHER_CREEPER_FLEE_SPEED = BUILDER
                .comment("Movement speed when fleeing from cats (should be faster than pursuit)")
                .defineInRange("flee_speed", 1.5, 0.0, 3.0);

        BUILDER.pop(); // End stats

        // --- Explosion Section ---
        BUILDER.comment("Explosion Settings").push("explosion");

        NETHER_CREEPER_EXPLOSION_RADIUS = BUILDER
                .comment("Explosion radius in blocks when warm (vanilla Creeper: 3)")
                .defineInRange("explosion_radius", 3, 1, 10);

        NETHER_CREEPER_FUSE_TIME = BUILDER
                .comment("Fuse time in ticks before explosion (20 ticks = 1 second)")
                .defineInRange("fuse_time", 30, 1, 200);

        NETHER_CREEPER_COLD_EXPLOSION_MULTIPLIER = BUILDER
                .comment("Explosion radius multiplier when cold (0.2 = 80% reduction)")
                .defineInRange("cold_explosion_multiplier", 0.2, 0.0, 1.0);

        BUILDER.pop(); // End explosion

        // --- Targeting Section ---
        BUILDER.comment("Targeting Behavior").push("targeting");

        NETHER_CREEPER_PLAYER_DETECTION_RANGE = BUILDER
                .comment("Range in blocks at which the creeper detects players on foot")
                .defineInRange("player_detection_range", 16.0, 1.0, 64.0);

        NETHER_CREEPER_STRIDER_RIDER_DETECTION_RANGE = BUILDER
                .comment("Range in blocks at which the creeper detects players riding Striders")
                .defineInRange("strider_rider_detection_range", 14.0, 1.0, 64.0);

        NETHER_CREEPER_CAT_FEAR_RANGE = BUILDER
                .comment("Range in blocks at which the creeper flees from cats/ocelots")
                .defineInRange("cat_fear_range", 6.0, 1.0, 32.0);

        BUILDER.pop(); // End targeting

        // --- Temperature Section ---
        BUILDER.comment("Temperature State").push("temperature");

        NETHER_CREEPER_COLD_DAMAGE = BUILDER
                .comment("Damage taken per tick while cold (after delay)")
                .defineInRange("cold_damage", 1.0, 0.0, 20.0);

        NETHER_CREEPER_COLD_DAMAGE_DELAY = BUILDER
                .comment("Delay in ticks before cold damage starts after becoming cold (60 = 3 seconds)")
                .defineInRange("cold_damage_delay", 60, 0, 600);

        NETHER_CREEPER_COLD_DAMAGE_INTERVAL = BUILDER
                .comment("Interval in ticks between cold damage ticks (20 = 1 second)")
                .defineInRange("cold_damage_interval", 20, 1, 200);

        BUILDER.pop(); // End temperature

        BUILDER.pop(); // End nether_creeper

        // Build the final specification
        SPEC = BUILDER.build();
    }
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static members.
     */
    private CreepingCreepersConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
