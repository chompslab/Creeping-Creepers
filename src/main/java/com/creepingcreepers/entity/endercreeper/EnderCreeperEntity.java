/*
 * ============================================================================
 * EnderCreeperEntity.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Implements the Ender Creeper - a hybrid mob combining Creeper explosion
 * mechanics with Enderman teleportation behavior.
 *
 * WHY IT EXISTS:
 * One of three Creeper variants in the mod (alongside Wither and Nether).
 * It demonstrates how to extend AbstractVariantCreeper and add unique abilities.
 *
 * BEHAVIOR SUMMARY:
 * - Idle behavior exactly like Enderman (random teleportation, no block pickup)
 * - Teleportation uses exact Enderman logic (64x64x64 cube, 32 blocks each axis)
 * - Teleport sound ONLY plays at destination
 * - Targets players within 16 blocks (14 when crouching, like standard Creeper)
 * - Provoked when attacked by player or mob
 * - Teleports when hit by projectiles (avoids projectile damage, except splash water)
 * - Once engaged, teleports within 12 blocks of target (not if within 11 blocks)
 * - Cannot teleport inside solid blocks
 * - Explodes when close to target (like Creeper)
 * - Explosion continues if target within 7 blocks, cancels if further (re-engages quickly)
 * - NO line of sight requirement (like vanilla creeper)
 * - Explosion uses mob_portal particles
 * - Explosion teleports victims using Chorus Fruit logic (±8 blocks, 16 attempts)
 * - Takes damage from melee attacks, water, rain, lava, fire, splash water bottles
 * - Prevents players from sleeping nearby
 * - Afraid of cats and ocelots (flee within 6 blocks, faster than pursuit speed)
 *
 * HOW TO USE AS A TEMPLATE:
 * 1. Copy this file and rename for your variant
 * 2. Change the class name and update imports
 * 3. Modify registerGoals() for different AI
 * 4. Override createCustomExplosionEffects() for different effects
 * 5. Update attribute values in createAttributes()
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.entity.endercreeper;

import com.creepingcreepers.ai.goal.EnderCreeperSwellGoal;
import com.creepingcreepers.ai.goal.IdleRandomTeleportGoal;
import com.creepingcreepers.ai.goal.TeleportToTargetGoal;
import com.creepingcreepers.config.CreepingCreepersConfig;
import com.creepingcreepers.entity.base.AbstractVariantCreeper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Ender Creeper entity.
 * A terrifying hybrid that combines the explosive nature of Creepers
 * with the teleportation abilities of Endermen.
 */
public class EnderCreeperEntity extends AbstractVariantCreeper {
    
    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** Portal particles spawned per tick while idle (Enderman-like ambiance). */
    private static final int IDLE_PARTICLE_COUNT = 2;

    /** Reverse portal particles at explosion center. */
    private static final int EXPLOSION_PARTICLE_COUNT = 128;

    /** Reverse portal particles at teleport origin/destination. */
    private static final int TELEPORT_PARTICLE_COUNT = 32;

    /** Ticks between environmental damage teleports (prevents lag from spam). */
    private static final int ENVIRONMENTAL_TELEPORT_COOLDOWN = 5;

    /** Fire duration when touching lava, in ticks (5 seconds). */
    private static final int LAVA_FIRE_DURATION = 100;

    /** Max attempts to find a valid engage teleport position. */
    private static final int ENGAGE_TELEPORT_ATTEMPTS = 10;

    /** Closest distance to teleport near target during engagement. */
    private static final double ENGAGE_TELEPORT_MIN_DISTANCE = 3.0;

    /** Max attempts to find a valid dodge teleport position. */
    private static final int DODGE_TELEPORT_ATTEMPTS = 32;

    /** Minimum dodge teleport distance in blocks. */
    private static final double DODGE_MIN_RANGE = 8.0;

    /** Random additional dodge teleport distance in blocks (total: 8-16). */
    private static final double DODGE_RANDOM_RANGE = 8.0;

    /** Minimum damage required to trigger hurt-teleport. */
    private static final float HURT_TELEPORT_DAMAGE_THRESHOLD = 1.0F;

    /** Squared distance for sleep prevention (8 blocks squared = 64). */
    private static final double SLEEP_PREVENTION_RANGE_SQ = 64.0;

    /** Spawn chance in the Nether dimension (10%). */
    private static final double SPAWN_CHANCE_NETHER = 0.10;

    /** Spawn chance in the End dimension (20%). */
    private static final double SPAWN_CHANCE_END = 0.20;

    /** Spawn chance in Overworld — no gate, same as Enderman (weight controls rarity). */

    /** Range to flee from cats and ocelots in blocks. */
    private static final float CAT_FEAR_RANGE = 6.0F;

    /** Walk speed when fleeing from cats. */
    private static final double CAT_FLEE_WALK_SPEED = 1.3;

    /** Sprint speed when fleeing from cats. */
    private static final double CAT_FLEE_SPRINT_SPEED = 1.5;

    /** Idle wander movement speed. */
    private static final double WANDER_SPEED = 0.8;

    /** Range to look at nearby players in blocks. */
    private static final float LOOK_AT_PLAYER_RANGE = 8.0F;

    /** Player detection range when crouching in blocks. */
    private static final double DETECTION_RANGE_CROUCHING = 14.0;

