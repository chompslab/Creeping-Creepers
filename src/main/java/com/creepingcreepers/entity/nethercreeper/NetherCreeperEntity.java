/*
 * ============================================================================
 * NetherCreeperEntity.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Implements the Nether Creeper - a creeper that spawns in Lava Seas and
 * walks on lava like a Strider. Has warm/cold states affecting behavior.
 *
 * BEHAVIOR SUMMARY:
 * - Spawns in Lava Seas in the Nether (same conditions as Strider)
 * - Walks on lava (fast) and land (slow when cold)
 * - When outside lava, becomes cold: shivers, 80% explosion reduction, slower
 * - When in lava, is warm: normal explosion, normal speed
 * - Chases players within 16-block radius (14 when riding a Strider)
 * - Does not attack other mobs unless provoked
 * - Flees from cats/ocelots within 6-block radius (faster than pursuit)
 * - Immune to fire and lava damage
 * - Can be damaged by splash water bottles
 * - Explosion creates flame particles
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.entity.nethercreeper;

import com.creepingcreepers.ai.goal.NetherCreeperGoToLavaGoal;
import com.creepingcreepers.ai.goal.NetherCreeperSwellGoal;
import com.creepingcreepers.ai.goal.NetherCreeperTargetGoal;
import com.creepingcreepers.config.CreepingCreepersConfig;
import com.creepingcreepers.entity.base.AbstractVariantCreeper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;


/**
 * The Nether Creeper entity.
 *
 * A creeper variant that spawns in Lava Seas and has temperature-based
 * mechanics affecting its explosion power and movement speed.
 */
public class NetherCreeperEntity extends AbstractVariantCreeper {

    // =========================================================================
    // SYNCED DATA PARAMETERS
    // =========================================================================

    /**
     * Whether the creeper is currently cold (out of lava).
     * Synced to client for texture and animation changes.
     */
    private static final EntityDataAccessor<Boolean> DATA_IS_COLD =
            SynchedEntityData.defineId(NetherCreeperEntity.class, EntityDataSerializers.BOOLEAN);

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** Unique ID for the cold speed modifier. */
    private static final Identifier COLD_SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("creepingcreepers", "nether_creeper_cold_slowdown");

    /** How much faster warming is than cooling (4x = 2.5 seconds to fully warm). */
    private static final int WARM_UP_SPEED_MULTIPLIER = 4;

    /** Movement friction when traveling on lava surface. */
    private static final double LAVA_FRICTION = 0.9D;

    /** Upward force to keep entity locked to lava surface. */
    private static final double LAVA_SURFACE_BUOYANCY = 0.01D;

    /** Maximum Y level for spawning (lava seas are at Y=31). */
    private static final int MAX_SPAWN_Y_LEVEL = 36;

    /** Radius to check for nearby lava when spawning on solid ground. */
    private static final int NEAR_LAVA_SPAWN_RADIUS = 4;

    /** Spawn chance percentage (20%). */
    private static final double SPAWN_CHANCE = 0.20;

    /** Particle count scaling factor per unit of explosion radius. */
    private static final int PARTICLES_PER_RADIUS_UNIT = 30;

    /** Fire duration applied to explosion victims in ticks (3 seconds). */
    private static final int EXPLOSION_FIRE_DURATION = 60;

    /** Damage multiplier for splash water bottles. */
    private static final float WATER_DAMAGE_MULTIPLIER = 2.0f;

    /** Range to look at nearby players in blocks. */
    private static final float LOOK_AT_PLAYER_RANGE = 8.0F;

    // =========================================================================
    // INSTANCE FIELDS
    // =========================================================================

    /**
     * Timer for temperature transition (warm to cold).
     */
    private int temperatureTransitionTimer = 0;

    /**
     * Counter for shivering animation when cold.
     */
    private int shiveringTicks = 0;

    /**
     * Counter for cold damage timing.
     * Tracks how long the creeper has been cold for damage purposes.
     */
    private int coldDamageTicks = 0;

    /**
     * Time before becoming cold in ticks (10 seconds).
     */
    private static final int COLD_TRANSITION_TICKS = 200;