    /** Normal player detection range in blocks. */
    private static final double DETECTION_RANGE_NORMAL = 16.0;

    /** Walk ticks required before allowing next teleport. */
    private static final int MIN_WALK_TICKS_BEFORE_TELEPORT = 40;

    /** Maximum teleport range for random/idle teleportation.
     * Uses exact Enderman logic: 32 blocks along each axis (64x64x64 cube). */
    private static final int ENDERMAN_TELEPORT_RANGE = 32;

    /** Maximum teleport range for Chorus Fruit-style teleportation (explosion victims). */
    private static final double CHORUS_TELEPORT_RANGE = 8.0;

    /** Maximum attempts for Chorus Fruit-style teleportation. */
    private static final int CHORUS_TELEPORT_ATTEMPTS = 16;

    /** Range at which the Ender Creeper will teleport to engage a target. */
    public static final double ENGAGE_TELEPORT_MAX_RANGE = 14.0;

    /** Minimum distance at which the Ender Creeper will NOT teleport toward target. */
    public static final double ENGAGE_TELEPORT_MIN_RANGE = 13.0;

    // =========================================================================
    // INSTANCE FIELDS
    // =========================================================================

    /**
     * Cooldown timer for teleportation (in ticks).
     * Prevents spam-teleporting and gives players a chance to react.
     */
    private int teleportCooldown = 0;

    /**
     * Tracks whether the entity has teleported since last starting to walk.
     * Used to alternate between walking and teleporting like Enderman.
     */
    private boolean hasWalkedSinceTeleport = false;

    /**
     * Counter for walking ticks after teleporting.
     * Ensures the entity walks a bit before teleporting again.
     */
    private int walkingTicks = 0;

    /**
     * Cached water/environmental damage value from config.
     * Cached at construction to avoid per-tick config lookup overhead.
     */
    private final float cachedWaterDamage;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Creates a new Ender Creeper.
     *
     * @param entityType The registered entity type
     * @param level The world this entity exists in
     */
    public EnderCreeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // Set fuse time from config
        this.maxSwell = CreepingCreepersConfig.ENDER_CREEPER_FUSE_TIME.get();
        // Cache config values used in per-tick hot paths
        this.cachedWaterDamage = CreepingCreepersConfig.ENDER_CREEPER_WATER_DAMAGE.get().floatValue();

        // Set pathfinding penalties - avoid water, lava, and fire like Enderman
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }
    
    // =========================================================================
    // ATTRIBUTE CREATION
    // =========================================================================
    
    /**
     * Creates the attribute supplier for Ender Creepers.
     * Uses default values since config isn't loaded at registration time.
     * Config values are used at runtime for other calculations.
     *
     * @return Builder with all entity attributes
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)  // Default from config
                .add(Attributes.MOVEMENT_SPEED, 0.28)  // Default from config
                .add(Attributes.ATTACK_DAMAGE, 7.0)  // Default from config
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }
    
    // =========================================================================
    // AI GOALS REGISTRATION
    // =========================================================================

    /**
     * Registers all AI goals for the Ender Creeper.
     *
     * Goals are processed in priority order (lower number = higher priority).
     * The entity will attempt to execute the highest priority goal that can run.
     *
     * GOAL PRIORITY BREAKDOWN:
     * 1 - Swell (explode) when close to target - highest priority
     * 2 - Flee from water/lava/fire - survival instinct
     * 3 - Avoid cats/ocelots - inherited Creeper fear
     * 4 - Teleport to target - repositioning (only when > 12 blocks)
     * 5 - Move toward target - standard creeper chase behavior
     * 6 - Wander around - idle behavior
     * 7 - Idle random teleportation - Enderman-like passive teleporting
     * 8 - Look at players - awareness
     * 9 - Random look around - idle animation
     *
     * TARGET GOALS:
     * 1 - Retaliate when hurt by any mob
     * 2 - Target players within range (16 blocks, 14 when crouching)
     */
    @Override
    protected void registerGoals() {
        // === BEHAVIOR GOALS ===

        // Priority 1: Start swelling when close enough to target
        this.goalSelector.addGoal(1, new EnderCreeperSwellGoal(this));

        // Priority 2: Escape from water/lava/fire (they damage us)
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.5) {
            @Override
            protected boolean shouldPanic() {
                return this.mob.isOnFire() || this.mob.isInWater() || this.mob.isInLava();
            }
        });

        // Priority 3: Run away from cats and ocelots (if enabled in config)
        // Flee within 6-block radius with FASTER movement than when pursuing a player
        // Walk speed: 1.3 (vs 0.8 normal wander), Sprint speed: 1.5 (faster than pursuit)
        if (CreepingCreepersConfig.ENDER_CREEPER_AFRAID_OF_CATS.get()) {
            this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, CAT_FEAR_RANGE, CAT_FLEE_WALK_SPEED, CAT_FLEE_SPRINT_SPEED));
            this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, CAT_FEAR_RANGE, CAT_FLEE_WALK_SPEED, CAT_FLEE_SPRINT_SPEED));
        }

        // Priority 4: Teleport toward target when farther than 12 blocks
        this.goalSelector.addGoal(4, new TeleportToTargetGoal(this));

        // Priority 5: Move toward target (standard creeper chase behavior)
        // This makes the creeper walk/run towards the player after teleporting
        // or when already within engagement range
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, false) {
            @Override
            protected void checkAndPerformAttack(LivingEntity target) {
                // Don't actually melee attack - creepers explode instead
                // The swell goal handles the explosion
            }
        });

        // Priority 6: Wander around when idle
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, WANDER_SPEED, 0.0F));

        // Priority 7: Idle random teleportation (Enderman-like behavior when no target)
        // No movement flags — runs concurrently with wander and look-around goals
        this.goalSelector.addGoal(7, new IdleRandomTeleportGoal(this));

        // Priority 8: Look at nearby players
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, LOOK_AT_PLAYER_RANGE));

        // Priority 9: Random looking around
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // === TARGETING GOALS ===

        // Target whatever hurt us (retaliation)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // Target players within range (standard creeper behavior)
        // 16 blocks normally, 14 blocks when player is crouching
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            protected double getFollowDistance() {
                Player target = (Player) this.target;
                if (target != null && target.isCrouching()) {
                    return DETECTION_RANGE_CROUCHING;
                }
                return DETECTION_RANGE_NORMAL;
            }
        });
    }
    
    // =========================================================================
    // TICK AND UPDATE LOGIC
    // =========================================================================
    
    /**
     * Called every game tick. Handles:
     * - Teleport cooldown
     * - Water/rain/lava/fire damage
     * - Anger timer
     * - Walking/teleporting alternation
     */
    @Override
    public void tick() {
        // Decrement teleport cooldown
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }

        // Handle environmental damage (water, rain, lava, fire)
        if (this.level() instanceof ServerLevel) {
            this.handleEnvironmentalDamage();
        }

        // Track walking after teleporting
        if (this.hasWalkedSinceTeleport && this.getDeltaMovement().horizontalDistanceSqr() > 0.0001) {
            this.walkingTicks++;
        }

        super.tick();
    }

    /**
     * Called every tick to handle AI and other per-tick logic.
     * Spawns reverse portal particles around the entity like Endermen do.
     */
    @Override
    public void aiStep() {
        // Spawn reverse portal particles on client side (like Enderman)
        if (this.level().isClientSide()) {
            for (int i = 0; i < IDLE_PARTICLE_COUNT; ++i) {
                this.level().addParticle(
                        ParticleTypes.PORTAL,
                        this.getRandomX(0.5),
                        this.getRandomY() - 0.25,
                        this.getRandomZ(0.5),
                        (this.random.nextDouble() - 0.5) * 1.0,
                        -this.random.nextDouble(),
                        (this.random.nextDouble() - 0.5) * 1.0
                );
            }
        }

        super.aiStep();
    }

    /**
     * Returns whether this entity is sensitive to water.
     * When true, the entity takes damage from:
     * - Being in water
     * - Rain
     * - Splash water bottles
     *
     * This is what makes splash water bottles damage the Ender Creeper.
     *
     * @return true - Ender Creepers are hurt by water like Endermen
     */
    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    /**
     * Handles damage from water, rain, lava, and fire.
     * Ender Creepers take damage from all of these, like Endermen with water.
     * When in water or lava blocks, they take damage AND immediately teleport away.
     */
    private void handleEnvironmentalDamage() {
        float damage = this.cachedWaterDamage;

        // Skip all checks if damage is disabled
        if (damage <= 0) {
            return;
        }

        // Check for water contact (includes bubble columns)
        boolean isInWater = this.isInWaterOrSwimmable();

        // Check for lava contact
        boolean isInLava = this.isInLava();

        // Check for fire (single block state lookup, check both fire types)
        boolean isInFire;
        if (this.isOnFire()) {
            isInFire = true;
        } else {
            BlockState blockAtPos = this.level().getBlockState(this.blockPosition());
            isInFire = blockAtPos.is(Blocks.FIRE) || blockAtPos.is(Blocks.SOUL_FIRE);
        }

        // Check for rain (skip if already in water, since water damage handles it)
        boolean isInRain = !isInWater && this.level().isRainingAt(this.blockPosition());

        // Handle water block damage - take damage and teleport away (with cooldown to prevent lag)
        if (isInWater) {
            this.hurt(this.damageSources().drown(), damage);
            if (this.teleportCooldown <= 0) {
                this.teleportToAvoidProjectile();
                this.teleportCooldown = ENVIRONMENTAL_TELEPORT_COOLDOWN;
            }
        }

        // Handle lava block damage - take damage, catch fire, and teleport away (with cooldown)
        // Similar to Enderman behavior with lava
        if (isInLava) {
            this.hurt(this.damageSources().lava(), damage);
            this.setRemainingFireTicks(LAVA_FIRE_DURATION);
            if (this.teleportCooldown <= 0) {
                this.teleportToAvoidProjectile();
                this.teleportCooldown = ENVIRONMENTAL_TELEPORT_COOLDOWN;
            }
        }

        // Handle rain damage - take damage, try to teleport if possible
        if (isInRain) {
            this.hurt(this.damageSources().drown(), damage);
            if (this.canTeleport()) {
                this.teleportRandomly();
            }
        }

        // Handle fire damage - take damage, try to teleport if possible
        if (isInFire) {
            this.hurt(this.damageSources().onFire(), damage);
            if (this.canTeleport()) {
                this.teleportRandomly();
            }
        }
    }
    
    // =========================================================================
    // EXPLOSION BEHAVIOR
    // =========================================================================
    
    /**
     * Gets the explosion radius from config.
     * Default is 4.0 (vanilla creeper is 3.0).
     *
     * @return Explosion radius in blocks
     */
    @Override
    protected float getExplosionRadius() {
        // Get explosion radius from config (default 4, vanilla is 3)
        return CreepingCreepersConfig.ENDER_CREEPER_EXPLOSION_RADIUS.get().floatValue();
    }

    /**
     * Creates custom explosion effects - teleports affected entities Chorus Fruit-style.
     *
     * This is called right after the explosion damage is applied.
     * - Spawns mob_portal (REVERSE_PORTAL) particles at explosion center
     * - Entities in the explosion radius are teleported randomly (±8 blocks, up to 16 attempts)
     * - Does NOT apply dragon's breath effect (removed from original design)
     */
    @Override
    protected void createCustomExplosionEffects() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Store explosion position
        double explosionX = this.getX();
        double explosionY = this.getY();
        double explosionZ = this.getZ();

        // Spawn mob_portal particles (REVERSE_PORTAL) at explosion center
        // Batched into a single packet using count parameter for network efficiency
        serverLevel.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                explosionX,
                explosionY + 1.5,
                explosionZ,
                EXPLOSION_PARTICLE_COUNT, 2.0, 1.5, 2.0, 0.15
        );

        // Find all living entities in explosion radius
        // Use larger search area (radius * 2) to catch all entities affected by explosion knockback
        float radius = this.getExplosionRadius();
        float searchRadius = radius * 2.0F;
        AABB explosionBox = new AABB(
                explosionX - searchRadius, explosionY - searchRadius, explosionZ - searchRadius,
                explosionX + searchRadius, explosionY + searchRadius, explosionZ + searchRadius
        );

        List<LivingEntity> affectedEntities = serverLevel.getEntitiesOfClass(LivingEntity.class, explosionBox,
                entity -> entity != this && entity.isAlive());

        for (LivingEntity entity : affectedEntities) {
            // Teleport entity using Chorus Fruit-style logic
            this.teleportEntityChorusFruitStyle(entity, serverLevel);
        }
    }

    /**
     * Teleports an entity using Chorus Fruit-style logic.
     * Up to 16 attempts to find a valid position within ±8 blocks on all axes.
     *
     * @param entity The entity to teleport
     * @param serverLevel The server level
     */
    private void teleportEntityChorusFruitStyle(LivingEntity entity, ServerLevel serverLevel) {
        double startX = entity.getX();
        double startY = entity.getY();
        double startZ = entity.getZ();

        for (int attempt = 0; attempt < CHORUS_TELEPORT_ATTEMPTS; attempt++) {
            // Random position within ±8 blocks
            double targetX = startX + (this.random.nextDouble() - 0.5) * CHORUS_TELEPORT_RANGE * 2.0;
            double targetY = startY + (this.random.nextDouble() - 0.5) * CHORUS_TELEPORT_RANGE * 2.0;
            double targetZ = startZ + (this.random.nextDouble() - 0.5) * CHORUS_TELEPORT_RANGE * 2.0;

            // Clamp Y to valid world bounds
            targetY = Mth.clamp(targetY, serverLevel.getMinY(), serverLevel.getMaxY() - 1);

            // Find ground at target position
            BlockPos targetPos = BlockPos.containing(targetX, targetY, targetZ);

            // Search downward for solid ground
            while (targetPos.getY() > serverLevel.getMinY() &&
                    !serverLevel.getBlockState(targetPos.below()).isSolid()) {
                targetPos = targetPos.below();
            }

            // Check if the position is valid (not in solid block, not in water/lava)
            BlockState blockBelow = serverLevel.getBlockState(targetPos.below());
            BlockState blockAt = serverLevel.getBlockState(targetPos);
            BlockState blockAbove = serverLevel.getBlockState(targetPos.above());

            if (!blockBelow.isSolid()) {
                continue; // No ground found
            }

            // Check for sufficient space (2 blocks for standing, 1 for crawling)
            boolean hasSpace = !blockAt.isSolid() && !blockAbove.isSolid();
            boolean isCrawling = entity instanceof Player player && player.isShiftKeyDown();
            if (isCrawling) {
                hasSpace = !blockAt.isSolid();
            }

            if (!hasSpace) {
                continue;
            }

            // Check not teleporting into water or lava
            if (blockAt.getFluidState().is(FluidTags.WATER) ||
                    blockAt.getFluidState().is(FluidTags.LAVA)) {
                continue;
            }

            // Valid position found - teleport!
            double finalX = targetPos.getX() + 0.5;
            double finalY = targetPos.getY();
            double finalZ = targetPos.getZ() + 0.5;

            // Fire Forge EntityTeleportEvent.ChorusFruit — allows protection mods
            // (FTB Chunks, GriefPrevention, etc.) to cancel or modify the teleport
            // Use BUS.post() directly because .fire() doesn't expose cancellation state
            EntityTeleportEvent.ChorusFruit chorusEvent =
                    new EntityTeleportEvent.ChorusFruit(entity, finalX, finalY, finalZ);
            if (EntityTeleportEvent.ChorusFruit.BUS.post(chorusEvent)) {
                continue; // Event was cancelled — try next position
            }
            // Respect any coordinate modifications from event listeners
            finalX = chorusEvent.getTargetX();
            finalY = chorusEvent.getTargetY();
            finalZ = chorusEvent.getTargetZ();

            // Spawn particles at old position (batched into single packet)
            serverLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    startX, startY + 1.0, startZ,
                    TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
            );

            // Teleport entity
            entity.teleportTo(finalX, finalY, finalZ);

            // Play teleport sound at both locations
            serverLevel.playSound(null, startX, startY, startZ,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, entity.getSoundSource(), 1.0F, 1.0F);
            serverLevel.playSound(null, finalX, finalY, finalZ,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, entity.getSoundSource(), 1.0F, 1.0F);

            // Spawn particles at new position (batched into single packet)
            serverLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    finalX, finalY + 1.0, finalZ,
                    TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
            );

            // Fire game event
            entity.gameEvent(GameEvent.TELEPORT);
            return;
        }

        // If all attempts failed, teleport directly down to ground at current position
        BlockPos groundPos = entity.blockPosition();
        while (groundPos.getY() > serverLevel.getMinY() &&
                !serverLevel.getBlockState(groundPos.below()).isSolid()) {
            groundPos = groundPos.below();
        }

        if (groundPos.getY() != entity.blockPosition().getY()) {
            double fallbackX = entity.getX();
            double fallbackY = groundPos.getY();
            double fallbackZ = entity.getZ();

            // Fire Forge EntityTeleportEvent.ChorusFruit for the fallback teleport too
            // Use BUS.post() directly because .fire() doesn't expose cancellation state
            EntityTeleportEvent.ChorusFruit fallbackEvent =
                    new EntityTeleportEvent.ChorusFruit(entity, fallbackX, fallbackY, fallbackZ);
            if (!EntityTeleportEvent.ChorusFruit.BUS.post(fallbackEvent)) {
                fallbackX = fallbackEvent.getTargetX();
                fallbackY = fallbackEvent.getTargetY();
                fallbackZ = fallbackEvent.getTargetZ();

                entity.teleportTo(fallbackX, fallbackY, fallbackZ);

                // Play teleport sound
                serverLevel.playSound(null, fallbackX, fallbackY, fallbackZ,
                        SoundEvents.CHORUS_FRUIT_TELEPORT, entity.getSoundSource(), 1.0F, 1.0F);
            }
        }
    }
    
    // =========================================================================
    // TELEPORTATION LOGIC
    // =========================================================================
    
    /**
     * Checks if the entity can currently teleport.
     * 
     * @return true if teleportation is allowed
     */
    public boolean canTeleport() {
        return this.teleportCooldown <= 0 && this.onGround();
    }
    
    /**
     * Checks if the entity should teleport rather than walk.
     * Alternates between walking and teleporting like Enderman.
     * 
     * @return true if should teleport
     */
    public boolean shouldTeleport() {
        // If we haven't walked since last teleport, keep walking
        if (!this.hasWalkedSinceTeleport) {
            return false;
        }
        
        // Need to walk at least 40 ticks before teleporting again
        return this.walkingTicks >= MIN_WALK_TICKS_BEFORE_TELEPORT;
    }
    
    /**
     * Attempts to teleport to a random nearby location using exact Enderman logic.
     *
     * Teleportation chooses a random destination 32 blocks along each axis
     * (i.e. a 64×64×64 cube centered on the current position).
     *
     * Used for idle teleportation and escaping environmental damage.
     *
     * @return true if teleportation succeeded
     */
    public boolean teleportRandomly() {
        if (!this.level().isClientSide() && this.isAlive()) {
            // Exact Enderman logic: random position within ±32 blocks on each axis
            double targetX = this.getX() + (this.random.nextDouble() - 0.5) * ENDERMAN_TELEPORT_RANGE * 2.0;
            double targetY = this.getY() + (this.random.nextInt(ENDERMAN_TELEPORT_RANGE * 2 + 1) - ENDERMAN_TELEPORT_RANGE);
            double targetZ = this.getZ() + (this.random.nextDouble() - 0.5) * ENDERMAN_TELEPORT_RANGE * 2.0;
            return this.tryTeleportToEndermanStyle(targetX, targetY, targetZ);
        }
        return false;
    }
    
    /**
     * Attempts to teleport toward a specific entity for engagement.
     *
     * Teleports within 12 blocks of the target. Will NOT teleport if already
     * within 11 blocks of the target. Cannot teleport inside solid blocks.
     *
     * @param target The entity to teleport toward
     * @return true if teleportation succeeded
     */
    public boolean teleportTowardEntity(Entity target) {
        if (this.level().isClientSide() || !this.isAlive()) {
            return false;
        }
        if (!this.canTeleport()) {
            return false;
        }

        // Check if already within minimum range - don't teleport
        double currentDistance = this.distanceTo(target);
        if (currentDistance <= ENGAGE_TELEPORT_MIN_RANGE) {
            return false;
        }

        // Teleport to within 12 blocks of target
        // Target distance: between 3-11 blocks from target (to stay just outside the 11-block no-teleport zone)
        double minTeleportDist = ENGAGE_TELEPORT_MIN_DISTANCE;
        double maxTeleportDist = ENGAGE_TELEPORT_MIN_RANGE - 0.5;
        double teleportDistance = minTeleportDist + this.random.nextDouble() * (maxTeleportDist - minTeleportDist);

        // Try multiple positions around the target
        for (int attempt = 0; attempt < ENGAGE_TELEPORT_ATTEMPTS; attempt++) {
            // Random angle around the target
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double targetX = target.getX() + Math.cos(angle) * teleportDistance;
            double targetY = target.getY();
            double targetZ = target.getZ() + Math.sin(angle) * teleportDistance;

            if (this.tryTeleportToEndermanStyle(targetX, targetY, targetZ)) {
                return true;
            }
        }

        return false;
    }
    
    /**
     * Attempts teleportation using EXACT Enderman logic.
     *
     * Enderman teleportation validation:
     * 1. Starting from selected target, seek downward while block is not movement-blocking
     * 2. If the found block is waterlogged, teleportation fails
     * 3. Starting again from originally selected target, seek downward while block BELOW is not movement-blocking
     * 4. Teleportation succeeds if no liquid or solid blocks prevent standing at destination
     * 5. Endermen need at least 3 non-solid blocks above destination to successfully teleport
     *
     * Sound plays ONLY at the destination (not at origin).
     *
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @return true if teleportation succeeded
     */
    public boolean tryTeleportToEndermanStyle(double x, double y, double z) {
        if (this.level().isClientSide() || !this.isAlive()) {
            return false;
        }
        Level level = this.level();
        BlockPos targetPos = BlockPos.containing(x, y, z);

        // Clamp Y to valid world bounds
        int targetY = Mth.clamp(targetPos.getY(), level.getMinY(), level.getMaxY() - 1);
        targetPos = new BlockPos(targetPos.getX(), targetY, targetPos.getZ());

        // === STEP 1: Seek downward while block is NOT movement-blocking ===
        BlockPos seekPos = targetPos;
        while (seekPos.getY() > level.getMinY()) {
            BlockState state = level.getBlockState(seekPos);
            if (state.blocksMotion()) {
                break;
            }
            seekPos = seekPos.below();
        }

        // === STEP 2: If found block is waterlogged, teleportation fails ===
        BlockState foundState = level.getBlockState(seekPos);
        if (foundState.getFluidState().is(FluidTags.WATER)) {
            return false;
        }

        // === STEP 3: Starting from original target, seek downward while block BELOW is not movement-blocking ===
        BlockPos landingPos = targetPos;
        while (landingPos.getY() > level.getMinY()) {
            BlockState stateBelow = level.getBlockState(landingPos.below());
            if (stateBelow.blocksMotion()) {
                break;
            }
            landingPos = landingPos.below();
        }

        // === STEP 4: Check if we can stand at the landing position ===
        // Check the block at landing position and 2 blocks above (need 3 non-solid blocks total)
        BlockState atLanding = level.getBlockState(landingPos);
        BlockState above1 = level.getBlockState(landingPos.above());
        BlockState above2 = level.getBlockState(landingPos.above(2));

        // Check for liquids at landing position
        if (!atLanding.getFluidState().isEmpty() || !above1.getFluidState().isEmpty()) {
            return false;
        }

        // Check for solid blocks preventing standing (need space for entity)
        if (atLanding.blocksMotion() || above1.blocksMotion() || above2.blocksMotion()) {
            return false;
        }

        // Check that there's solid ground below the landing position
        BlockState groundState = level.getBlockState(landingPos.below());
        if (!groundState.blocksMotion()) {
            return false;
        }

        // Also reject fire blocks
        if (atLanding.is(Blocks.FIRE) || atLanding.is(Blocks.SOUL_FIRE)) {
            return false;
        }

        // === TELEPORTATION SUCCESS ===
        // Store old position for particles only (sound plays ONLY at destination)
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();

        // Compute teleport destination
        double finalX = landingPos.getX() + 0.5;
        double finalY = landingPos.getY();
        double finalZ = landingPos.getZ() + 0.5;

        // Fire Forge EntityTeleportEvent.EnderEntity — allows protection mods
        // (FTB Chunks, GriefPrevention, etc.) to cancel or modify the teleport
        EntityTeleportEvent.EnderEntity teleportEvent =
                ForgeEventFactory.onEnderManTeleport(this, finalX, finalY, finalZ);
        if (teleportEvent == null) {
            return false; // Event was cancelled by a protection mod
        }
        // Respect any coordinate modifications from event listeners
        finalX = teleportEvent.getTargetX();
        finalY = teleportEvent.getTargetY();
        finalZ = teleportEvent.getTargetZ();

        // Perform teleportation
        this.teleportTo(finalX, finalY, finalZ);

        // Reset navigation
        this.getNavigation().stop();

        // Play teleport effects (batched into single packets for network efficiency)
        if (level instanceof ServerLevel serverLevel) {
            // Particles at origin
            serverLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    oldX, oldY + 1.0, oldZ,
                    TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
            );

            // Particles at destination
            serverLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    finalX, finalY + 1.0, finalZ,
                    TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
            );
        }

        // Play Enderman teleport sound ONLY at destination
        this.level().playSound(
                null,
                finalX, finalY, finalZ,
                SoundEvents.ENDERMAN_TELEPORT,
                this.getSoundSource(),
                1.0F, 1.0F
        );

        // Set cooldown and reset walking counter
        this.teleportCooldown = CreepingCreepersConfig.ENDER_CREEPER_TELEPORT_COOLDOWN.get();
        this.hasWalkedSinceTeleport = false;
        this.walkingTicks = 0;

        // Fire game event
        this.gameEvent(GameEvent.TELEPORT);

        return true;
    }

    /**
     * Teleports the Ender Creeper to avoid a projectile.
     *
     * This method is specifically for projectile dodging:
     * - NO cooldown (always executes)
     * - Shorter range (8-16 blocks) for more reliable positioning
     * - Multiple attempts (32) to find a valid spot
     * - Simpler validation for faster execution
     * - Does NOT set teleport cooldown afterward
     *
     * @return true if teleportation succeeded
     */
    public boolean teleportToAvoidProjectile() {
        return teleportToAvoidProjectile(null);
    }

    /**
     * Teleports the Ender Creeper to avoid a projectile, optionally staying within range of a target.
     *
     * When a target is provided, the teleport will prioritize positions that keep the
     * EnderCreeper within engagement range of the target, preventing aggro loss from
     * being pushed too far back by repeated dodge teleports.
     *
     * @param engagedTarget The target to stay within range of, or null for unrestricted dodge
     * @return true if teleportation succeeded
     */
    public boolean teleportToAvoidProjectile(@Nullable LivingEntity engagedTarget) {
        if (this.level().isClientSide() || !this.isAlive()) {
            return false;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        double startX = this.getX();
        double startY = this.getY();
        double startZ = this.getZ();

        // Try to find a valid teleport position
        for (int attempt = 0; attempt < DODGE_TELEPORT_ATTEMPTS; attempt++) {
            double targetX, targetZ, targetY;

            if (engagedTarget != null && engagedTarget.isAlive()) {
                // Target-aware dodge: teleport to a position within engagement range of target
                // Pick a random position around the TARGET, not around self
                double maxRange = ENGAGE_TELEPORT_MAX_RANGE - 1.0; // Stay within 11 blocks of target
                double minRange = ENGAGE_TELEPORT_MIN_DISTANCE;
                double range = minRange + this.random.nextDouble() * (maxRange - minRange);
                double angle = this.random.nextDouble() * Math.PI * 2.0;

                targetX = engagedTarget.getX() + Math.cos(angle) * range;
                targetZ = engagedTarget.getZ() + Math.sin(angle) * range;
                targetY = engagedTarget.getY() + (this.random.nextInt(5) - 2); // -2 to +2 blocks vertical from target
            } else {
                // No target - use original random dodge logic
                double range = DODGE_MIN_RANGE + this.random.nextDouble() * DODGE_RANDOM_RANGE;
                double angle = this.random.nextDouble() * Math.PI * 2.0;

                targetX = startX + Math.cos(angle) * range;
                targetZ = startZ + Math.sin(angle) * range;
                targetY = startY + (this.random.nextInt(9) - 4); // -4 to +4 blocks vertical
            }

            // Clamp Y to world bounds
            targetY = Mth.clamp(targetY, this.level().getMinY() + 1, this.level().getMaxY() - 2);

            BlockPos targetPos = BlockPos.containing(targetX, targetY, targetZ);

            // Find ground - search downward for solid block
            BlockPos groundSearch = targetPos;
            while (groundSearch.getY() > this.level().getMinY() + 1) {
                BlockState below = this.level().getBlockState(groundSearch.below());
                if (below.blocksMotion() && !below.getFluidState().is(FluidTags.WATER) && !below.getFluidState().is(FluidTags.LAVA)) {
                    break;
                }
                groundSearch = groundSearch.below();
            }

            // Validate the position
            BlockState ground = this.level().getBlockState(groundSearch.below());
            BlockState atFeet = this.level().getBlockState(groundSearch);
            BlockState atHead = this.level().getBlockState(groundSearch.above());

            // Need: solid ground, empty space at feet and head, no liquids
            boolean validGround = ground.blocksMotion();
            boolean feetClear = !atFeet.blocksMotion() && atFeet.getFluidState().isEmpty();
            boolean headClear = !atHead.blocksMotion() && atHead.getFluidState().isEmpty();
            boolean notFire = !atFeet.is(Blocks.FIRE) && !atFeet.is(Blocks.SOUL_FIRE);

            if (validGround && feetClear && headClear && notFire) {
                // Valid position found - teleport!
                double finalX = groundSearch.getX() + 0.5;
                double finalY = groundSearch.getY();
                double finalZ = groundSearch.getZ() + 0.5;

                // Fire Forge EntityTeleportEvent.EnderEntity — allows protection mods
                // (FTB Chunks, GriefPrevention, etc.) to cancel or modify the teleport
                EntityTeleportEvent.EnderEntity teleportEvent =
                        ForgeEventFactory.onEnderManTeleport(this, finalX, finalY, finalZ);
                if (teleportEvent == null) {
                    continue; // Event was cancelled — try next position
                }
                // Respect any coordinate modifications from event listeners
                finalX = teleportEvent.getTargetX();
                finalY = teleportEvent.getTargetY();
                finalZ = teleportEvent.getTargetZ();

                // Move the entity
                this.teleportTo(finalX, finalY, finalZ);
                this.getNavigation().stop();

                // Particles at origin (batched into single packet)
                serverLevel.sendParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        startX, startY + 1.0, startZ,
                        TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
                );

                // Particles at destination (batched into single packet)
                serverLevel.sendParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        finalX, finalY + 1.0, finalZ,
                        TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
                );

                // Play teleport sound at destination only
                this.level().playSound(
                        null,
                        finalX, finalY, finalZ,
                        SoundEvents.ENDERMAN_TELEPORT,
                        this.getSoundSource(),
                        1.0F, 1.0F
                );

                // Fire game event
                this.gameEvent(GameEvent.TELEPORT);

                // NO cooldown set - projectile dodge has no cooldown
                return true;
            }
        }

        return false;
    }
    
    /**
     * Called when the entity starts walking (for alternating behavior).
     */
    public void onStartWalking() {
        this.hasWalkedSinceTeleport = true;
        this.walkingTicks = 0;
    }
    
    // =========================================================================
    // DAMAGE HANDLING
    // =========================================================================

    /**
     * Called on the server when the entity is about to take damage.
     *
     * Handles projectile dodge like Enderman:
     * - ALL projectiles cause the entity to teleport FIRST (before hit registers)
     * - Then damage is cancelled entirely - the projectile "misses"
     * - Splash water bottles are NOT projectile damage - they trigger
     *   the water sensitivity system instead (handled by isSensitiveToWater())
     *
     * By overriding hurtServer() we intercept the damage BEFORE actuallyHurt()
     * is called, allowing the teleport to happen first so the projectile
     * visually misses the entity.
     *
     * @param level The server level
     * @param source The damage source
     * @param amount The damage amount
     * @return true if damage was applied, false if cancelled
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Check if this is projectile damage - handle Enderman-style dodge
        // Note: Splash water bottles don't use projectile damage - they trigger
        // the water sensitivity system instead (handled by isSensitiveToWater())
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            // Preserve current target before teleporting - dodge should not break aggro
            LivingEntity currentTarget = this.getTarget();

            // Teleport away FIRST (before the projectile "hits")
            // Pass current target so dodge stays within engagement range
            this.teleportToAvoidProjectile(currentTarget);

            // Restore target after teleport to maintain aggro
            if (currentTarget != null && currentTarget.isAlive()) {
                this.setTarget(currentTarget);
            }

            // Return false to cancel the damage entirely - the projectile "missed"
            return false;
        }

        // Non-projectile damage proceeds normally
        boolean wasHurt = super.hurtServer(level, source, amount);

        // Try to teleport away when hurt (if not killed)
        if (wasHurt && this.isAlive()) {
            // Only teleport if damage was significant
            if (amount >= HURT_TELEPORT_DAMAGE_THRESHOLD && this.canTeleport()) {
                // 50% chance to teleport when hurt
                if (this.random.nextBoolean()) {
                    this.teleportRandomly();
                }
            }
        }

        return wasHurt;
    }

    // =========================================================================
    // SPAWN CONDITIONS
    // =========================================================================
    
    /**
     * Checks if this entity can spawn at the given location.
     * Uses Enderman spawn rules (checks light level).
     * 
     * @param level The world
     * @return true if spawn is allowed
     */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        // Prefer darker areas
        return 0.5F - level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
    }
    
    /**
     * Checks spawn conditions for Ender Creeper.
     *
     * Uses vanilla's checkMonsterSpawnRules for light level checks (same logic
     * as Enderman), then applies a dimension-specific spawn chance on top.
     * This ensures correct behavior across all dimensions — the Overworld,
     * Nether, and End all handle sky light differently, and vanilla's
     * isDarkEnoughToSpawn already accounts for this.
     *
     * @param type The entity type
     * @param level The server level accessor
     * @param spawnReason The reason for spawn
     * @param pos The spawn position
     * @param random Random source
     * @return true if spawn is allowed
     */
    public static boolean checkEnderCreeperSpawnRules(
            EntityType<EnderCreeperEntity> type,
            ServerLevelAccessor level,
            EntitySpawnReason spawnReason,
            BlockPos pos,
            RandomSource random
    ) {
        // Use vanilla's monster spawn rules for light checks — same as Enderman.
        // This correctly handles sky light in all dimensions (Overworld day/night,
        // Nether with no sky, End with hasSkyLight=true but dark sky).
        if (!Monster.checkMonsterSpawnRules(type, level, spawnReason, pos, random)) {
            return false;
        }

        // Apply dimension-specific spawn chance
        // Nether: 10%, End: 20%, Overworld: 100% (same as Enderman)
        double spawnChance;
        if (level.getLevel().dimension() == Level.NETHER) {
            spawnChance = SPAWN_CHANCE_NETHER;
        } else if (level.getLevel().dimension() == Level.END) {
            spawnChance = SPAWN_CHANCE_END;
        } else {
            return true; // Overworld: no spawn chance gate, same as Enderman
        }
        return random.nextDouble() < spawnChance;
    }

    // =========================================================================
    // SLEEP PREVENTION
    // =========================================================================

    /**
     * Checks if this entity prevents players from sleeping.
     * Ender Creepers prevent sleep like other hostile mobs.
     *
     * @param level The server level
     * @param player The player trying to sleep
     * @return true if this entity prevents the player from sleeping
     */
    @Override
    public boolean isPreventingPlayerRest(ServerLevel level, Player player) {
        // Prevent sleep if within 8 blocks of a player (standard hostile mob behavior)
        return this.isAlive() && this.distanceToSqr(player) < SLEEP_PREVENTION_RANGE_SQ;
    }
}