    /**
     * Cached config values for cold damage, read at construction to avoid
     * per-tick config lookup overhead.
     */
    private final int cachedColdDamageDelay;
    private final int cachedColdDamageInterval;
    private final float cachedColdDamageAmount;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Creates a new Nether Creeper.
     *
     * @param entityType The registered entity type
     * @param level The world this entity exists in
     */
    public NetherCreeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.maxSwell = CreepingCreepersConfig.NETHER_CREEPER_FUSE_TIME.get();
        // Cache config values used in per-tick hot paths
        this.cachedColdDamageDelay = CreepingCreepersConfig.NETHER_CREEPER_COLD_DAMAGE_DELAY.get();
        this.cachedColdDamageInterval = CreepingCreepersConfig.NETHER_CREEPER_COLD_DAMAGE_INTERVAL.get();
        this.cachedColdDamageAmount = CreepingCreepersConfig.NETHER_CREEPER_COLD_DAMAGE.get().floatValue();

        // This tells the AI that Lava has a "cost" of 0, making it as attractive as grass.
        this.setPathfindingMalus(PathType.LAVA, 0.0F);
    }

    // =========================================================================
    // ATTRIBUTE CREATION
    // =========================================================================

    /**
     * Creates the attribute supplier for Nether Creepers.
     * Uses warm speed as the base; cold speed is applied via modifier.
     * Uses default values since config isn't loaded at registration time.
     *
     * @return Builder with all entity attributes
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)  // Default from config
                .add(Attributes.MOVEMENT_SPEED, 0.25)  // Default warm speed from config
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    // =========================================================================
    // DATA SYNCHRONIZATION
    // =========================================================================

    /**
     * Defines synced data fields for client-server synchronization.
     *
     * @param builder The builder used to define synced data entries
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_COLD, false);
    }

    // =========================================================================
    // SERIALIZATION (Save/Load)
    // =========================================================================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsCold", this.isCold());
        output.putInt("TemperatureTimer", this.temperatureTransitionTimer);
        output.putInt("ColdDamageTicks", this.coldDamageTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setCold(input.getBooleanOr("IsCold", false));
        this.temperatureTransitionTimer = input.getIntOr("TemperatureTimer", 0);
        this.coldDamageTicks = input.getIntOr("ColdDamageTicks", 0);
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    /**
     * Creates custom pathfinding that allows walking on lava.
     *
     * @param level The level
     * @return Custom path navigation for lava walking
     */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new NetherCreeperPathNavigation(this, level);
    }

    // =========================================================================
    // AI GOALS REGISTRATION
    // =========================================================================

    /**
     * Registers all AI goals for the Nether Creeper.
     *
     * GOAL PRIORITY BREAKDOWN:
     * 1 - Swell (explode) when close to target
     * 2 - Seek lava when not in lava and no nearby target
     * 3 - Avoid cats/ocelots - flee faster than pursuit
     * 4 - Melee attack (approach target)
     * 5 - Wander around
     * 6 - Look at players
     * 7 - Random look around
     *
     * TARGET GOALS:
     * 1 - Retaliate when hurt (provocation)
     * 2 - Target nearby players (with strider rider detection)
     */
    @Override
    protected void registerGoals() {
        // === BEHAVIOR GOALS ===

        // Priority 1: Start swelling when close enough to target
        this.goalSelector.addGoal(1, new NetherCreeperSwellGoal(this));

        // Priority 2: Chase and explode at all cost when warm.
        // When cold: only commit if player is within 6 blocks (explosion still hurts them).
        // Beyond 6 blocks while cold, step aside so lava goal takes over.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            private static final double COLD_COMMIT_RANGE_SQ = 36.0; // 6 blocks squared
            @Override
            public boolean canUse() {
                if (NetherCreeperEntity.this.isCold()) {
                    LivingEntity target = NetherCreeperEntity.this.getTarget();
                    return target != null && NetherCreeperEntity.this.distanceToSqr(target) <= COLD_COMMIT_RANGE_SQ;
                }
                return super.canUse();
            }
            @Override
            public boolean canContinueToUse() {
                if (NetherCreeperEntity.this.isCold()) {
                    LivingEntity target = NetherCreeperEntity.this.getTarget();
                    return target != null && NetherCreeperEntity.this.distanceToSqr(target) <= COLD_COMMIT_RANGE_SQ;
                }
                return super.canContinueToUse();
            }
        });

        // Priority 3: Run away from cats and ocelots
        float fleeSpeed = CreepingCreepersConfig.NETHER_CREEPER_FLEE_SPEED.get().floatValue();
        float catFearRange = CreepingCreepersConfig.NETHER_CREEPER_CAT_FEAR_RANGE.get().floatValue();
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, catFearRange, 1.0, fleeSpeed));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, catFearRange, 1.0, fleeSpeed));

        // Priority 4: Seek lava when idle, or when cold and player is out of commit range
        this.goalSelector.addGoal(4, new NetherCreeperGoToLavaGoal(this, 1.0));

        // Priority 5: Wander around when idle (don't avoid water in Nether)
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));

        // Priority 6: Look at nearby players
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LOOK_AT_PLAYER_RANGE));

        // Priority 7: Random looking around
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // === TARGETING GOALS ===

        // Target whatever hurt us (provocation response only)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // Target nearby players (with special strider rider detection)
        this.targetSelector.addGoal(2, new NetherCreeperTargetGoal(this));
    }

    // =========================================================================
    // TICK AND UPDATE LOGIC
    // =========================================================================

    /**
     * Called every game tick.
     * Handles temperature state transitions, cold damage, and movement speed updates.
     */
    @Override
    public void tick() {
        // Update temperature state
        this.updateTemperatureState();

        // Update shivering animation counter and cold damage
        if (this.isCold()) {
            this.shiveringTicks++;

            // Apply cold damage on server side only
            if (!this.level().isClientSide()) {
                this.coldDamageTicks++;

                int damageDelay = this.cachedColdDamageDelay;
                int damageInterval = this.cachedColdDamageInterval;
                float damageAmount = this.cachedColdDamageAmount;

                // After delay, apply damage at regular intervals
                if (this.coldDamageTicks > damageDelay && damageAmount > 0) {
                    int ticksSinceDelay = this.coldDamageTicks - damageDelay;
                    if (ticksSinceDelay % damageInterval == 0) {
                        this.hurt(this.damageSources().freeze(), damageAmount);
                    }
                }
            }
        } else {
            this.shiveringTicks = 0;
            this.coldDamageTicks = 0;
        }

        super.tick();
    }

    /**
     * Tells the physics engine that this entity can stand on lava
     * as if it were a solid block.
     */
    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluidState) {
        // Return true if the fluid is lava, allowing the creeper to walk on the surface
        return fluidState.is(FluidTags.LAVA);
    }
    /**
     * Overrides how the entity moves through space.
     * This is the "Strider Secret" that prevents sinking.
     */
    @Override
    public void travel(Vec3 travelVector) {
        // 1. Check if the mob is currently in lava
        if (this.isEffectiveAi() && this.isInLava()) {

            // 2. Move based on speed attributes rather than liquid drag
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());

            // 3. Apply friction (0.9 multiplier) so they don't slide forever
            // and add a tiny upward force (0.01) to keep them "locked" to the surface.
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * LAVA_FRICTION, LAVA_SURFACE_BUOYANCY, delta.z * LAVA_FRICTION);

        } else {
            // 4. If not in lava, just do normal walking/swimming
            super.travel(travelVector);
        }
    }

    /**
     * Updates the temperature state based on whether the creeper is in lava.
     * Creeper becomes cold after 10 seconds (200 ticks) out of lava.
     */
    private void updateTemperatureState() {
        if (this.level().isClientSide()) {
            return;
        }

        boolean inLava = this.isInLava();

        if (inLava) {
            // In lava - warm up (faster than cooling down)
            if (this.temperatureTransitionTimer > 0) {
                // Warm up faster than cooling down
                this.temperatureTransitionTimer -= WARM_UP_SPEED_MULTIPLIER;
            }
            if (this.temperatureTransitionTimer <= 0 && this.isCold()) {
                this.temperatureTransitionTimer = 0;
                this.setCold(false);
                this.updateMovementSpeed();
            }
        } else {
            // Out of lava - cool down over 10 seconds
            if (!this.isCold()) {
                this.temperatureTransitionTimer++;
                if (this.temperatureTransitionTimer >= COLD_TRANSITION_TICKS) {
                    this.setCold(true);
                    this.updateMovementSpeed();
                }
            }
        }
    }

    /**
     * Updates movement speed based on temperature state.
     * Applies or removes speed modifier when cold.
     * Only runs on server side - attribute modifiers are synced automatically.
     */
    private void updateMovementSpeed() {
        // Only modify attributes on server side - they sync to client automatically
        if (this.level().isClientSide()) {
            return;
        }

        AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }

        // Remove existing cold modifier if present
        speedAttr.removeModifier(COLD_SPEED_MODIFIER_ID);

        if (this.isCold()) {
            // Calculate the speed reduction needed
            double warmSpeed = CreepingCreepersConfig.NETHER_CREEPER_WARM_SPEED.get();
            double coldSpeed = CreepingCreepersConfig.NETHER_CREEPER_COLD_SPEED.get();
            double reductionPercent = (warmSpeed - coldSpeed) / warmSpeed;

            // Apply speed reduction as a multiplier modifier
            speedAttr.addTransientModifier(new AttributeModifier(
                    COLD_SPEED_MODIFIER_ID,
                    -reductionPercent,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
    }

    // =========================================================================
    // TEMPERATURE STATE
    // =========================================================================

    /**
     * Returns whether the creeper is currently cold.
     *
     * @return true if cold (out of lava for too long)
     */
    public boolean isCold() {
        return this.entityData.get(DATA_IS_COLD);
    }

    /**
     * Sets the cold state.
     * Temperature affects explosion power, movement speed, and chase commitment range,
     * but goal priorities remain static — the MeleeAttackGoal handles cold logic internally.
     *
     * @param cold true to set cold, false to set warm
     */
    public void setCold(boolean cold) {
        this.entityData.set(DATA_IS_COLD, cold);
    }

    /**
     * Gets the shivering tick counter for animations.
     *
     * @return The number of ticks spent shivering
     */
    public int getShiveringTicks() {
        return this.shiveringTicks;
    }

    // =========================================================================
    // EXPLOSION BEHAVIOR
    // =========================================================================

    /**
     * Gets the explosion radius, reduced when cold.
     *
     * @return Explosion radius in blocks
     */
    @Override
    protected float getExplosionRadius() {
        float baseRadius = CreepingCreepersConfig.NETHER_CREEPER_EXPLOSION_RADIUS.get().floatValue();
        if (this.isCold()) {
            // Apply cold multiplier (e.g., 0.2 = 80% reduction)
            float coldMultiplier = CreepingCreepersConfig.NETHER_CREEPER_COLD_EXPLOSION_MULTIPLIER.get().floatValue();
            return baseRadius * coldMultiplier;
        }
        return baseRadius;
    }

    /**
     * Overrides explosion to set nearby entities on fire for 3 seconds.
     */
    @Override
    protected void explode() {
        if (!this.level().isClientSide()) {
            float radius = this.getExplosionRadius();

            // Set all living entities within explosion radius on fire for 3 seconds
            for (LivingEntity entity : this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.getBoundingBox().inflate(radius),
                    e -> e != this && e.isAlive())) {
                double distSq = this.distanceToSqr(entity);
                if (distSq <= radius * radius) {
                    entity.setRemainingFireTicks(EXPLOSION_FIRE_DURATION);
                }
            }
        }

        // Call parent explosion logic (creates explosion, effects, discards entity)
        super.explode();
    }

    /**
     * Creates flame particles when the creeper explodes.
     */
    @Override
    protected void createCustomExplosionEffects() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Spawn flame particles in a sphere around the explosion (batched into single packet)
        float radius = this.getExplosionRadius();
        int particleCount = (int) (radius * PARTICLES_PER_RADIUS_UNIT);

        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                this.getX(),
                this.getY() + 0.5,
                this.getZ(),
                particleCount,
                radius * 0.5, radius * 0.5, radius * 0.5,
                0.05
        );

        // Also add some larger fire particles (batched into single packet)
        serverLevel.sendParticles(
                ParticleTypes.LAVA,
                this.getX(),
                this.getY() + 0.5,
                this.getZ(),
                particleCount / 3,
                radius * 0.3, radius * 0.3, radius * 0.3,
                0
        );
    }

    // =========================================================================
    // DAMAGE HANDLING
    // =========================================================================

    /**
     * Handles damage, including extra damage from water.
     * Splash water bottles deal extra damage to this fire creature.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Check if this is indirect magic damage (includes splash potions)
        // Water bottles specifically use this damage type
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            // This is likely a splash potion - water hurts this creature
            return super.hurtServer(level, source, amount * WATER_DAMAGE_MULTIPLIER);
        }

        return super.hurtServer(level, source, amount);
    }
    @Override
    public boolean isSensitiveToWater() {
        return true; // Makes the mob take damage in rain/water, fitting for a Nether mob
    }

    @Override
    protected void jumpInLiquid(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> fluidTag) {
        // Overriding this to be empty stops the "auto-jump"
        // that mobs do when they feel liquid at their feet.
    }
    // =========================================================================
    // SPAWN CONDITIONS
    // =========================================================================

    /**
     * Checks spawn conditions for Nether Creeper.
     * Spawns in the Nether under similar conditions to Strider:
     * - Must be in the Nether dimension
     * - Must be at Y level 40 or below (lava seas are at Y=31)
     * - Can spawn in lava, above lava, or on solid ground near lava
     * - Applies spawn chance percentage
     *
     * @param type The entity type
     * @param level The server level accessor
     * @param spawnReason The spawn reason
     * @param pos The spawn position
     * @param random Random source
     * @return true if spawn is allowed
     */
    public static boolean checkNetherCreeperSpawnRules(
            EntityType<NetherCreeperEntity> type,
            ServerLevelAccessor level,
            EntitySpawnReason spawnReason,
            BlockPos pos,
            RandomSource random
    ) {
        // Must be in the Nether dimension
        if (level.getLevel().dimension() != Level.NETHER) {
            return false;
        }

        // Must be at or below Y=40 (lava seas are at Y=31)
        if (pos.getY() > MAX_SPAWN_Y_LEVEL) {
            return false;
        }

        // Check spawn position validity (similar to Strider behavior)
        boolean inLava = level.getFluidState(pos).is(FluidTags.LAVA);
        boolean aboveLava = level.getFluidState(pos.below()).is(FluidTags.LAVA);

        // For ground spawning, must be near lava (within 4 blocks)
        boolean onSolidGroundNearLava = false;
        if (!inLava && !aboveLava) {
            boolean onSolidGround = level.getBlockState(pos.below()).isSolid() &&
                                    level.getBlockState(pos).isAir();
            if (onSolidGround) {
                onSolidGroundNearLava = isNearLava(level, pos, NEAR_LAVA_SPAWN_RADIUS);
            }
        }

        if (!inLava && !aboveLava && !onSolidGroundNearLava) {
            return false;
        }

        // Apply spawn chance (20%)
        return random.nextDouble() < SPAWN_CHANCE;
    }

    /**
     * Checks if a position is near lava within the specified radius.
     *
     * @param level The level accessor
     * @param pos The position to check from
     * @param radius The search radius
     * @return true if lava is found nearby
     */
    private static boolean isNearLava(ServerLevelAccessor level, BlockPos pos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    if (level.getFluidState(checkPos).is(FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Checks if a player is riding a Strider.
     *
     * @param player The player to check
     * @return true if the player is riding a Strider
     */
    public boolean isPlayerRidingStrider(Player player) {
        return player.getVehicle() instanceof Strider;
    }

    /**
     * Gets the detection range for a specific player.
     * Reduced range if player is riding a Strider.
     *
     * @param player The player to check
     * @return Detection range in blocks
     */
    public double getDetectionRangeForPlayer(Player player) {
        if (isPlayerRidingStrider(player)) {
            return CreepingCreepersConfig.NETHER_CREEPER_STRIDER_RIDER_DETECTION_RANGE.get();
        }
        return CreepingCreepersConfig.NETHER_CREEPER_PLAYER_DETECTION_RANGE.get();
    }
}
